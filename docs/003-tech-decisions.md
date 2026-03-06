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

## Extending the Bundled Markdown Plugin

**Decision:** Depend on `org.intellij.plugins.markdown` (IntelliJ's bundled Markdown plugin) rather than building from scratch.

**Rationale:**
- The bundled plugin provides PSI tree, parsing, basic highlighting, and preview
- We extend it with editor actions, smart editing, and productivity features
- Avoids duplicating significant parser/renderer work
- Users get our features on top of the existing Markdown support they already have

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
