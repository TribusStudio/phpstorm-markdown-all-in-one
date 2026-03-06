# Markdown All-in-One for PHPStorm

A comprehensive Markdown editing plugin for JetBrains PHPStorm — inspired by the best of VSCode's Markdown experience, purpose-built for the IntelliJ Platform.

## Features

### Keyboard Shortcuts

All shortcuts use a **chord** prefix (`Cmd+M` on macOS, `Ctrl+M` on Windows/Linux) to avoid conflicts with PHPStorm built-in keybindings. Press the prefix, release, then press the second key.

| Shortcut | Action |
|----------|--------|
| `Cmd+M, B` | Toggle **bold** |
| `Cmd+M, I` | Toggle *italic* |
| `Cmd+M, S` | Toggle ~~strikethrough~~ |
| `Cmd+M, `` ` `` | Toggle `code span` |
| `Cmd+M, C` | Toggle code block |
| `Cmd+M, ]` | Increase heading level |
| `Cmd+M, [` | Decrease heading level |
| `Cmd+M, X` | Toggle task list checkbox |

### Smart List Editing
- Auto-continue lists on Enter (unordered, ordered, task lists)
- Tab/Shift+Tab to indent/outdent list items
- Smart backspace to remove empty list markers
- Ordered list auto-renumbering

### Table of Contents
- Generate TOC from document headings
- Auto-update on save
- Configurable heading level range
- Multiple slug generation modes (GitHub, GitLab, Azure DevOps, etc.)

### Table Formatting
- Auto-format GFM tables with proper alignment
- Preserve column alignment markers

### Auto-Completion
- File and image path completion
- Reference link label completion
- Heading reference completion

### Smart Paste
- Paste a URL with text selected to create a markdown link

### HTML Export
- Export markdown to styled HTML

## Installation

### From JetBrains Marketplace
1. Open PHPStorm > Settings > Plugins
2. Search for "Markdown All-in-One"
3. Click Install

### From Source
```bash
git clone https://github.com/tribus/phpstorm-markdown-all-in-one.git
cd phpstorm-markdown-all-in-one
./dev up && ./dev setup
./dev package
```
Install the resulting `.zip` from `build/distributions/` via Settings > Plugins > Install from disk.

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

## Contributing

Contributions are welcome. Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

MIT License. See [LICENSE](LICENSE) for details.
