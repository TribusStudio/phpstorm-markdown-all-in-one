# Changelog

All notable changes to Markdown All-in-One for PHPStorm will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.14.0] - 2026-03-09

### Added
- **Comprehensive settings page** — all plugin options now exposed in Settings > Languages > Markdown All-in-One:
  - TOC unordered list marker (`-`, `*`, `+`)
  - Format tables on save (separate from table formatter enabled)
  - Toolbar enabled/disabled toggle
  - Export options: embed images as base64, validate links on export
  - Descriptive comments on all settings
- **Plugin icon and branding** — 40x40 `pluginIcon.svg` and `pluginIcon_dark.svg` for JetBrains Marketplace listing
- **Marketplace-ready plugin description** — expanded `<description>` in plugin.xml with full feature list and getting started guide
- **Plugin signing in CI** — GitHub Actions workflow signs the plugin when `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, and `PRIVATE_KEY_PASSWORD` secrets are configured
- **IDE compatibility range** — `untilBuild` set to `261.*` for clear version compatibility

## [0.13.2] - 2026-03-09

### Fixed
- **Toolbar now appears immediately on file open** — replaced broken `EditorFactoryListener` trigger (where `editor.project` is null) with a `FileEditorManagerListener.fileOpened()` callback that fires after the FileEditor tab is fully constructed

## [0.13.1] - 2026-03-09

### Fixed
- **TOC anchor links now match heading IDs** — HTML converter reuses `Slugify.slugify()` so TOC `#anchor` references and heading `id` attributes are always identical
- **Nested list rendering in preview** — indented list items now produce proper nested `<ol>`/`<ul>` elements instead of a flat list; TOC with multiple heading levels renders correctly
- **Toolbar loads faster** — uses `ApplicationManager.invokeLater` with `ModalityState.any()` targeting only the specific file instead of iterating all projects via `SwingUtilities.invokeLater`
- **TOC toolbar button** — changed from "Create TOC" to "Update Table of Contents" (`UpdateToc` action) with descriptive tooltip
- **Table toolbar button tooltip** — changed from "Table" to "Format Table" to clarify it formats the table at cursor

## [0.13.0] - 2026-03-09

