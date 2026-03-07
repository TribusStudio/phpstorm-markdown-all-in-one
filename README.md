# Markdown All-in-One for PHPStorm

A comprehensive Markdown editing plugin for JetBrains PHPStorm — inspired by the best of VSCode's Markdown experience, purpose-built for the IntelliJ Platform.

## Features

### Keyboard Shortcuts

Shortcuts automatically take priority over built-in IDE actions when editing markdown files via `ActionPromoter`. In non-markdown files, the standard IDE shortcuts work as normal.

| Shortcut | Action |
|----------|--------|
| `Cmd/Ctrl+B` | Toggle **bold** |
| `Cmd/Ctrl+I` | Toggle *italic* |
| `Alt+S` | Toggle ~~strikethrough~~ |
| `Cmd/Ctrl+`` ` `` | Toggle `code span` |
| `Cmd/Ctrl+Shift+`` ` `` | Toggle code block |
| `Ctrl+Shift+]` | Increase heading level |
| `Ctrl+Shift+[` | Decrease heading level |
| `Cmd/Ctrl+]` | Indent list item |
| `Cmd/Ctrl+[` | Outdent list item |
| `Alt+C` | Toggle task list checkbox |

### Syntax Highlighting
- Headings, code blocks, inline code, blockquotes, list markers, horizontal rules
- Inline formatting: **bold**, *italic*, ~~strikethrough~~
- Links and images with distinct colors for text and URL
- Theme-aware — adapts to your IDE color scheme

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

### Auto-Update (Recommended)
1. Open PHPStorm > Settings > Plugins > gear icon > **Manage Plugin Repositories**
2. Add: `https://TribusStudio.github.io/phpstorm-markdown-all-in-one/updatePlugins.xml`
3. Click OK, then search for "Markdown All-in-One" in the Marketplace tab and install
4. **Disable the bundled "Markdown" plugin** (Settings > Plugins > Installed > search "Markdown" > Disable)
5. Restart PHPStorm

Updates will appear automatically in Settings > Plugins whenever a new version is pushed to main.

### From GitHub Release
1. Download the latest `.zip` from [Releases](https://github.com/TribusStudio/phpstorm-markdown-all-in-one/releases)
2. PHPStorm > Settings > Plugins > gear icon > **Install Plugin from Disk**
3. Select the downloaded `.zip` and restart

### From Source
```bash
git clone https://github.com/TribusStudio/phpstorm-markdown-all-in-one.git
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
