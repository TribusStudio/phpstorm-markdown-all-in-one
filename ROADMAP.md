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

## Phase 6: Toolbars and Context Actions ← NEXT
In editor-mode toolbars for quick and easy access to common text formatting and layout functions.

- [ ] Right click Context Action menu for selection and mouse position function awareness
- [ ] Hover toolbar for text formatting on seletion
- [ ] Top of Editor window toolbar with text formatting, power tools, settings, quick access to common tasks
- [ ] Top of Preview window toolbar with text sizing (+/- zoom size), render style (github, gitlab, vscode, etc)
- [ ] Top right of editor window toolbar will show, like the standard JetBrains Markdown plugin, three states for viewing a markdown file "editor | preview | split mode"
- [ ] Tests for toolbar layout and placement along with icons

## Phase 7: Export & Preview Enhancements
HTML export and preview improvements.

- [ ] Export current file to HTML
- [ ] Batch export (folder of .md files)
- [ ] Image path resolution (absolute/base64)
- [ ] Styled HTML output with themes
- [ ] Link validation on export
- [ ] Tests for HTML export

## Phase 8: Polish & Release
Final polish, documentation, and marketplace publishing.

- [ ] Comprehensive settings page with all options
- [x] README with feature documentation
- [ ] README screenshots
- [x] CHANGELOG maintenance
- [ ] Plugin icon and branding
- [ ] JetBrains Marketplace submission
- [x] GitHub Actions CI/CD pipeline
- [ ] Plugin signing configuration
- [x] Community contribution guidelines (CONTRIBUTING.md)

## Future Considerations
- Math/LaTeX support (KaTeX rendering)
- Syntax decorations (visual indicators for formatting marks)
- Mermaid diagram preview
- Custom markdown-it extensions integration
- Multi-language support (i18n beyond English)
- Vim mode compatibility
