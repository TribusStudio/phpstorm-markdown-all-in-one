# Markdown All-in-One for PHPStorm — Development Roadmap

## Phase 1: Foundation (MVP)
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
- [x] Verify build compiles and passes in devcontainer (47 tests, 0 failures)

## Phase 2: Smart List Editing
Context-aware list editing behavior.

- [ ] Smart Enter key — auto-continue lists (unordered, ordered, task)
- [ ] Smart Enter — auto-continue blockquotes
- [ ] Smart Enter — remove empty list item on double-enter
- [ ] Tab/Shift+Tab — indent/outdent list items
- [ ] Smart Backspace — remove empty list markers
- [ ] Ordered list auto-renumbering
- [ ] Adaptive indentation (CommonMark-aware)
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
