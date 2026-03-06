# Changelog

All notable changes to Markdown All-in-One for PHPStorm will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.2.0] - 2026-03-06

### Changed
- All keyboard shortcuts now use **chord prefix** (`Cmd+M` / `Ctrl+M`) to avoid conflicts with PHPStorm built-in keybindings
- Standalone architecture — removed dependency on IntelliJ's bundled Markdown plugin
- Task list toggle shortcut changed from `Alt+C` to `Cmd+M, X`

### Added
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
