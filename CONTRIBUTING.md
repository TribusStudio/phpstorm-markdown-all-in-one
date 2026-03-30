# Contributing to Markdown All-in-One

Thank you for your interest in contributing to Markdown All-in-One for PHPStorm.

## Development

All builds run inside a Docker devcontainer — no JDK or Gradle required on your machine.

### Prerequisites
- Docker

### Quick Start
```bash
./dev up        # Start the dev container
./dev setup     # Generate Gradle wrapper, download dependencies
./dev build     # Build the plugin
./dev test      # Run tests
```

### All Commands
```bash
# Container management
./dev up          # Start container
./dev down        # Stop container
./dev status      # Show status and versions
./dev shell       # Open shell in container
./dev destroy     # Remove container + cache

# Build & test
./dev build       # Build the plugin
./dev test        # Run all tests
./dev package     # Build distributable .zip
./dev verify      # Verify IDE compatibility
./dev clean       # Clean build artifacts
./dev gradle ...  # Run any Gradle task
```

See [docs/004-devcontainer-setup.md](docs/004-devcontainer-setup.md) for full details.

### Running the Plugin
```bash
./dev gradle runIde
```
This launches a sandboxed PHPStorm instance with the plugin loaded.

## Project Structure

```
phpstorm-markdown-all-in-one/
  build.gradle.kts          # Build configuration
  settings.gradle.kts       # Project settings
  gradle.properties         # Gradle and plugin properties
  gradle/
    libs.versions.toml      # Version catalog
  src/
    main/
      kotlin/com/tribus/markdown/   # Plugin source code
        actions/             # Editor actions (bold, italic, lists, etc.)
        editor/              # Editor enhancements (auto-pairs, formatting)
        highlighting/        # Syntax highlighting extensions
        completion/          # Code completion providers
        inspection/          # Inspections and quick-fixes
        preview/             # Live preview tool window
        settings/            # Plugin settings/preferences
        toc/                 # Table of contents generation
        util/                # Shared utilities
      resources/
        META-INF/
          plugin.xml         # Plugin descriptor
        messages/            # i18n message bundles
        icons/               # Plugin icons
    test/
      kotlin/com/tribus/markdown/   # Test sources
      testData/              # Test fixture data (sample .md files)
```

## How the Plugin Works

This section walks through the plugin's architecture from bootstrap to runtime, explaining what each piece does and why it was built that way. If you're new to IntelliJ plugin development, this should give you the mental model you need to contribute confidently.

### The Starting Point: plugin.xml

Every IntelliJ plugin begins with `src/main/resources/META-INF/plugin.xml`. This is the plugin descriptor — the file IntelliJ reads at startup to discover what the plugin provides. Think of it as a manifest that declares:

- **What language and file type the plugin handles** (the `<fileType>` and `<lang.*>` extensions)
- **What services and listeners to register** (settings, save listeners, editor handlers)
- **What UI components to create** (split editor, toolbar actions, menus)
- **What keyboard shortcuts to bind** (actions with `<keyboard-shortcut>`)

IntelliJ uses a lazy-loading, declarative model. Nothing in plugin.xml runs code at startup — it tells IntelliJ "when a user opens a `.md` file, use *these* classes to handle it." The platform instantiates classes on demand.

### Standalone by Design

A key architectural decision: this plugin has **zero dependency on IntelliJ's bundled Markdown plugin**. The only dependency declared is `com.intellij.modules.platform` — the base IntelliJ Platform APIs.

This means the plugin registers its own language ("MarkdownAIO"), its own file type, its own parser, its own highlighter — the complete language stack from scratch. The language ID is deliberately different from the bundled plugin's "Markdown" to allow coexistence (though we recommend disabling the bundled one).

Why standalone? Because hooking into the bundled plugin would mean depending on its internal APIs, which change between IDE versions. A standalone plugin controls its entire stack and can evolve independently.

### Bootstrap Sequence: What Happens When You Open a .md File

Here's the chain of events when a user opens a markdown file:

