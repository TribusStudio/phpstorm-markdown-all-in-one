# Markdown All-in-One for PHPStorm

A comprehensive Markdown editing plugin for PHPStorm, built on the IntelliJ Platform.

## Project Overview

This is a free, open-source PHPStorm plugin that provides a superior Markdown editing experience — inspired by VSCode's Markdown All-in-One but purpose-built for the JetBrains ecosystem.

**Repository:** GitHub (public, open-source, MIT license)
**Target IDE:** PHPStorm 2025.1+ (IntelliJ Platform 2025.1+)
**Language:** Kotlin (with Kotlin 2.x as required by IntelliJ 2025.1+)
**Build System:** Gradle with IntelliJ Platform Gradle Plugin 2.11.0

## Tech Stack

- **Language:** Kotlin 2.x
- **Build:** Gradle 9.2.1+ with IntelliJ Platform Gradle Plugin 2.11.0
- **Testing:** JUnit 5 with IntelliJ Platform test fixtures
- **CI/CD:** GitHub Actions
- **Minimum Target:** PHPStorm 2025.1 (build 251.*)
- **JDK:** 21 (required for IntelliJ 2024.2+)

## Project Structure

```
phpstorm-markdown-all-in-one/
  build.gradle.kts          # Build configuration
  settings.gradle.kts       # Project settings
  gradle.properties         # Gradle and plugin properties
  gradle/
    libs.versions.toml      # Version catalog
  src/
    main/
      kotlin/com/tribus/markdown/   # Plugin source code
        actions/             # Editor actions (bold, italic, lists, etc.)
        editor/              # Editor enhancements (auto-pairs, formatting)
        highlighting/        # Syntax highlighting extensions
        completion/          # Code completion providers
        inspection/          # Inspections and quick-fixes
        preview/             # Live preview tool window
        settings/            # Plugin settings/preferences
        toc/                 # Table of contents generation
        util/                # Shared utilities
      resources/
        META-INF/
          plugin.xml         # Plugin descriptor
        messages/            # i18n message bundles
        icons/               # Plugin icons
    test/
      kotlin/com/tribus/markdown/   # Test sources
      testData/              # Test fixture data (sample .md files)
```

## Coding Conventions

- **Style:** Follow Kotlin coding conventions (https://kotlinlang.org/docs/coding-conventions.html)
- **Naming:** PascalCase for classes, camelCase for functions/properties, SCREAMING_SNAKE for constants
- **Packages:** `com.tribus.markdown.*` — mirror the directory structure above
- **Plugin ID:** `com.tribus.markdown-all-in-one`
- **No wildcard imports** — use explicit imports
- **Use IntelliJ Platform APIs** — never reinvent what the platform provides
- **Extension points must be dynamic** — mark all extension points with `dynamic="true"` where possible
- **UI:** Use Kotlin UI DSL v2 (required for 2025.1+)
- **Coroutines:** Use `kotlinx.coroutines` with IntelliJ's coroutine scope for async work

## Git Workflow

- **Branch model:** `main` (stable) + feature branches (`feature/xxx`), bugfix branches (`fix/xxx`)
- **Commit messages:** Conventional Commits format: `type(scope): description`
  - Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `build`, `ci`, `chore`
  - Example: `feat(actions): add toggle bold keyboard shortcut`
- **PRs:** All changes via PR with description and test plan
- **Tags:** Semantic versioning `vMAJOR.MINOR.PATCH` for releases

## Testing Requirements

- Every new feature MUST have unit tests
- Editor actions must have integration tests using `BasePlatformTestCase`
- Test data goes in `src/test/testData/` as `.md` fixture files
- Use `before.md` / `after.md` naming for transformation tests
- Run tests before marking any feature complete: `./gradlew test`

## Development Environment

All builds run inside a Docker devcontainer. Use the `./dev` CLI from the host:

### Container Management
- **Start:** `./dev up`
- **Stop:** `./dev down`
- **Status:** `./dev status`
- **Shell:** `./dev shell`
- **Setup:** `./dev setup` (generates Gradle wrapper, downloads deps)
- **Destroy:** `./dev destroy` (removes container + cache volume)

### Build & Test (run via `./dev`)
- **Build:** `./dev build`
- **Test:** `./dev test`
- **Package:** `./dev package` (creates distributable .zip)
- **Verify:** `./dev verify` (plugin compatibility check)
- **Clean:** `./dev clean`
- **Any Gradle command:** `./dev gradle <task>`

### Direct Gradle (inside container only)
- `./gradlew build`
- `./gradlew test`
- `./gradlew runIde`
- `./gradlew buildPlugin`
- `./gradlew verifyPlugin`
- `./gradlew publishPlugin` (requires marketplace token)

## Documentation Rules

- **All plans and documentation MUST live in the repo** — never in external tools, local-only files, or ephemeral locations. Everything is tracked in version control.
- **Documentation files are numbered** with a prefix indicating the order they were written: `001-`, `002-`, etc. This applies to all docs in the `docs/` directory (plans, specs, research, decisions, guides).
- **Core root files** (`README.md`, `CHANGELOG.md`, `CONTRIBUTING.md`, `ROADMAP.md`, `LICENSE`) are NOT numbered — they follow standard open-source conventions.
- **When creating a new document**, check the highest existing number in `docs/` and increment by one.
- **The `docs/` directory structure:**
  ```
  docs/
    001-project-vision.md          # Initial vision and goals
    002-vscode-reference-analysis.md  # Analysis of the VSCode reference plugin
    003-feature-spec-*.md          # Feature specifications
    ...
  ```

## Claude Workflow Preferences

- **Automation:** Commits, pushes, docs, testing, and planning are all authorized for automation
- **Autonomy:** Operate autonomously; ask questions when hitting walls or ambiguity
- **Planning:** Use phased roadmap development — complete phases incrementally
- **Agents:** Use developer and user-voice agents for review (see .claude/ skills)
- **Documentation:** Keep README, CHANGELOG, help text, and numbered docs updated with each feature
- **Transparency:** Log decisions and rationale in commit messages and PR descriptions
