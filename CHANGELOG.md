# Changelog

All notable changes to Markdown All-in-One for PHPStorm will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.22.0] - 2026-04-01

### Added
- **Context-sensitive floating toolbar** — the floating selection toolbar now shows different actions based on what you've selected:
  - **TOC block** — Update TOC, Add/Remove Section Numbers
  - **Table** — Format Table, Insert/Delete Row, Insert/Delete Column
  - **Code block** — Toggle Code Block (unwrap)
  - **Math** — Toggle Math, Toggle Math Reverse
  - **Blockquote** — Toggle Blockquote (unwrap)
  - **Default** — formatting, headings, links, images (unchanged)
- **Context detection engine** (`SelectionContext`) with priority rules: TOC > Code Block > Table > Math > Blockquote > Default
- **Settings toggle** — `contextSensitiveToolbar` (default on) to fall back to the generic toolbar

## [0.21.7] - 2026-04-01

### Fixed
- **Floating toolbar and keyboard shortcuts restored** — the `applicationListeners` XML registration for `EditorFactoryListener` was silently not firing in PhpStorm 2026.1. Switched to the `<editorFactoryListener>` extension point which registers directly with `EditorFactory`. This fixes both the floating selection toolbar and component-level keyboard shortcut registration (Ctrl+B for bold, etc.)

## [0.21.3] - 2026-04-01

### Fixed
- **Floating toolbar visibility** — reverted from `contentComponent.add()` (which doesn't paint in IntelliJ's custom editor pipeline) back to `JBPopup` with `setCancelOnClickOutside(false)` to coexist with the intention lightbulb. 200ms debounce for responsive feel

## [0.21.2] - 2026-04-01

### Fixed
- **Floating toolbar reliability** — replaced `JBPopup` approach with direct `JLayeredPane` overlay. The popup system was conflicting with IntelliJ's intention/lightbulb system, causing the toolbar to be dismissed or never shown. The toolbar is now added directly to the editor's layered pane as an overlay component, bypassing the popup layer entirely

## [0.21.1] - 2026-04-01

### Fixed
- **Floating selection toolbar** — rewrote using platform `ActionToolbar` inside a `JBPopup` with a 200ms debounce timer. The previous custom `JButton` approach was not rendering reliably. Now shows consistently when text is selected with proper IDE styling

### Added
- **Insert Link action** (`Ctrl+K`) — opens a dialog with Text, URL, and optional Title fields. Pre-fills selected text as the link text. Inserts `[text](url)` or `[text](url "title")`
- **Insert Image action** (`Ctrl+Shift+K`) — opens a dialog with Alt Text, Image Path/URL, and optional Title fields. Pre-fills selected text as alt text. Inserts `![alt](src)` or `![alt](src "title")`
- **Link and image toolbar icons** — chain link icon and landscape/photo icon with dark theme variants
- **Link and image buttons** — added to the editor toolbar, floating selection toolbar, and right-click context menu

## [0.21.0] - 2026-04-01

### Added
- **Table row operations** — Insert Row Above, Insert Row Below, Delete Row, Move Row Up, Move Row Down
- **Table column operations** — Insert Column Before, Insert Column After, Delete Column, Move Column Left, Move Column Right
- **Column alignment actions** — Set Align Left, Center, Right, or None for the column at cursor
- **Table submenu** — all operations registered in Markdown > Table submenu and right-click context menu (context-aware: only enabled when cursor is inside a table)
- **TableOperations utility** — immutable table transformations returning new Table instances for all row/column/alignment operations

## [0.20.0] - 2026-04-01

### Added
- **Auto-export HTML on save** — new `exportOnSave` setting automatically generates an `.html` file alongside the `.md` file when saving. Disabled by default
- **HTML title from comment** — `<!-- title: Your Title -->` sets the `<title>` in exported HTML, taking priority over heading extraction
- **`.md` → `.html` link conversion in export** — internal markdown links (`[text](file.md)`) are rewritten to point to `.html` files in exported output, preserving anchor fragments
- **Pure HTML export mode** — new `exportPureCss` setting exports without any theme CSS stylesheets — just the raw HTML body
- **Auto-show preview on file open** — new `autoShowPreview` setting controls whether the split editor starts in split mode (default) or editor-only mode
- **Zola slug mode** — new slugify mode for the Zola static site generator: alphanumeric + hyphens + underscores, collapsed hyphens, trimmed edges. Available in TOC settings

## [0.19.4] - 2026-03-31

