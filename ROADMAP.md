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
- [x] Version bump script (`scripts/bump-version.sh`)

## Phase 2: Smart List Editing ← CURRENT
Context-aware list editing behavior.

- [ ] Smart Enter — auto-continue unordered lists (`-`, `+`, `*`)
- [ ] Smart Enter — auto-continue ordered lists (`1.`, `2)`)
- [ ] Smart Enter — auto-continue task lists (`- [ ]`, `- [x]`)
- [ ] Smart Enter — auto-continue blockquotes (`> `)
- [ ] Smart Enter — remove empty list item on double-enter (outdent or delete marker)
- [ ] Tab — indent list items
- [ ] Shift+Tab — outdent list items
- [ ] Smart Backspace — outdent indented list markers
- [ ] Smart Backspace — delete top-level list markers (replace with whitespace)
- [ ] Smart Backspace — remove task list checkbox
- [ ] Ordered list auto-renumbering after enter/indent/outdent
- [ ] Tests for all list editing scenarios

## Phase 3: Table of Contents
TOC generation and management.

- [ ] Create TOC from headings (insert at cursor)
- [ ] Update existing TOC
- [ ] TOC heading level filtering (e.g., 2..4)
- [ ] Slug generation (GitHub mode)
- [ ] Additional slug modes (GitLab, Azure DevOps, Bitbucket)
- [ ] Section numbering (add/remove)
- [ ] Auto-update TOC on save (configurable)
- [ ] Heading omission via comment markers
- [ ] Tests for TOC generation and slug modes

## Phase 4: Table Formatting
GFM table auto-formatting.

- [ ] Parse GFM table structure
- [ ] Auto-format table with padding and alignment
- [ ] Detect and preserve column alignment (`:---`, `:--:`, `---:`)
- [ ] Format on action / format on save
- [ ] Tab key navigation between cells
- [ ] Tests for table formatting

## Phase 5: Auto-Completion
Completion providers for markdown-specific content.

- [ ] File/image path completion (triggered by `(`, `[`)
- [ ] Reference link label completion
- [ ] Heading reference completion (for `[text](#heading)`)
- [ ] Smart paste — paste URL over selected text creates `[text](url)`
- [ ] Tests for completion providers

## Phase 6: Export & Preview Enhancements
HTML export and preview improvements.

- [ ] Export current file to HTML
- [ ] Batch export (folder of .md files)
- [ ] Image path resolution (absolute/base64)
- [ ] Styled HTML output with themes
- [ ] Link validation on export
- [ ] Tests for HTML export

## Phase 7: Polish & Release
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
