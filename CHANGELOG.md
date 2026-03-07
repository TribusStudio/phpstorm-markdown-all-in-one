# Changelog

All notable changes to Markdown All-in-One for PHPStorm will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