### Fixed
- **Relative file links in preview** — clicking a relative link like `01-reconnaissance.md` in the preview now opens the target file in PHPStorm's editor (with split preview) instead of showing `ERR_FILE_NOT_FOUND`. Links with anchor fragments (e.g., `file.md#heading`) are supported. External URLs continue to open in the system browser

## [0.19.3] - 2026-03-30

### Fixed
- **Blockquote code block rendering** — fenced code blocks inside blockquotes (`> ````) now render correctly in the preview. The blockquote handler was rewritten to strip `>` prefixes and recursively convert the inner content, so headings, lists, code blocks, and other block-level elements inside blockquotes all render properly

### Added
- **Toggle Blockquote action** (`Ctrl+Shift+.`) — toggles `> ` prefix on the current line or selection. Available in the editor toolbar, right-click context menu, and Tools > Markdown menu
- **Blockquote icon** — toolbar icon (vertical bar + text lines) with dark theme variant

## [0.19.2] - 2026-03-30

### Fixed
- **Code span background covers full span** — the background tint now covers the entire code span including backticks for a uniform visual (matching GitHub/VS Code rendering). The previous approach of excluding backticks created a three-tone boundary artifact. Also fixed `enforcedTextAttributes` to only set `backgroundColor` without overriding font style

## [0.19.1] - 2026-03-30

### Fixed
- **Decoration settings now refresh immediately** — toggling decoration settings on/off now restarts `DaemonCodeAnalyzer` so annotations re-run without needing to reopen the file
- **Code span background no longer covers backticks** — the background tint is now applied only to the content between backticks, eliminating the discoloration artifact at the backtick boundary

## [0.19.0] - 2026-03-30

### Added
- **Code span background** — inline `` `code` `` gets a subtle background tint in the editor, theme-aware (light gray / dark gray)
- **Strikethrough rendering** — `~~text~~` shows an actual STRIKEOUT text effect in the editor, not just a color change
- **Formatting marker dimming** — `**`, `~~`, `*`, `_` markers rendered in a muted foreground color so content stands out
- **Trailing space indicator** — trailing whitespace highlighted with a soft background color
- **Hard line break indicator** — trailing double-space (renders as `<br>`) highlighted with a distinct blue-tinted background
- **Per-decoration toggle settings** — each decoration type can be individually enabled/disabled in Settings > Editor Decorations
- **Color Settings Page** — all markdown colors customizable in Settings > Editor > Color Scheme > Markdown (22 configurable attributes)
- **Decoration file size limit** — decorations automatically skipped on files larger than configurable threshold (default 500K chars)

## [0.18.2] - 2026-03-30

### Fixed
- **Math icon centering** — replaced the `<text>` sigma with a `<path>` sigma, properly centered in the 16x16 viewBox. Eliminates font-dependent rendering inconsistencies across platforms

## [0.18.1] - 2026-03-30

### Added
- **List toolbar icons** — unordered list (bullet dots) and ordered list (1. 2. 3.) SVG icons with dark theme variants
- **Toggle List button in toolbar** — the editor toolbar's Lists group now includes a Toggle List button to cycle list markers without a keyboard shortcut

## [0.18.0] - 2026-03-30

### Added
- **Toggle List action** — cycle the current line's marker through configurable candidates (`-`, `*`, `+`, `1.`, `1)`). When the last candidate is reached, the marker is removed. Available via Markdown > Toggle List Marker in the Tools menu
- **Move line up/down** (`Alt+Up/Down`) — moves the current line or selection and auto-renumbers any affected ordered lists
- **Copy line up/down** (`Shift+Alt+Up/Down`) — duplicates the current line or selection and auto-renumbers ordered lists
- **Exit list continuation** (`Ctrl+Enter`) — inserts a plain newline without a list marker, breaking out of a list mid-stream
- **Soft break** (`Shift+Enter`) — inserts two trailing spaces followed by a newline, rendering as `<br>` in HTML without starting a new list item
- **Tab/Shift+Tab list indent/outdent** — Tab and Shift+Tab are now context-aware: indent/outdent list items when on a list line, navigate table cells when in a table
- **Ordered list marker style setting** — choose between "ordered" (1. 2. 3.) and "one" (always 1.) for ordered list continuation on Enter
- **Toggle list candidates setting** — configurable comma-separated list of markers to cycle through with Toggle List
- **Ordered list auto-renumbering after indent/outdent** — when auto-renumber is enabled, indent/outdent now renumbers affected ordered list items automatically

## [0.17.4] - 2026-03-12

### Fixed
- **Structure View restored** — the `ToolbarTextEditor` wrapper now explicitly overrides all `FileEditor` Java default methods (`getStructureViewBuilder`, `getState`, `setState`, `getBackgroundHighlighter`, `getCurrentLocation`, `selectNotify`, `deselectNotify`). Kotlin's `by` delegation only covers abstract methods — Java default methods were silently inherited, causing `getStructureViewBuilder()` to return `null` instead of forwarding to the real editor. This broke the Structure View, code folding, and other editor features that depend on these methods

## [0.17.3] - 2026-03-12

### Fixed
- **ToolbarTextEditor `getFile()` override** — added explicit `getFile()` override to the `ToolbarTextEditor` wrapper. Kotlin's `by` delegation doesn't cover Java default interface methods, causing a `PluginException` at editor creation (#9)

## [0.17.2] - 2026-03-12

### Fixed
- **Toolbar scoped to editor pane** — the formatting toolbar now lives inside the editor pane instead of spanning the full split editor width. Fixes toolbar buttons (hamburger menu, settings gear) not working when the preview pane has focus (#8). In preview-only mode the toolbar hides naturally with the editor

## [0.17.1] - 2026-03-11

### Fixed
- **Structure View navigation** — clicking a heading in the Structure View now correctly jumps the editor cursor to that heading's line. The `navigate()` method was previously a no-op stub
- **Structure View labels** — removed redundant `#` hash prefixes from heading labels since the tree nesting already communicates hierarchy