```
User opens file.md
       |
       v
1. FileType matching
   IntelliJ checks the file extension against registered FileTypes.
   Our MarkdownFileType (lang/MarkdownFileType.kt) claims .md, .markdown, .mdown, .mkd, .mkdn.
       |
       v
2. Language stack activates
   IntelliJ looks up the language ("MarkdownAIO") and finds our ParserDefinition,
   which provides the Lexer and Parser for tokenizing and building the PSI tree.
       |
       v
3. Lexer tokenizes the document
   MarkdownLexer (lang/MarkdownLexer.kt) scans the text character by character,
   producing a flat stream of tokens: HEADING_MARKER, TEXT, CODE_FENCE, LIST_MARKER, etc.
       |
       v
4. Parser builds the PSI tree
   MarkdownParser (lang/parser/MarkdownParser.kt) takes the token stream and builds
   an AST. In our case, it builds a deliberately FLAT tree — just a FILE node containing
   raw tokens. No nested nodes for headings, paragraphs, or lists.
       |
       v
5. Syntax highlighting (Layer 1: token colors)
   MarkdownSyntaxHighlighter maps token types to colors.
   HEADING_MARKER → heading color, CODE_FENCE → code color, etc.
       |
       v
6. Annotator highlighting (Layer 2: inline patterns)
   MarkdownAnnotator runs regex patterns on TEXT tokens to find **bold**, *italic*,
   [links](url), ![images](url), ~~strikethrough~~, and $math$. These get colored
   without changing the PSI tree.
       |
       v
7. Editor creation triggers
   MarkdownFileEditorListener fires (via EditorFactoryListener) and registers
   component-level keyboard shortcuts on the editor. The split editor provider
   creates MarkdownSplitEditor with toolbar and preview panel.
       |
       v
8. Smart handlers active
   TypedHandler (selection wrapping), EnterHandler (list continuation),
   BackspaceHandler (smart marker deletion) all start intercepting keystrokes.
       |
       v
9. Ready for editing
```

### The Language Foundation

Three small files define the language identity:

**`lang/MarkdownLanguage.kt`** — A singleton `Language("MarkdownAIO")` object. The string `"MarkdownAIO"` is the language identifier used throughout the entire plugin: in plugin.xml attributes, in token types, in extension point registrations. It's a Kotlin `object` with `readResolve()` for serialization safety (an IntelliJ requirement for Language singletons).

**`lang/MarkdownFileType.kt`** — Extends `LanguageFileType` and associates the language with file extensions. Provides the file icon. Registered as a singleton via `@JvmField INSTANCE` so IntelliJ can reference it from plugin.xml's `fieldName` attribute.

**`lang/MarkdownIcons.kt`** — A Kotlin `object` that lazy-loads SVG icons via `IconLoader.getIcon()`. IntelliJ handles light/dark variant selection automatically — if `toolbar-bold.svg` exists alongside `toolbar-bold_dark.svg`, the platform picks the right one based on the active theme.

### The Flat PSI Tree: A Deliberate Architecture Choice

This is the most important design decision in the plugin and it affects everything else.

IntelliJ's standard pattern for language plugins is to build a **deep PSI (Program Structure Interface) tree** — think of an AST where headings contain paragraphs, paragraphs contain inline elements, and so on. The bundled Markdown plugin does this. Most language plugins do this.

We chose not to. Our PSI tree is **flat**: a FILE node containing a sequence of tokens with no nesting. Here's why:

**The problem with a deep tree for Markdown:**
Markdown inline syntax is *ambiguous and context-dependent*. Consider `_foo_bar_` — is that italic `foo_bar` or italic `foo` followed by `bar_`? What about `**bold *and italic* text**`? Nested inline formatting creates combinatorial parsing complexity. Building a correct tree means implementing a full CommonMark parser in the PSI builder, which is complex, fragile, and hard to maintain.

