# Markdown All-in-One for PHPStorm

A comprehensive Markdown editing plugin for JetBrains PHPStorm — inspired by the best of VSCode's Markdown experience, purpose-built for the IntelliJ Platform.

## Features

### Keyboard Shortcuts

Shortcuts automatically take priority over built-in IDE actions when editing markdown files. In non-markdown files, the standard IDE shortcuts work as normal.

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

### Selection Wrapping

Select text and type any of these characters to wrap the selection:

| Character | Result |
|-----------|--------|
| `*` | `*selection*` (italic) |
| `~` | `~selection~` (strikethrough) |
| `_` | `_selection_` (emphasis) |
| `` ` `` | `` `selection` `` (code) |
| `\|` | `\| selection \|` (table cell — smart: detects preceding pipe) |
| `-` | Fills with dashes inside table cells (header borders) |

### New File Templates

Right-click a directory > **New > Markdown File** to create from a template:

- **Blank** — empty file with a `# Title` heading
- **README** — standard open-source README skeleton
- **Document** — general document with author, date, sections
- **Meeting Notes** — date, attendees, agenda, action items
- **Changelog** — Keep a Changelog format
- **API Documentation** — endpoint docs with request/response tables

### Generate Menu (Cmd+N / Alt+Insert)

Press `Cmd+N` (macOS) or `Alt+Insert` (Windows/Linux) while editing a markdown file to access the **Markdown** generate submenu:

- **Table** — prompts for column count, inserts a GFM table skeleton
- **Code Block** — prompts for language, inserts a fenced code block
- **Image** — inserts `![alt](path)` (uses selected text as alt text)
- **Footnote** — inserts `[^n]` reference at cursor and definition at end of file
- **Link Reference** — inserts `[text][ref]` and appends `[ref]: url` definition
- **Front Matter** — inserts YAML front matter at the top of the document
- **Table of Contents** — generates TOC from existing headings

> **Note:** `Cmd+N` in the editor opens the **Generate** popup, not the **New File** dialog. This is standard PHPStorm behavior. To create a new file, use `Cmd+N` in the **Project panel** (click the project tree first), or right-click > New > Markdown File. If you prefer `Cmd+N` to always open the New File dialog, you can rebind it in **Settings > Keymap**: search for "Generate" and remove/change its shortcut, then assign `Cmd+N` to "New..." instead.

### Syntax Highlighting
- Headings, code blocks, inline code, blockquotes, list markers, horizontal rules
- Inline formatting: **bold**, *italic*, ~~strikethrough~~
- Links and images with distinct colors for text and URL
- Theme-aware — adapts to your IDE color scheme

### Smart List Editing
- **Smart Enter** — auto-continue unordered lists (`-`, `+`, `*`), ordered lists (auto-increments number), task lists (resets to `[ ]`), and blockquotes (`> `)
- **Empty list handling** — Enter on an empty list item outdents it (if indented) or removes the marker (if top-level)
- **Indent/Outdent** — `Cmd+]` / `Cmd+[` to indent/outdent list items (supports multi-line selection)
- **Smart Backspace** — outdents indented markers, removes top-level markers, strips task checkboxes

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