### Added
- **Architecture guide in CONTRIBUTING.md** — comprehensive walkthrough of the plugin's bootstrap sequence, flat PSI design, two-layer highlighting, shortcut override mechanism, and all major subsystems

## [0.17.0] - 2026-03-11

### Added
- **Structure View** — heading hierarchy in the Structure tool window (`Cmd+7` / `Alt+7`) with level-based icons and nesting. Powered by `PsiStructureViewFactory` using the existing `HeadingExtractor`
- **Go To Symbol** — all markdown headings are searchable via `Ctrl+Shift+Alt+N` across the entire project
- **Code Folding** — collapsible regions for heading sections (folds content under each heading), fenced code blocks (with language in placeholder), YAML front matter, and multi-line blockquotes
- **Breadcrumbs** — editor breadcrumb bar shows the heading hierarchy path at the current cursor position

## [0.16.3] - 2026-03-11

### Fixed
- **TOC markers inside code blocks ignored** — `<!-- TOC -->` and `<!-- /TOC -->` markers inside fenced code blocks (backtick and tilde) are now skipped by the TOC generator, preventing false TOC detection when documenting TOC syntax in markdown

### Changed
- **Development docs moved to CONTRIBUTING.md** — the Development section and project structure have been moved from README.md to CONTRIBUTING.md, keeping the README focused on features and installation

## [0.16.2] - 2026-03-11

### Changed
- **Toolbar rewritten using IntelliJ ActionToolbar** — the editor toolbar now uses the platform's native `ActionToolbar` instead of custom `JButton` components. This gives proper icon sizing, DPI scaling, spacing, hover states, and separator rendering that matches the IDE's design language (Bookmarks, VCS, etc.)
- **Dark theme icon variants** — all 13 toolbar SVG icons now have `_dark.svg` variants with the standard IntelliJ dark icon color (`#AFB1B3`), automatically selected by the platform based on the active theme
- **Action icons in plugin.xml** — toolbar actions now declare their icons in the plugin descriptor, enabling proper icon display in menus, keymap settings, and the Find Action dialog
- **Tools popup uses ActionGroup** — the "More" tools menu is now a proper `DefaultActionGroup` popup instead of a raw `JPopupMenu`, gaining consistent styling and keyboard navigation
- **Distinct code block icon** — the code block action now has its own icon (rounded rectangle with code lines) instead of sharing the `</>` icon with code span
- **Removed "Button display" setting** — the toolbar display mode setting (icons/labels/icons and labels) was removed since the native `ActionToolbar` handles display automatically

### Fixed
- **CI build cache staleness** — added `clean` and `--no-build-cache` to the CI workflow to prevent stale Gradle build cache entries from causing `NoSuchMethodError` failures when data class constructors change

## [0.16.1] - 2026-03-11

