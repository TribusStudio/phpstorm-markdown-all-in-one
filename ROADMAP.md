# Markdown All-in-One for PHPStorm — Development Roadmap

## Phase 1: Foundation (MVP) ✅
Core plugin infrastructure and basic formatting actions.

- [x] Project scaffold (Gradle, plugin.xml, build config)
- [x] Docker devcontainer with JDK 21 build environment
- [x] `./dev` CLI for container management and build commands
- [x] Plugin settings framework (UI DSL v2 settings page)
- [x] Toggle Bold action (`Cmd+B` / `Ctrl+B`)
- [x] Toggle Italic action (`Cmd+I` / `Ctrl+I`)
- [x] Toggle Strikethrough action (`Alt+S`)
- [x] Toggle Code Span action (backtick wrapping)
- [x] Toggle Code Block action (fenced block wrapping)
- [x] Heading level up/down actions (`Ctrl+Shift+]` / `[`)
- [x] Task list checkbox toggle (`Alt+C`)
- [x] Unit tests for formatting utilities
- [x] Integration tests with editor fixtures (BasePlatformTestCase)
- [x] Verify build compiles and passes in devcontainer
- [x] Own file type registration (MarkdownAIO) — full language stack with lexer, parser, highlighter, annotator
- [x] Keyboard shortcuts override IDE builtins via component-level registration (EditorFactoryListener)
- [x] Selection wrapping — type `*`, `~`, `_`, `` ` ``, `|`, `-` with text selected to wrap contextually
- [x] Pipe wrapper creates table cells (`| text |`) with smart preceding-pipe detection
- [x] Dash wrapper fills with dashes inside table cells for header border generation
- [x] GitHub Actions CI/CD pipeline with auto-release and custom plugin repository (updatePlugins.xml)
- [x] Release notes auto-populated with commit log since previous tag
- [x] Version bump script (`scripts/bump-version.sh`)

## Phase 2: Smart List Editing & File Scaffolding ✅
Context-aware list editing behavior and markdown file creation tools.

- [x] Smart Enter — auto-continue unordered lists (`-`, `+`, `*`)
- [x] Smart Enter — auto-continue ordered lists (`1.`, `2)`) with auto-increment
- [x] Smart Enter — auto-continue task lists (`- [ ]`, `- [x]`) with unchecked reset
- [x] Smart Enter — auto-continue blockquotes (`> `)
- [x] Smart Enter — remove empty list item on Enter (outdent if indented, delete marker if top-level)
- [x] Indent list item (`Cmd+]` / `Ctrl+]`) — supports multi-line selection
- [x] Outdent list item (`Cmd+[` / `Ctrl+[`) — supports multi-line selection
- [x] Smart Backspace — outdent indented list markers
- [x] Smart Backspace — delete top-level list markers (replace with whitespace)
- [x] Smart Backspace — remove task list checkbox
- [x] New File templates — right-click > New > Markdown File with template chooser (Blank, README, Document, Meeting Notes, Changelog, API Documentation)
- [x] Generate menu (`Cmd+N` / `Alt+Insert`) — Table, Code Block, Image, Footnote, Link Reference, Front Matter, Table of Contents
- [ ] Ordered list auto-renumbering after indent/outdent
- [ ] Tests for all list editing scenarios

## Phase 3: Table of Contents ✅
TOC generation and management.

- [x] Create TOC from headings (insert at cursor)
- [x] Update existing TOC
- [x] TOC heading level filtering (e.g., 2..4)
- [x] Slug generation (GitHub mode)
- [x] Additional slug modes (GitLab, Gitea, Azure DevOps, Bitbucket Cloud)
- [x] Section numbering (add/remove)
- [x] Auto-update TOC on save (configurable)
- [x] Heading omission via comment markers (`<!-- omit from toc -->`, `<!-- omit in toc -->`)
- [x] Range-based heading omission (`<!-- omit from toc start -->` / `<!-- omit from toc end -->`)
- [x] TOC region markers (`<!-- TOC -->` / `<!-- /TOC -->`)
- [x] Duplicate heading slug handling (-1, -2 suffixes)
- [x] Setext heading support
- [x] Code block and front matter exclusion
- [x] Named TOCs with attribute overrides (`name`, `type`, `level`)
- [x] TOC content ranges for scoping named TOCs to document sections
- [x] Multiple TOCs per document with independent settings
- [x] Per-level ordered list numbering (branch-specific counters)
- [x] Tests for TOC generation, slug modes, and heading extraction

## Phase 4: Table Formatting ✅
GFM table auto-formatting.

- [x] Parse GFM table structure
- [x] Auto-format table with padding and alignment
- [x] Detect and preserve column alignment (`:---`, `:--:`, `---:`)
- [x] Format on action / format on save
- [x] Tab key navigation between cells
- [x] Tests for table formatting

## Phase 5: Auto-Completion ✅
Completion providers for markdown-specific content.

- [x] File/image path completion (triggered by `(`, `[`)
- [x] Reference link label completion
- [x] Heading reference completion (for `[text](#heading)`)
- [x] Smart paste — paste URL over selected text creates `[text](url)`
- [x] Tests for completion providers

## Phase 6: Toolbars, Context Actions & Preview ✅
In-editor toolbars, context menus, and JCEF-based markdown preview.

- [x] Right-click context menu — context-aware actions based on selection, cursor position (table/TOC awareness)
- [x] Floating selection toolbar — Notion-style popup on text selection (Bold, Italic, Strikethrough, Code, Headings, Task Toggle)
- [x] Editor notification toolbar — top-of-editor bar with formatting, power tools, and settings links
- [x] JCEF markdown preview — live HTML preview with document change listener
- [x] CSS theme system — GitHub, GitHub Dark, GitLab, VSCode, Auto (follows IDE theme)
- [x] Custom CSS override — user-specified CSS file path in settings
- [x] Preview zoom — zoom in/out/reset support
- [x] Split editor — editor | preview | split mode via TextEditorWithPreview
- [x] Markdown-to-HTML converter — full GFM support without external dependencies
- [x] Tests for HTML converter (22 test cases)

## Phase 7: Export & Preview Enhancements ✅
HTML export and preview improvements.

- [x] Export current file to HTML
- [x] Batch export (folder of .md files)
- [x] Image path resolution (absolute/base64)
- [x] Styled HTML output with themes
- [x] Link validation on export
- [x] Tests for HTML export

## Phase 8: Polish & Release ✅
Final polish, documentation, and marketplace publishing.

- [x] Comprehensive settings page with all options
- [x] README with feature documentation
- [x] README screenshots (placeholder section — actual screenshots require running IDE)
- [x] CHANGELOG maintenance
- [x] Plugin icon and branding (pluginIcon.svg + pluginIcon_dark.svg)
- [x] JetBrains Marketplace submission (description, untilBuild, vendor metadata)
- [x] GitHub Actions CI/CD pipeline (with signing step)
- [x] Plugin signing configuration (env vars in build.gradle.kts + CI workflow)
- [x] Community contribution guidelines (CONTRIBUTING.md)

## Phase 9: Scroll Sync & Toolbar Rewrite ✅
Editor–preview scroll synchronization and architectural toolbar fix.


### 9A — Editor–Preview Scroll Synchronization ✅
- [x] Add `data-source-line` attributes to block-level HTML elements during markdown→HTML conversion
- [x] Editor→Preview sync: `VisibleAreaListener` on editor sends top visible line to JCEF via JavaScript `scrollToSourceLine()`
- [x] Preview→Editor sync: JavaScript scroll event reports visible source line back to Kotlin via `JBCefJSQuery`
- [x] Scroll-lock flag to prevent feedback loops (editor sets flag before programmatic preview scroll, and vice versa)
- [x] Debounce/throttle scroll events (50ms JS debounce + 200ms flag reset) to avoid excessive updates
- [x] Only active in split mode (both panels visible)
- [x] Settings toggle: "Synchronize editor and preview scroll position" (enabled by default)
- [x] Tests for source-line annotation in HTML converter output (11 new test cases)

### 9B — Toolbar Architecture Rewrite ✅
- [x] Move toolbar creation into `MarkdownSplitEditor` — override `getComponent()` to wrap the split editor with a toolbar panel at `NORTH`
- [x] Toolbar created synchronously in constructor — zero-latency, no notification system
- [x] Remove `EditorToolbarProvider` (the `EditorNotificationProvider` implementation)
- [x] Remove `ToolbarInitListener` (`FileEditorManagerListener` workaround)
- [x] Remove hierarchy listener hacks for window move/split/combine
- [x] Toolbar respects `toolbarEnabled` and `toolbarDisplayMode` settings
- [x] Toolbar updates on settings change via `MarkdownSettings.ChangeListener`
- [x] Floating toolbar (selection popup) remains unchanged — it's architecturally sound
- [x] Verify toolbar survives split/unsplit, tab drag, window move, group operations
- [x] Tests for toolbar presence and button state

## Phase 10: Math & LaTeX Support ✅
Full math editing and rendering to match VSCode parity.

- [x] Toggle Math action (`Ctrl+M`) — cycles: no math → `$...$` (inline) → `$$...$$` (display)
- [x] Toggle Math Reverse action (command palette) — cycles in opposite direction
- [x] KaTeX rendering in preview — bundled KaTeX JS/CSS library in JCEF, render `$...$` and `$$...$$` as formatted equations via auto-render
- [x] Math-aware syntax highlighting — annotator patterns for `$` and `$$` delimiters, distinct text attributes for math delimiters and content
- [x] Math completion provider — LaTeX command autocompletion inside math environments (170+ commands: Greek letters, operators, relations, arrows, symbols, accents, delimiters, environments)
- [x] Build toolbar button for math functions
- [x] Include Math functions in right-click menu markdown options
- [x] Include Math functions in hamburger menu in toolbar
- [x] Settings: `mathEnabled` toggle (enabled by default)
- [x] Tests for math toggle action, math rendering, and math completions (20 new test cases)

## Phase 11: Advanced List Editing ✅
List editing enhancements for full VSCode parity.

- [x] Toggle List action — cycle current line through marker candidates (`-`, `*`, `+`, `1.`, `1)`)
- [x] Configurable marker candidates setting (`listToggleCandidates`)
- [x] Move line up/down (`Alt+Up/Down`) with ordered list auto-renumber
- [x] Copy line up/down (`Shift+Alt+Up/Down`) with ordered list auto-renumber
- [x] `Ctrl+Enter` to exit list continuation (insert plain line)
- [x] `Shift+Enter` for soft break within list item (trailing double-space + newline)
- [x] Tab/Shift+Tab for list indent/outdent (context-aware: list vs table)
- [x] Ordered list marker style setting — "ordered" (incrementing: 1. 2. 3.) vs "one" (always 1.)
- [x] Ordered list auto-renumbering after indent/outdent
- [x] Toolbar icons — unordered list and ordered list icons with dark variants; Toggle List button added to editor toolbar
- [x] Tests for toggle list, ordered list renumbering, and marker parsing

## Phase 12: Editor Decorations & Visual Theming ✅
Visual decorations in the editor (not just preview) for richer editing experience.

- [x] Code span background — inline `code` gets visible background tint in the editor (light/dark aware)
- [x] Strikethrough rendering — `~~text~~` shown with actual STRIKEOUT text effect in editor
- [x] Formatting mark dimming — `**`, `~~`, `*`, `_` markers rendered in muted foreground color
- [x] Trailing space shading — trailing whitespace highlighted with background color
- [x] Hard line break indicator — trailing double-space highlighted with distinct blue-tinted background
- [x] Link visual distinction — separate styling for link text vs URL portion (existing from Phase 1)
- [x] Per-decoration toggle settings (enable/disable each decoration type individually)
- [x] Color Settings Page — Settings > Editor > Color Scheme > Markdown for full theme customization
- [x] Decoration file size limit setting — skip decorations on files larger than configurable threshold
- [x] Tests for decoration colors, patterns, and TextAttributesKey definitions

## Phase 13: Export & Preview Polish ✅
Minor export improvements and preview conveniences.

- [x] Auto-export on save — `exportOnSave` setting with `FileDocumentManagerListener` to generate HTML alongside `.md` on save
- [x] HTML title from comment — `<!-- title: Your Title -->` sets `<title>` in exported HTML (highest priority, before heading extraction)
- [x] `.md` → `.html` link conversion in export — `convertMdLinksToHtml()` rewrites internal markdown links, preserving anchor fragments
- [x] Pure HTML export mode — `exportPureCss` setting skips all theme and custom CSS in the exported document
- [x] Auto-show preview on file open — `autoShowPreview` setting controls whether split editor starts in split or editor-only mode
- [x] Zola slug mode — `slugifyZola()` with alphanumeric + hyphen + underscore, collapsed hyphens, trimmed edges
- [x] Tests for title extraction, link conversion, Zola slugs, and export options

## Phase 14: Diagram Rendering (Mermaid & Beyond) ← NEXT
Render diagram code blocks as visual diagrams in the preview.

- [ ] Mermaid diagram rendering — detect ```` ```mermaid ```` blocks, render via embedded Mermaid.js in JCEF
- [ ] PlantUML rendering — detect ```` ```plantuml ```` blocks, render via PlantUML library or server
- [ ] Graceful fallback — show raw code if rendering library fails to load
- [ ] Diagram theme integration — diagrams respect the selected preview theme (light/dark)
- [ ] Tests for diagram detection and rendering

## Phase 15: Table Column & Row Operations ✅
Advanced table editing to match JetBrains bundled plugin parity.

- [x] Insert row above/below cursor
- [x] Insert column before/after cursor
- [x] Delete current row
- [x] Delete current column
- [x] Swap columns left/right
- [x] Swap rows up/down
- [x] Set column alignment (left/center/right/none) via action
- [x] Register table actions in Markdown menu (Table submenu) and context menu
- [x] Context-aware: actions only enabled when cursor is inside a table
- [x] Tests for all table operations, column index detection, and alignment changes

## Phase 16: Structure View & Code Folding ✅
IDE integration features for navigation and document management.

- [x] Structure view — heading hierarchy in Structure tool window (`PsiStructureViewFactory` + `StructureViewModelBase`)
- [x] Go To Symbol support — headings searchable via `Ctrl+Shift+Alt+N` (`ChooseByNameContributorEx`)
- [x] Code folding — collapsible regions for headings, code fences, front matter, and long blockquotes (`FoldingBuilderEx`)
- [x] Breadcrumb support — show heading path in editor breadcrumb bar (`BreadcrumbsInfoProvider`)
- [x] Tests for structure view tree building, folding patterns, and breadcrumb path resolution

## Phase 17: IncrementalDOM Preview & Performance
Replace full HTML replacement with efficient DOM patching for large document performance.

- [ ] Implement IncrementalDOM-based preview updates — parse HTML diff, generate JS patch calls
- [ ] Preserve scroll position across preview updates (no flicker on edit)
- [ ] Preview static server — serve local resources via `httpRequestHandler` instead of `file://` URLs
- [ ] CSP headers and security hardening for preview content
- [ ] Debounced preview updates with visual loading indicator for very large documents
- [ ] Performance benchmarks: measure preview update latency for 100/500/1000/5000-line documents
- [ ] Tests for incremental update correctness

## Phase 18: Preview Navigation & Link Handling
Browser-style navigation within the JCEF preview, with configurable link behavior.

- [ ] Preview link behavior setting — choose between "Open in editor" (current: opens linked `.md` files in PHPStorm editor) and "Browser-style" (navigates within the JCEF preview like a web browser)
- [ ] Browser-style navigation — render linked `.md` files directly in the JCEF preview panel with full HTML conversion
- [ ] Document history stack — track visited documents for back/forward navigation within the preview
- [ ] Back/Forward toolbar buttons — visible navigation bar when in browser-style mode (existing `createNavBar()` infrastructure)
- [ ] "Return to source" button — return from browsed document to the file open in the editor
- [ ] Breadcrumb trail — show navigation path of visited documents in the preview toolbar
- [ ] Anchor fragment support — when navigating to `file.md#heading`, scroll to the heading after loading
- [ ] Relative path resolution — resolve `../`, `./`, and nested paths correctly in both modes
- [ ] Non-markdown file handling — open non-`.md` links (images, PDFs, etc.) in appropriate IDE viewer or system app
- [ ] Tests for link resolution, history stack, and navigation mode switching

## Phase 19: Extension Point Architecture
Open the plugin for third-party extensions.

- [ ] Define `previewExtensionProvider` EP — inject JS/CSS into JCEF preview (for math, diagrams, custom renderers)
- [ ] Define `fenceLanguageProvider` EP — map custom info strings to rendering logic
- [ ] Define `exportProvider` EP — additional export formats (PDF, DOCX)
- [ ] Define `markdownFlavourProvider` EP — override parsing behavior per file
- [ ] All extension points marked `dynamic="true"` for hot-swap
- [ ] Documentation for extension point API (contributor guide)
- [ ] Migrate built-in math/diagram support to use own extension points (dogfooding)
- [ ] Tests for extension point loading and lifecycle

## Phase 20: Language Injection in Code Fences
Full IDE language support inside fenced code blocks (JetBrains' marquee feature).

- [ ] `MultiHostInjector` implementation for code fence content ranges
- [ ] Map fence info strings to IntelliJ `Language` instances (with alias resolution)
- [ ] Syntax highlighting from target language inside code fences in the editor
- [ ] Completion from target language inside code fences
- [ ] Error highlighting toggle (`showProblemsInCodeBlocks` setting) — opt-in to avoid noise
- [ ] Proper PSI range mapping between fence content and injected language fragments
- [ ] Tests for injection activation, highlighting, and completion in common languages (Kotlin, Java, SQL, JSON, HTML)

## Phase 21: Project-Level Settings & Advanced Configuration
Per-project markdown configuration for multi-project workflows.

- [ ] Migrate settings from application-level to project-level (`Service.Level.PROJECT`)
- [ ] Update all `getInstance()` calls to `getInstance(project)`
- [ ] Settings inheritance — project settings override application defaults
- [ ] Front matter parsing — extract YAML/TOML metadata for document properties
- [ ] Registry keys for experimental features (boundary scroll, etc.)
- [ ] Settings import/export for team sharing
- [ ] Tests for project-level settings isolation

## Phase 22: Marketplace Screenshots & Release
Final marketing assets and JetBrains Marketplace submission. Only after the plugin is battle-tested and peer-reviewed.

- [ ] Create `docs/screenshots/` directory for all marketing assets
- [ ] Capture required screenshots (see docs/005-marketplace-assets.md for full list)
- [ ] Create 1-2 animated GIFs demonstrating key workflows
- [ ] Update README.md with inline screenshots
- [ ] Update plugin description in plugin.xml with screenshot references (raw GitHub URLs)
- [ ] Document screenshot capture instructions for future updates
- [ ] Final peer review and community feedback round
- [ ] JetBrains Marketplace submission

## Phase 23: Addendums & UX Polish ✅
Fixes and additions identified during real-world usage.

- [x] Fix floating selection toolbar — rewrote using platform `ActionToolbar` + `JBPopup` with debounce timer for reliable display
- [x] Insert Link action (`Ctrl+K`) — dialog with text, URL, and optional title fields; pre-fills selected text
- [x] Insert Image action (`Ctrl+Shift+K`) — dialog with alt text, image path/URL, and optional title fields
- [x] Link and image toolbar icons (chain link + landscape) with dark variants
- [x] Link and image buttons in editor toolbar, floating toolbar, and right-click context menu

## Phase 24: Context-Sensitive Floating Toolbar ✅
Show different toolbar actions based on the selection context instead of a generic formatting palette.

- [x] Context detection engine (`SelectionContext`) — analyzes selection and surrounding text to determine context
- [x] **Default context** (plain text) — Bold, Italic, Strikethrough, Code, Heading Up/Down, Link, Image
- [x] **TOC context** — selection overlaps `<!-- TOC -->...<!-- /TOC -->` → Update TOC, Add/Remove Section Numbers
- [x] **Table context** — selection inside table rows → Format Table, Insert/Delete Row/Column
- [x] **Code block context** — selection inside fenced code block → Toggle Code Block (unwrap)
- [x] **Math context** — selection inside `$...$` or `$$...$$` → Toggle Math, Toggle Math Reverse
- [x] **Blockquote context** — selection inside `> ` lines → Toggle Blockquote (unwrap)
- [x] Context priority rules: TOC > Code Block > Table > Math > Blockquote > Default
- [x] Settings toggle — `contextSensitiveToolbar` (default on), falls back to default toolbar when off
- [x] Tests for TOC detection, code block detection, math detection, and priority rules

## Future Considerations
- Custom markdown-it extensions integration
- Multi-language support (i18n beyond English)
- Vim mode compatibility
- R Markdown / Quarto language support
- GFM Alerts rendering (NOTE, TIP, IMPORTANT, WARNING, CAUTION)
- PDF/DOCX export via extension point providers
- Stub-based indexing for fast heading navigation without full AST parsing
- Spellcheck integration with markdown-aware word boundaries
