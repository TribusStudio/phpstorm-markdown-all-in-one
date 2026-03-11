# Markdown All-in-One — Project Memory

## Project Identity
- PHPStorm plugin for comprehensive Markdown editing
- Package: `com.tribus.markdown`, Plugin ID: `com.tribus.markdown-all-in-one`
- Kotlin 2.x on IntelliJ Platform Gradle Plugin 2.11.0
- Target: PHPStorm 2025.1+ (JDK 21)
- License: MIT, open-source on GitHub

## Reference Implementation
- VSCode Markdown All-in-One at `/Users/wilco/Docker/tribus/development/vscode-markdown`
- 19 commands, 24 keyboard shortcuts, 29+ settings
- Key files: formatting.ts, listEditing.ts, toc.ts, tableFormatter.ts, completion.ts

## Architecture Decisions
- STANDALONE — no dependency on IntelliJ's bundled Markdown plugin
- Registers own file type for .md/.markdown/.mdown/.mkd/.mkdn extensions
- Full language stack: lexer, parser, syntax highlighter, annotator
- Users must disable bundled "Markdown" plugin to avoid file type conflicts
- Language ID is "MarkdownAIO" (not "Markdown") to prevent crash if both plugins loaded
- Uses UI DSL v2 for settings (required for 2025.1+)
- All formatting actions share `BaseToggleFormattingAction` base class
- All actions implement `MarkdownAction` marker interface
- `MarkdownActionPromoter` promotes our actions over IDE builtins in .md files only
- Direct shortcuts (Cmd+B, Cmd+I, etc.) — NOT chord shortcuts (user prefers direct)
- plugin.xml keymap: use `ctrl` (NOT `meta`) in `$default` keymap — Mac keymap auto-remaps ctrl→Cmd; `meta` maps to Ctrl on Mac
- Component-level registration via EditorFactoryListener overrides IDE builtins in markdown files
- Runtime shortcut defaults use `Toolkit.menuShortcutKeyMaskEx` (META on Mac, CTRL on Win/Linux)
- Chord shortcuts (Cmd+M prefix) were tried and rejected — Cmd+M conflicts in PHPStorm
- Version management: `scripts/bump-version.sh [major|minor|patch]`
- ALWAYS bump version in gradle.properties before pushing — CI has no auto-increment; PHPStorm won't detect updates without a new version number
- Smart key handlers: TypedHandler, EnterHandler, BackspaceHandler

## Dev Environment
- Docker devcontainer (eclipse-temurin:21-jdk-jammy)
- `./dev` CLI on host for all container/build commands
- Gradle wrapper generated inside container (`./dev setup`)
- Named volume `markdown-plugin-gradle-cache` for dependency caching
- No JDK/Gradle needed on host — only Docker

## Development Phases
1. Foundation/MVP — formatting actions, settings (current)
2. Smart List Editing — enter/tab/backspace handlers
3. Table of Contents — generation, update, slug modes
4. Table Formatting — GFM auto-format
5. Auto-Completion — file paths, links, headings
6. Export — HTML export with styling
7. Polish & Release — marketplace submission

## User Preferences
- Full automation authorized (commits, pushes, docs, testing)
- Phased development with roadmap
- Developer + user-voice review agents for quality
- Conventional Commits format
- ALL plans and documentation MUST live in the repo (never external/ephemeral)
- Docs in `docs/` are numbered with prefix `001-`, `002-`, etc. in creation order
- Root convention files (README, CHANGELOG, CONTRIBUTING, ROADMAP, LICENSE) are NOT numbered
