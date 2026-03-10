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

## Phase 9: Scroll Sync, Toolbar Rewrite & Marketplace Assets ← NEXT
Editor–preview scroll synchronization, architectural toolbar fix, and marketing assets for marketplace publishing.

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

### 9C — Marketplace Screenshots & Animated Demos
- [ ] Create `docs/screenshots/` directory for all marketing assets
- [ ] Capture required screenshots (see docs/005-marketplace-assets.md for full list)
- [ ] Create 1-2 animated GIFs demonstrating key workflows
- [ ] Update README.md with inline screenshots
- [ ] Update plugin description in plugin.xml with screenshot references (raw GitHub URLs)
- [ ] Document screenshot capture instructions for future updates

## Phase 10: Math & LaTeX Support
Full math editing and rendering to match VSCode parity.

- [ ] Toggle Math action (`Ctrl+M`) — cycles: no math → `$...$` (inline) → `$$...$$` (display)
- [ ] Toggle Math Reverse action (command palette) — cycles in opposite direction
- [ ] KaTeX rendering in preview — embed KaTeX JS library in JCEF, render `$...$` and `$$...$$` as formatted equations
- [ ] Math-aware syntax highlighting — lexer tokens for `$` and `$$` delimiters, distinct text attributes for math content
- [ ] Math completion provider — LaTeX command autocompletion inside math environments (e.g., `\frac`, `\sum`, `\alpha`, `\int`)
- [ ] Settings: `mathEnabled` toggle, `katexMacros` for custom macro definitions
- [ ] Tests for math toggle action, math rendering, and math completions

## Phase 11: Advanced List Editing
List editing enhancements for full VSCode parity.

- [ ] Toggle List action — cycle current line through marker candidates (`-`, `*`, `+`, `1.`, `1)`)
- [ ] Configurable marker candidates setting (`list.toggleCandidateMarkers`)
- [ ] Move line up/down (`Alt+Up/Down`) with ordered list auto-renumber
- [ ] Copy line up/down (`Shift+Alt+Up/Down`) with ordered list auto-renumber
- [ ] `Ctrl+Enter` to exit list continuation (insert plain line)
- [ ] `Shift+Enter` for soft break within list item (insert `<br>` or trailing double-space)
- [ ] Tab/Shift+Tab for list indent/outdent (context-aware: list vs table)
- [ ] Ordered list marker style setting — "ordered" (incrementing: 1. 2. 3.) vs "one" (always 1.)
- [ ] Adaptive list indentation — align sublists with parent content instead of fixed tab size
- [ ] Ordered list auto-renumbering after indent/outdent (carried from Phase 2)
- [ ] Tests for all new list editing behaviors

## Phase 12: Editor Decorations & Visual Theming
Visual decorations in the editor (not just preview) for richer editing experience.

- [ ] Code span background/border — inline `` `code` `` gets visible background and border in the editor
- [ ] Strikethrough rendering — `~~text~~` shown with line-through text decoration in editor
- [ ] Formatting mark dimming — `**`, `~~`, `` ` `` markers rendered in muted/faded foreground color
- [ ] Trailing space shading — trailing whitespace highlighted with background color
- [ ] Hard line break indicator — visual marker for trailing double-space line breaks
- [ ] Link visual distinction — separate styling for link text vs URL portion
- [ ] Per-decoration toggle settings (enable/disable each decoration type individually)
- [ ] Color scheme keys in plugin descriptor for theme integration
- [ ] Decoration file size limit setting — skip decorations on large files for performance
- [ ] Tests for decoration rendering

## Phase 13: Export & Preview Polish
Minor export improvements and preview conveniences.

- [ ] Auto-export on save — `exportOnSave` setting to automatically generate HTML when saving `.md` files
- [ ] HTML title from comment — `<!-- title: Your Title -->` sets `<title>` in exported HTML
- [ ] `.md` → `.html` link conversion in export — internal markdown links rewritten for HTML output
- [ ] Pure HTML export mode — export without any CSS stylesheets
- [ ] Auto-show preview on file open — `autoShowPreview` setting to open split preview automatically
- [ ] Zola slug mode — additional slugify mode for Zola static site generator
- [ ] Tests for new export features and slug mode

## Phase 14: Diagram Rendering (Mermaid & Beyond)
Render diagram code blocks as visual diagrams in the preview.

- [ ] Mermaid diagram rendering — detect ```` ```mermaid ```` blocks, render via embedded Mermaid.js in JCEF
- [ ] PlantUML rendering — detect ```` ```plantuml ```` blocks, render via PlantUML library or server
- [ ] Graceful fallback — show raw code if rendering library fails to load
- [ ] Diagram theme integration — diagrams respect the selected preview theme (light/dark)
- [ ] Tests for diagram detection and rendering

## Future Considerations
- Custom markdown-it extensions integration
- Multi-language support (i18n beyond English)
- Vim mode compatibility
- R Markdown / Quarto language support
- GFM Alerts rendering (NOTE, TIP, IMPORTANT, WARNING, CAUTION)