### Added
- **Export to HTML** — export the current markdown file to a styled HTML document via file save dialog (Tools > Markdown > Export to HTML)
- **Batch export to HTML** — export all markdown files in a folder (with recursive subdirectory support) to HTML (Tools > Markdown > Batch Export to HTML)
- **Image path resolution** — relative image paths in exported HTML are resolved to absolute `file://` paths; optional base64 embedding for self-contained documents
- **Styled HTML output** — exported HTML includes the selected preview theme CSS (GitHub, GitHub Dark, GitLab, VSCode, Auto) plus any custom CSS overrides
- **Link validation on export** — validates anchor links (#headings), relative file links, and reference-style link definitions; warnings displayed after export
- **Document title extraction** — exported HTML `<title>` is set from the first heading (ATX or Setext), falling back to the filename
- 22 unit tests for HTML export (single file, batch, image resolution, link validation, title extraction)

## [0.12.4] - 2026-03-09

### Fixed
- **Toolbar persists across window operations** — `HierarchyListener` detects when the editor component's showing state changes (window move, split, combine) and re-triggers `EditorNotifications.updateNotifications()` to recreate the toolbar

### Added
- **Better toolbar button grouping** — buttons organized into logical groups: Text Formatting (Bold, Italic, Strikethrough, Code, Code Block) | Structure (H+, H-) | Lists (Indent, Outdent, Task) | Tables & TOC (Format Table, Create TOC)
- **Missing toolbar buttons** — added Code Block, List Indent, List Outdent, and Task Toggle to the toolbar
- **Tools popup menu** — `...` button next to settings gear opens a dropdown with: Update TOC, Create TOC, Format Table at Cursor, Format All Tables, Add/Remove Section Numbers, Export to HTML
- Uses `AllIcons.Actions.More` for the tools dropdown and `AllIcons.Actions.MoveRight`/`MoveLeft` for list indent/outdent

## [0.12.3] - 2026-03-09

### Fixed
- **Toolbar display mode setting now sticks** — comboBox values are stored and read in consistent lowercase; previously a case mismatch caused "icons" mode to not be recognized
- **Toolbar appears immediately on file open** — `EditorNotifications.updateAllNotifications()` is triggered from `editorCreated()` so the toolbar renders as soon as the editor opens, not on the next lazy evaluation cycle
- **Toolbar rebuilds on settings change** — applying settings now calls `EditorNotifications.updateAllNotifications()` across all open projects so display mode changes take effect immediately

## [0.12.2] - 2026-03-09

### Fixed
- **Toolbar hover state** — buttons now show a translucent highlight and hand cursor on hover; disabled buttons suppress hover entirely
- **Toolbar appears immediately** — buttons start enabled instead of waiting for a focus event that fires after construction
- **Tighter button spacing** — switched from `FlowLayout` to `BoxLayout` with 24x24 fixed-size icon buttons and minimal padding

## [0.12.1] - 2026-03-09

### Fixed
- **Toolbar rewritten as custom panel** — proper `BorderLayout` with left-justified action buttons and right-justified settings gear (`AllIcons.General.GearPlain`)
- **Toolbar icons now render** — replaced `EditorNotificationPanel` text links with actual `JButton` components that display SVG icons via `IconLoader`
- **Toolbar buttons disabled when preview focused** — action buttons are focus-aware; disabled when editor loses focus, re-enabled on focus gain
- **Backtick rendering in preview** — double/triple backtick code spans (`` `` ` `` ``) now render correctly per CommonMark spec
- **Preview hot-swaps CSS on settings change** — changing the render theme in settings immediately re-renders the preview without needing to edit the document

### Added
- **Toolbar display mode setting** — choose between Icons (default), Labels, or Icons and Labels (`Settings > Toolbar > Button display`)
- **Settings change listener** — `MarkdownSettings` now notifies subscribers when settings are applied, enabling live preview updates
- 2 new unit tests for multi-backtick inline code rendering

## [0.12.0] - 2026-03-08

### Added
- **Right-click context menu** — context-aware Markdown submenu in the editor popup with formatting actions (shown on selection), table actions (shown when cursor is in a table), and TOC actions (shown when TOC exists)
- **Floating selection toolbar** — Notion-style popup toolbar appears on text selection with quick-access buttons for Bold, Italic, Strikethrough, Code, Heading Up/Down, and Task Toggle
- **Editor notification toolbar** — persistent toolbar at the top of markdown editors with formatting shortcuts (B, I, S, Code, H+, H-), power tools (Table, TOC), and Settings link
- **JCEF markdown preview** — live HTML preview panel using Chromium Embedded Framework with automatic refresh on document changes
- **Split editor** — editor/preview/split mode toggle via IntelliJ's TextEditorWithPreview (same UX as the built-in Markdown plugin)
- **CSS theme system** — preview supports GitHub, GitHub Dark, GitLab, VSCode themes, plus Auto mode that follows the IDE's light/dark theme
- **Custom CSS override** — specify a `.css` file path in settings for additional style overrides on the preview
- **Preview zoom** — zoom in, zoom out, and reset zoom level on the preview panel
- **Markdown-to-HTML converter** — full GFM-compatible converter (headings, bold, italic, strikethrough, code, links, images, lists, task lists, blockquotes, tables with alignment, horizontal rules, front matter skipping) with no external dependencies
- 22 unit tests for the HTML converter covering all supported markdown elements

## [0.11.0] - 2026-03-08

### Added
- **Heading reference completion** — type `[text](#` to get a dropdown of all document headings with their slugs
- **Reference link label completion** — type `[text][` to get a dropdown of all `[label]: url` definitions in the document
- **File/image path completion** — type `[text](` or `![alt](` to get file/directory suggestions from the current file's location
- **Smart paste** — paste a URL while text is selected to create `[selected text](url)`; image URLs produce `![text](url)` (configurable in settings)
- Unit tests for completion pattern matching, slug generation, and smart paste logic

## [0.10.0] - 2026-03-08

### Added
- **GFM table parsing** — full parser for GitHub Flavored Markdown tables with header, separator, and data rows
- **Table auto-formatting** — pads cells to uniform column widths with consistent spacing
- **Column alignment detection and preservation** — `:---` (left), `:---:` (center), `---:` (right) markers are maintained during formatting
- **Format Table action** (`Ctrl+Shift+Alt+T`) — format the table at cursor position
- **Format All Tables action** — format every table in the document
- **Format tables on save** — auto-format all tables when saving (configurable via settings)
- **Tab navigation in tables** — Tab moves to next cell, Shift+Tab moves to previous cell, wrapping between rows
- Unit tests for table parsing and formatting (20+ test cases)

## [0.9.0] - 2026-03-08

### Added
- **Named TOCs with attribute overrides** — `<!-- TOC name="api" type="ordered" level="2..4" -->` for per-TOC settings
- **TOC content ranges** — `<!-- toc range name="api" start -->` / `<!-- toc range end -->` to scope a named TOC to specific document sections
- **Multiple TOCs per document** — each independently configured and auto-updated
- Auto-update-on-save now updates all TOC blocks in the document

### Fixed
- **Ordered TOC numbering** — per-level counters instead of global index (child sections restart at 1)

## [0.8.1] - 2026-03-07

### Added
- **TOC omit range markers** — `<!-- omit from toc start -->` / `<!-- omit from toc end -->` to exclude entire sections of headings from the TOC (also supports `omit in toc` variant, case-insensitive)

### Fixed
- **Settings not saving** — clicking Apply/OK in the settings dialog now correctly persists values (bold indicator, TOC levels, etc.)
- **CI test failures** — `TocGenerator.generateWithMarkers()` and `updateExistingToc()` no longer NPE in environments without IntelliJ application context

## [0.8.0] - 2026-03-07

### Added
- **Table of Contents** — generate TOC from headings, insert at cursor with `<!-- TOC -->` / `<!-- /TOC -->` markers
- **Update TOC** — update existing TOC manually or auto-update on save (configurable)
- **Heading level filtering** — configure which heading levels to include (e.g., `2..4`)
- **Slug generation** — GitHub mode (default) plus GitLab, Gitea, Azure DevOps, Bitbucket Cloud
- **Duplicate heading handling** — auto-appends `-1`, `-2` suffixes for duplicate anchors
- **Heading omission** — `<!-- omit from toc -->` / `<!-- omit in toc -->` comment markers (inline or previous line)
- **Section numbering** — Add/Remove hierarchical section numbers on headings
- **Setext heading support** — TOC extraction handles both ATX (`#`) and Setext (underlined) headings
- Skips headings inside fenced code blocks, YAML front matter, and HTML comments
- Comprehensive tests for slugification, heading extraction, and TOC generation

## [0.7.0] - 2026-03-07

### Added
- **New File templates** — right-click > New > Markdown File with template chooser (Blank, README, Document, Meeting Notes, Changelog, API Documentation)
- **Generate menu** — `Cmd+N` / `Alt+Insert` in editor shows Markdown submenu with: Table, Code Block, Image, Footnote, Link Reference, Front Matter, Table of Contents
- **List indent/outdent** — `Cmd+]` / `Cmd+[` (macOS) or `Ctrl+]` / `Ctrl+[` (Win/Linux)
- Release notes now include commit log since previous tag

## [0.6.0] - 2026-03-07

### Added
- **Smart Enter key** — auto-continues unordered lists (`-`, `+`, `*`), ordered lists (`1.`, `2)`), task lists (`- [ ]`, `- [x]`), and blockquotes (`> `)
- **Empty list item handling** — pressing Enter on an empty list item outdents (if indented) or removes the marker (if top-level); task checkboxes are removed first
- **Smart Backspace** — outdents indented list markers, replaces top-level markers with spaces, removes task checkboxes
- **List indent/outdent** — `Cmd+]` / `Cmd+[` (macOS) or `Ctrl+]` / `Ctrl+[` (Win/Linux) to indent/outdent list items; supports multi-line selection
- Ordered list continuation auto-increments the marker number
- Task list continuation resets checkbox to unchecked (`[ ]`)
- **New File templates** — right-click > New > Markdown File with template chooser (Blank, README, Document, Meeting Notes, Changelog, API Documentation)
- **Generate menu** — `Cmd+N` / `Alt+Insert` in editor shows Markdown submenu with: Table, Code Block, Image, Footnote, Link Reference, Front Matter, Table of Contents

## [0.5.1] - 2026-03-06

### Fixed
- Selection wrapping now uses `beforeSelectionRemoved` (not `beforeCharTyped`) — the selection is deleted before `beforeCharTyped` fires, which is why all previous approaches silently failed
- Follows the same proven pattern as the AsciiDoc IntelliJ plugin's `FormattingQuotedTypedHandler`
- Respects IDE Smart Keys setting ("Surround selection on typing quote or brace")
- Preserves selection direction (LTR/RTL) and sticky selection state after wrapping

### Changed
- Removed Swing InputMap/ActionMap approach (unnecessary with correct delegate method)
- Wrapping characters: `*`, `~`, `_`, `` ` ``, `|`, `-`

## [0.4.2] - 2026-03-06

### Fixed
- Selection wrapping now uses PsiFile check instead of editor.virtualFile (which can be null)
- Added file type check against MarkdownFileType for more reliable markdown file detection

## [0.4.1] - 2026-03-06

### Added
- Selection-aware character wrapping — select text and type `*`, `|`, `-`, `` ` ``, `~`, or `_` to wrap contextually
- Pipe wrapper creates table cells (`| text |`) with smart preceding-pipe detection
- Dash wrapper fills with dashes inside table cells for header borders

## [0.4.0] - 2026-03-06

### Fixed
- Keyboard shortcuts on macOS: changed plugin.xml keymap from `meta` to `ctrl` — IntelliJ's `$default` keymap auto-remaps `ctrl` to `Cmd` on Mac; using `meta` incorrectly mapped to `Ctrl`
- Switched from `FileEditorManagerListener` to `EditorFactoryListener` for more reliable component-level shortcut registration timing

## [0.3.3] - 2026-03-06

### Fixed
- Cmd+B (macOS) now correctly triggers Bold instead of GotoDeclaration
- Component-level shortcuts use platform-aware modifiers (Cmd on macOS, Ctrl on Windows/Linux)
- Fallback to hard-coded defaults when keymap shortcuts are removed due to conflict resolution

## [0.3.2] - 2026-03-06

### Fixed
- Keyboard shortcuts now definitively override IDE builtins in markdown files using component-level registration
- Replaced unreliable `ActionPromoter` approach with `FileEditorManagerListener` that registers shortcuts directly on the editor component — component-level shortcuts always take priority over global keymap shortcuts

## [0.3.1] - 2026-03-06

### Fixed
- Keyboard shortcuts (Cmd+B, Cmd+I, etc.) now properly override IDE builtins in markdown files
- Added `ActionPromoter.suppress()` to remove conflicting IDE actions (e.g. GotoDeclaration) when editing markdown
- Added `getActionUpdateThread()` override required by IntelliJ 2025.1+ for proper action update scheduling
- Changed `isEnabledAndVisible` to `isEnabled` so actions participate in shortcut conflict resolution even when disabled

### Added
- GitHub Actions release pipeline — auto-creates releases on push to main
- Custom plugin repository via GitHub Pages (`updatePlugins.xml`) for PHPStorm auto-updates

## [0.3.0] - 2026-03-06

### Added
- **Own file type registration** — plugin now claims `.md`, `.markdown`, `.mdown`, `.mkd`, `.mkdn` extensions
- Markdown lexer with block-level tokenization (headings, code fences, blockquotes, lists, horizontal rules, inline code spans)
- Syntax highlighter with theme-aware colors for all block constructs
- Inline formatting annotator for bold, italic, strikethrough, links, and images
- PSI file type (`MarkdownFile`) and parser definition

### Changed
- Plugin is now a **full language plugin** — no longer relies on the bundled Markdown plugin for file type association
- Users should disable the bundled "Markdown" plugin to avoid file type conflicts

## [0.2.1] - 2026-03-06

### Changed
- Reverted from chord shortcuts to direct shortcuts (Cmd+B, Cmd+I, etc.) using `ActionPromoter` for context-aware conflict resolution

## [0.2.0] - 2026-03-06

### Changed
- Standalone architecture — removed dependency on IntelliJ's bundled Markdown plugin
- Shortcuts now use `ActionPromoter` for context-aware priority — our actions override IDE builtins only in markdown files, no conflicts in other file types

### Added
- `MarkdownActionPromoter` — promotes plugin actions over IDE builtins when editing `.md` files
- `MarkdownAction` marker interface for all plugin actions
- Code span shortcut (`Cmd+`` ` ```) and code block shortcut (`Cmd+Shift+`` ` ```)
- Version bump script (`scripts/bump-version.sh`) for SEMVER management
- Versioning rule in CLAUDE.md

## [0.1.0] - 2026-03-06

### Added
- Project scaffold with Gradle, IntelliJ Platform Gradle Plugin 2.11.0
- Plugin descriptor (plugin.xml) with action registrations
- Toggle Bold, Italic, Strikethrough, Code Span, Code Block actions
- Heading level increase/decrease actions
- Task list checkbox toggle
- Plugin settings page with formatting, list, TOC, and table options
- Message bundle for i18n support
- Unit tests for formatting utilities
- GitHub Actions CI/CD pipeline
- Stub implementations for TOC, table formatting, and HTML export
- Docker devcontainer with JDK 21 build environment
- `./dev` CLI for container management and build commands
- Numbered documentation system in `docs/`
- Integration tests for all formatting actions (BasePlatformTestCase)
- Integration tests for code block, heading, and task list actions
- JUnit vintage engine for IntelliJ platform test compatibility
