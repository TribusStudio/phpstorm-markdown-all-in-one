# Contributing to Markdown All-in-One

Thank you for your interest in contributing to Markdown All-in-One for PHPStorm.

## Development

All builds run inside a Docker devcontainer — no JDK or Gradle required on your machine.

### Prerequisites
- Docker

### Quick Start
```bash
./dev up        # Start the dev container
./dev setup     # Generate Gradle wrapper, download dependencies
./dev build     # Build the plugin
./dev test      # Run tests
```

### All Commands
```bash
# Container management
./dev up          # Start container
./dev down        # Stop container
./dev status      # Show status and versions
./dev shell       # Open shell in container
./dev destroy     # Remove container + cache

# Build & test
./dev build       # Build the plugin
./dev test        # Run all tests
./dev package     # Build distributable .zip
./dev verify      # Verify IDE compatibility
./dev clean       # Clean build artifacts
./dev gradle ...  # Run any Gradle task
```

See [docs/004-devcontainer-setup.md](docs/004-devcontainer-setup.md) for full details.

### Running the Plugin
```bash
./dev gradle runIde
```
This launches a sandboxed PHPStorm instance with the plugin loaded.

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

## Workflow

1. **Create a branch** from `main` using the naming convention:
   - `feature/description` for new features
   - `fix/description` for bug fixes

2. **Write tests** for your changes. Every feature needs unit tests; editor actions need integration tests.

3. **Follow Kotlin conventions** and the project's coding style (see CLAUDE.md).

4. **Use Conventional Commits** for commit messages:
   ```
   feat(actions): add toggle underline action
   fix(list): correct ordered list renumbering at indent level 3
   ```

5. **Open a Pull Request** against `main` with a clear description and test plan.

## Reporting Issues

Please use GitHub Issues. Include:
- PHPStorm version
- Plugin version
- Steps to reproduce
- Expected vs actual behavior
- Sample markdown file (if applicable)