### Fixed
- **Code span priority in preview** — markdown syntax inside backtick code spans (e.g., `` `![alt](` ``) is now rendered as literal text instead of being processed as images/links/bold. Uses placeholder extraction to comply with CommonMark code span priority rules
- **Preview-to-editor scroll sync accuracy** — scroll position reported from the preview to the editor now uses linear interpolation between bracketing source-line elements, instead of snapping to the nearest earlier element. This significantly improves accuracy for long paragraphs and code blocks

## [0.16.0] - 2026-03-11

### Added
- **Math & LaTeX support** — full math editing and rendering using the bundled KaTeX library
  - Toggle Math action (`Ctrl+M`) — cycles: plain text → `$inline$` → `$$display$$` → plain text
  - Toggle Math Reverse action — cycles in opposite direction (available via Tools menu and command palette)
  - KaTeX rendering in preview — `$...$` (inline) and `$$...$$` (display) math expressions render as formatted equations
  - Math-aware syntax highlighting — `$` and `$$` delimiters and math content highlighted with distinct colors
  - LaTeX completion — 170+ commands (Greek letters, operators, relations, arrows, symbols, accents, delimiters, environments) auto-complete inside math environments when typing `\`
  - Math toolbar button, right-click context menu entry, and tools menu entries
  - Settings toggle: "Enable math rendering in preview" (enabled by default)

## [0.15.2] - 2026-03-11

### Added
- **External link handling** — clicking links in the preview now opens them in the system browser instead of navigating the preview pane away from the document. Anchor links (`#heading`) scroll smoothly within the preview. A `CefRequestHandler` blocks external navigation as a safety net, and a `JBCefJSQuery` bridge intercepts clicks at the JavaScript level for reliable handling

### Changed
- **Navigation bar** — a hidden nav bar (Back, Forward, Return to Preview, Open in Browser) is wired into the preview panel, ready to appear if the JCEF browser ever navigates away from the preview content

## [0.15.1] - 2026-03-10

### Fixed
- **Loose list rendering** — ordered and unordered lists with blank lines between items now render as a single list instead of being split into separate lists (which reset numbering to 1). The converter now looks ahead past blank lines to determine if the list continues
- **Preview scroll position preserved on update** — the preview no longer jumps to the top of the document after every edit. The editor's current visible line is tracked and restored after the full HTML reload

## [0.15.0] - 2026-03-10

### Added
- **Bidirectional scroll synchronization** — scrolling the editor automatically scrolls the preview to the matching position, and vice versa. Uses source-line mapping (`data-source-line` attributes) for accurate synchronization with debounced scroll events and feedback-loop prevention. Configurable via Settings > Preview > "Synchronize editor and preview scroll position"

### Fixed
- **Escaped brackets in TOC links** — TOC entries with headings containing `\[` and `\]` (e.g., keyboard shortcuts like `Ctrl+Shift+]` inside code spans) now render correctly as clickable links in the preview. Expanded backslash escape handling to all CommonMark escapable punctuation characters

### Changed
- **Toolbar architecture rewrite** — the editor toolbar is now embedded directly in `MarkdownSplitEditor` instead of using `EditorNotificationProvider`. This eliminates the toolbar load delay, and the toolbar no longer disappears during tab drag, window split, or group operations. Removed `EditorToolbarProvider`, `ToolbarInitListener`, and hierarchy listener workarounds

## [0.14.3] - 2026-03-10

### Fixed
- **Relative images now actually render in preview** — JCEF's `loadHTML(html, baseUrl)` does not reliably resolve local file paths; image `src` attributes are now resolved to absolute `file://` URLs before loading, reusing the export engine's path resolution logic

## [0.14.2] - 2026-03-10

### Fixed
- **Escaped pipes `\|` in tables** — table parser (both formatter and HTML converter) now correctly treats `\|` as a literal pipe character instead of a cell delimiter; preview renders `\|` as `|`
- **Table separator width** — in compact mode, separator dashes match header cell widths instead of the widest data cell

### Added
- **Smart table width limiting** — new "Max table width" setting (Settings > Table Formatting). When a table exceeds this column limit, cells use compact spacing (no extra padding) instead of full-width alignment. Set to 0 for no limit (default), or use 80/120 to match your editor margin guides.

## [0.14.1] - 2026-03-10

### Fixed
- **Relative images now render in preview** — `loadHTML()` now passes the file's parent directory as base URL so paths like `assets/photo.png` resolve correctly
- **Code block syntax highlighting** — bundled highlight.js 11.9.0 with GitHub and GitHub Dark themes; fenced code blocks with language tags (e.g., ` ```php `) now render with colored syntax highlighting
- Highlight theme auto-selects light or dark variant based on the active preview theme

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
