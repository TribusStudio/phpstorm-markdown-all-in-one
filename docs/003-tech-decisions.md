# 003 — Technical Decisions

**Date:** 2026-03-06
**Status:** Active

## Language: Kotlin 2.x

**Decision:** Use Kotlin as the sole implementation language.

**Rationale:**
- Kotlin is the recommended language for new IntelliJ plugins (per JetBrains)
- Kotlin 2.x is required when targeting IntelliJ Platform 2025.1+
- Null safety, coroutines, and data classes are ideal for plugin development
- JetBrains' own plugins are increasingly written in Kotlin

## Build System: Gradle + IntelliJ Platform Gradle Plugin 2.11.0

**Decision:** Use Gradle with the IntelliJ Platform Gradle Plugin 2.x series.

**Rationale:**
- The 1.x plugin is obsolete
- 2.11.0 is the latest stable version (January 2026)
- Provides `runIde`, `buildPlugin`, `verifyPlugin`, `publishPlugin` tasks
- Supports plugin signing and marketplace publishing

## Target: PHPStorm 2025.1+

**Decision:** Target PHPStorm 2025.1 as the minimum supported version.

**Rationale:**
- 2025.1 requires Kotlin 2.x and JDK 21 — aligns with our stack
- Kotlin UI DSL v1 is deprecated from 2025.1 — we use v2 exclusively
- No need to support older versions for a new plugin
- Current PHPStorm: 2025.3.3

## Standalone Plugin (No Dependency on Bundled Markdown)

**Decision:** Do NOT depend on `org.intellij.plugins.markdown`. Our plugin is fully standalone.

**Rationale:**
- Full control over the editing experience — shortcuts, settings, behavior
- No risk of the bundled plugin's updates breaking our features
- True "All-in-One" — we own the entire experience
- We can progressively add our own parsing, highlighting, and preview without conflicts

**File type ownership:**
- We register our own file type (`MarkdownAIO`) for `.md`, `.markdown`, `.mdown`, `.mkd`, `.mkdn` extensions
- We provide our own lexer, parser, syntax highlighter, and annotator
- Users must disable the bundled "Markdown" plugin to avoid file type conflicts
- Language ID is `MarkdownAIO` (not `Markdown`) to prevent `ImplementationConflictException` if both plugins are loaded simultaneously

**Keyboard shortcut strategy:**
- We use familiar shortcuts (Cmd+B for bold, Cmd+I for italic, etc.) that overlap with IDE builtins
- All our actions implement the `MarkdownAction` marker interface
- `MarkdownActionPromoter` (registered as an `actionPromoter` extension) reorders conflicting actions so ours take priority **only when editing markdown files**
- In non-markdown files, the standard IDE actions (Go to Declaration, Find Implementations, etc.) work as normal
- This is the standard IntelliJ Platform mechanism for context-aware shortcut resolution

**Previous decisions (superseded):**
- Originally depended on `org.intellij.plugins.markdown` — changed because it made us beholden to another plugin's decisions
- Then went standalone without file type registration (extension-based detection only) — changed because PHPStorm kept showing "Plugins supporting *.md files found" and users had no syntax highlighting without the bundled plugin

## Testing: JUnit 5

**Decision:** Use JUnit 5 with IntelliJ Platform test fixtures.

**Rationale:**
- JUnit 5 is the current standard for IntelliJ plugin testing
- IntelliJ's Starter framework (2025) exclusively supports JUnit 5
- `BasePlatformTestCase` provides editor simulation for integration tests

## CI/CD: GitHub Actions

**Decision:** Use GitHub Actions for build, test, and release automation.

**Rationale:**
- Free for open-source projects
- Native Gradle support via `gradle/actions/setup-gradle`
- Can automate marketplace publishing on tagged releases

## License: MIT

**Decision:** MIT license for maximum openness.

**Rationale:**
- Permissive, well-understood, no friction for contributors
- Compatible with JetBrains Marketplace requirements