**The flat tree solution:**
Our lexer handles only **block-level structure** — things with clear line-based boundaries:
- Headings (lines starting with `#`)
- Code fences (lines starting with three backticks ````` or `~~~`)
- Blockquote markers (lines starting with `>`)
- List markers (lines starting with `-`, `*`, `+`, or digits)
- Horizontal rules

Everything else becomes a `TEXT` token. Inline formatting (**bold**, *italic*, [links], etc.) is handled in a second pass by the `MarkdownAnnotator`, which runs regex patterns on TEXT tokens and creates visual highlighting annotations without modifying the tree.

**Why this works well:**
1. **Simplicity** — The lexer is a single hand-written class (~200 lines) with clear state transitions, not a generated grammar
2. **Performance** — Fewer PSI nodes means less memory, faster tree rebuilds on edits
3. **Maintainability** — Inline patterns live in one place (the annotator) and can be tweaked independently
4. **Correctness trade-off** — Regex patterns for inline formatting cover 95%+ of real-world usage without the complexity of a full CommonMark parser

**The trade-off:** Features that need semantic understanding of the document (like "which heading does this paragraph belong to?") can't query the PSI tree directly. Instead, we use text-based extraction — `HeadingExtractor.extract(text)` parses headings from raw document text. The Structure View, Code Folding, Breadcrumbs, and TOC generator all use this approach.

### The Lexer: Hand-Written and Stateful

`lang/MarkdownLexer.kt` extends `LexerBase` — a manual token-by-token lexer, not a generated one (no JFlex, no grammar files).

The lexer maintains state:
- `atLineStart` — Reset after each newline, enables detection of block-level constructs (headings require being at the start of a line)
- `inCodeBlock` — Tracks whether we're inside a fenced code block (state 0 = normal, state 1 = in code). This enables IntelliJ's incremental re-lexing — when a user types inside a code block, only that block needs re-tokenization
- `inHeading` — After seeing `#` markers, treats the rest of the line as HEADING_CONTENT for the annotator to process

The lexer produces ~10 token types: `HEADING_MARKER`, `HEADING_CONTENT`, `CODE_FENCE`, `CODE_BLOCK_CONTENT`, `BLOCKQUOTE_MARKER`, `LIST_MARKER`, `HORIZONTAL_RULE`, `CODE_SPAN`, `TEXT`, and `EOL`.

### Two-Layer Highlighting

Syntax highlighting happens in two distinct passes:

**Layer 1 — Token-level colors** (`lang/highlighting/MarkdownSyntaxHighlighter.kt`):
Direct mapping from token types to colors. `HEADING_MARKER` gets the keyword color, `CODE_SPAN` gets the string color, `BLOCKQUOTE_MARKER` gets the comment color. This covers block-level visual structure.

**Layer 2 — Inline pattern annotations** (`lang/highlighting/MarkdownAnnotator.kt`):
Runs on every `TEXT` and `HEADING_CONTENT` token. Uses regex to find inline formatting and creates `newSilentAnnotation()` calls with specific `TextRange` offsets. "Silent" means the annotations add visual styling without generating warnings or errors.

The annotator finds:
- `**bold**` and `__bold__`
- `*italic*` and `_italic_`
- `~~strikethrough~~`
- `[link text](url)` — highlights text and URL portions separately
- `![alt](url)` — image syntax
- `$math$` and `$$display math$$`

This two-layer system decouples tokenization from pattern matching. You can change how bold is detected without touching the lexer or parser.

### Keyboard Shortcuts: The Override Problem

When a user presses `Ctrl+B` in PHPStorm, the IDE fires `GotoDeclaration`. But in a markdown file, we want `Ctrl+B` to toggle bold. This is a fundamental conflict.

The solution is **component-level shortcut registration**. IntelliJ's shortcut resolution has a priority order:
1. Component-level shortcuts (highest priority)
2. Active tool window shortcuts
3. Global keymap shortcuts (lowest priority)

`editor/MarkdownFileEditorListener.kt` implements `EditorFactoryListener`, which fires when any editor is created. It checks if the file is markdown, and if so, registers our actions directly on `editor.contentComponent`:

```kotlin
action.registerCustomShortcutSet(shortcutSet, editor.contentComponent)
```

This ensures our `Ctrl+B` (ToggleBold) always wins over the IDE's `Ctrl+B` (GotoDeclaration), but only when the focus is in a markdown editor. In non-markdown files, the standard IDE shortcuts work normally.

The listener also respects user keymap customizations — if someone rebinds ToggleBold to a different key in Settings > Keymap, the component-level registration uses their custom binding.

### Smart Key Handlers

Three handlers intercept keystrokes to provide markdown-aware editing:

**`editor/MarkdownTypedHandler.kt`** — Handles character input. Two responsibilities:
1. **Selection wrapping:** When text is selected and you type `*`, `~`, `_`, `` ` ``, `|`, or `-`, it wraps the selection instead of replacing it. The pipe (`|`) and dash (`-`) characters have special table-aware behavior. Uses `beforeSelectionRemoved()` instead of `beforeCharTyped()` because the selection is deleted before `beforeCharTyped()` fires — a subtle IntelliJ API detail.
2. **Completion triggers:** Detects context patterns like `](` (file path completion), `](#` (heading reference), `][` (reference link) and triggers the auto-popup completion UI.

**`editor/MarkdownEnterHandler.kt`** — Handles Enter key. Detects the current line's context (unordered list, ordered list, task list, blockquote) and continues the pattern on the next line. Smart enough to auto-increment ordered list numbers, reset task checkboxes, and remove empty markers when you press Enter on a blank list item. Returns `Result.Stop` to prevent IntelliJ's default Enter behavior from also firing.

**`editor/MarkdownBackspaceHandler.kt`** — Handles Backspace. When the cursor is right after a list marker (`- |`), pressing Backspace outdents the item (if indented) or removes the marker (if top-level) instead of just deleting one character.

### The Split Editor and Preview

`preview/MarkdownSplitEditorProvider.kt` implements `FileEditorProvider` with `HIDE_DEFAULT_EDITOR` policy — when it accepts a file, IntelliJ's default text editor is hidden and replaced with our split editor.

`preview/MarkdownSplitEditor.kt` extends `TextEditorWithPreview`, combining:
- A standard text editor (left panel)
- A JCEF-based HTML preview (right panel)
- A formatting toolbar (top)
- Bidirectional scroll synchronization

The toolbar uses IntelliJ's native `ActionToolbar` API (not custom Swing buttons) to match the IDE's design language — proper icon sizing, DPI scaling, hover states, and separator rendering.

Scroll sync uses source-line mapping: the HTML converter injects `data-source-line` attributes into output elements, and the preview JavaScript reports which source lines are visible. The editor scrolls to match, and vice versa. Debounce flags (`scrollingFromEditor`/`scrollingFromPreview`) prevent feedback loops.

### Actions and the Formatting Pattern

Most formatting actions follow the same pattern via `BaseToggleFormattingAction`:

1. Check if text is selected
2. If yes — toggle the formatting wrapper (e.g., add or remove `**` around the selection)
3. If no selection — find the word at cursor and wrap it
4. If no word — insert empty markers with cursor in the middle (e.g., `****` with cursor between)

The toggle logic lives in `util/MarkdownFormattingUtil.kt`, which handles edge cases like nested formatting, partial selections, and cursor positioning after the edit. All edits are wrapped in `WriteCommandAction` for proper undo/redo support.

### Settings and State

`settings/MarkdownSettings.kt` uses IntelliJ's `PersistentStateComponent` pattern:
- State is a data class with all configuration properties (18 settings)
- Persisted as XML in the IDE's config directory (`MarkdownAllInOne.xml`)
- A `CopyOnWriteArrayList<ChangeListener>` broadcasts changes to subscribers (the split editor listens for toolbar visibility changes, the save listeners check their enable flags, etc.)

`settings/MarkdownSettingsConfigurable.kt` builds the Settings UI using Kotlin UI DSL v2 (required for IntelliJ 2025.1+). The declarative `panel { group { row { ... } } }` syntax creates the form without manual Swing layout code.

### Save-Time Automation

Two `FileDocumentManagerListener` implementations hook into the save event:

**`toc/TocAutoUpdateListener.kt`** — When a file is saved, checks for `<!-- TOC -->` markers and regenerates the table of contents from the document's headings. Uses `HeadingExtractor` and `TocGenerator` to parse headings and build the markdown list.

**`table/TableFormatOnSaveListener.kt`** — When a file is saved, finds all GFM tables and auto-formats them with consistent column padding and alignment. Uses `TableParser` and `TableFormatter`.

Both listeners wrap their changes in `WriteCommandAction` so the auto-formatting appears as a single undoable operation, and both check their respective settings flags before running.

### Completion Providers

`completion/MarkdownCompletionContributor.kt` registers four context-aware completion providers:

1. **Heading references** — type `[text](#` to get a list of document headings as anchor slugs
2. **Reference link labels** — type `[text][` to get defined `[label]: url` references
3. **File/image paths** — type `[text](` or `![alt](` to browse files relative to the current document
4. **Math commands** — type `\` inside `$...$` for LaTeX command completion (170+ commands)

Each provider analyzes the text before the cursor to determine if the completion context matches, then generates `LookupElement` items for IntelliJ's completion popup.

### Structure View, Folding, and Navigation

These features (Phase 16) all work from document text rather than the PSI tree, using `HeadingExtractor.extract(text)` to get a flat list of headings with their levels and line numbers:

**Structure View** (`structure/MarkdownStructureViewFactory.kt` + related classes) — Builds a hierarchical tree of `HeadingTreeElement` nodes using a stack-based algorithm. H2s nest under H1s, H3s under H2s, etc. Displayed in the Structure tool window.

**Code Folding** (`structure/MarkdownFoldingBuilder.kt`) — Defines collapsible regions for headings (fold content between same-level headings), fenced code blocks (with language in the placeholder text), YAML front matter, and multi-line blockquotes. Uses regex line scanning — same approach as the lexer.

**Go To Symbol** (`structure/MarkdownGoToSymbolContributor.kt`) — Makes all headings across all project markdown files searchable via `Ctrl+Shift+Alt+N`. Iterates markdown files using `FileTypeIndex`.

**Breadcrumbs** (`structure/MarkdownBreadcrumbsProvider.kt`) — Shows the heading hierarchy path at the cursor position. At any offset, walks the heading list to build the chain of ancestor headings.

### Build Configuration

`build.gradle.kts` uses the IntelliJ Platform Gradle Plugin 2.11.0. Key details:
- **Target:** PHPStorm 2025.1+ (platform build 251.*)
- **JDK:** 21 (required by IntelliJ 2024.2+)
- **Kotlin:** 2.x (required by IntelliJ 2025.1+)
- **Testing:** JUnit 5 with IntelliJ Platform test fixtures
- **Change notes:** A custom task extracts the current version's section from CHANGELOG.md and converts it to HTML for the "What's New" display in the plugin marketplace

### Key Files Reference

| File | Role |
| ---- | ---- |
| `resources/META-INF/plugin.xml` | Plugin descriptor — the bootstrap entry point |
| `lang/MarkdownLanguage.kt` | Language singleton ("MarkdownAIO") |
| `lang/MarkdownFileType.kt` | File type registration (.md, .markdown, etc.) |
| `lang/MarkdownLexer.kt` | Hand-written block-level tokenizer |
| `lang/parser/MarkdownParser.kt` | Flat PSI tree builder |
| `lang/parser/MarkdownParserDefinition.kt` | Wires lexer + parser together |
| `lang/highlighting/MarkdownSyntaxHighlighter.kt` | Token-to-color mapping (Layer 1) |
| `lang/highlighting/MarkdownAnnotator.kt` | Inline formatting patterns (Layer 2) |
| `editor/MarkdownFileEditorListener.kt` | Shortcut registration at editor creation |
| `editor/MarkdownTypedHandler.kt` | Selection wrapping + completion triggers |
| `editor/MarkdownEnterHandler.kt` | Smart list/blockquote continuation |
| `editor/MarkdownBackspaceHandler.kt` | Smart marker deletion |
| `preview/MarkdownSplitEditorProvider.kt` | Split editor factory |
| `preview/MarkdownSplitEditor.kt` | Editor + preview + toolbar + scroll sync |
| `settings/MarkdownSettings.kt` | Persistent configuration state |
| `completion/MarkdownCompletionContributor.kt` | Context-aware completions |
| `toc/HeadingExtractor.kt` | Heading parser (used by TOC, Structure, Folding) |
| `toc/TocGenerator.kt` | TOC generation and update |
| `structure/MarkdownFoldingBuilder.kt` | Code folding regions |
| `structure/MarkdownStructureViewFactory.kt` | Structure View heading tree |
| `actions/BaseToggleFormattingAction.kt` | Shared formatting action pattern |
| `util/MarkdownFormattingUtil.kt` | Toggle formatting logic |

## Workflow

1. **Create a branch** from `main` using the naming convention:
   - `feature/description` for new features
   - `fix/description` for bug fixes

2. **Write tests** for your changes. Every feature needs unit tests; editor actions need integration tests.

3. **Follow Kotlin conventions** and the project's coding style (see CLAUDE.md).

4. **Use Conventional Commits** for commit messages:
   ```
   feat(actions): add toggle underline action
   fix(list): correct ordered list renumbering at indent level 3
   ```

5. **Open a Pull Request** against `main` with a clear description and test plan.

## Reporting Issues

Please use GitHub Issues. Include:
- PHPStorm version
- Plugin version
- Steps to reproduce
- Expected vs actual behavior
- Sample markdown file (if applicable)
