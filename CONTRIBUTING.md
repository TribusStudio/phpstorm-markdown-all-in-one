# Contributing to Markdown All-in-One

Thank you for your interest in contributing to Markdown All-in-One for PHPStorm.

## Getting Started

### Prerequisites
- JDK 21
- Gradle 9.2.1+ (or use the included Gradle wrapper)
- IntelliJ IDEA or PHPStorm (for running/debugging the plugin)

### Setup
```bash
git clone https://github.com/tribus/phpstorm-markdown-all-in-one.git
cd phpstorm-markdown-all-in-one
./gradlew build
```

### Running the Plugin
```bash
./gradlew runIde
```
This launches a sandboxed PHPStorm instance with the plugin loaded.

### Running Tests
```bash
./gradlew test
```

## Development Workflow

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

## Project Structure

See CLAUDE.md for the full project structure and conventions.

## Reporting Issues

Please use GitHub Issues. Include:
- PHPStorm version
- Plugin version
- Steps to reproduce
- Expected vs actual behavior
- Sample markdown file (if applicable)
