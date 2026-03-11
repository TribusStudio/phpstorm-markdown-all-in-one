# Markdown All-in-One for PHPStorm

I really love what JetBrains has built for Markdown within PHPStorm. For the most part, its a really thorough implementation with a lot of room for others to hook into it. But something was bothering me with it. Various UX details that kept me from being satisfyed with it. I would routinely use VSCode instead for Markdown work because of those oddities.

Recently, with the advent of Claude Code and LLM development, I got an idea:

> *What if I tried to build my own plugin for PHPStorm?*

And so was born an alternative, comprehensive Markdown editing plugin for JetBrains PHPStorm — inspired by the best of VSCode's Markdown experience, purpose-built for the IntelliJ Platform.

> **Big shout** out and *thank you* to the creator, [Yu Zhang](https://marketplace.visualstudio.com/publishers/yzhang), of Markdown All in One [https://github.com/yzhang-gh/vscode-markdown](https://marketplace.visualstudio.com/items?itemName=yzhang.markdown-all-in-one). Great plugin which has lead to the of inspiration for this module -- *even the name*!

I built this to help resolve issues I felt I needed fixed to continue working with Markdown in PHPStorm the way I did it in VSCode and the way it felt intuitively right, for me. I am now sharing this with others to see how they feel about it and whether it scratches the itch others might have had with the default plugin.

Again, its something for me and it makes me happy. I hope it does the same for you!

## Features

<!-- TOC -->
- [Markdown All-in-One for PHPStorm](#markdown-all-in-one-for-phpstorm)
    - [Features](#features)
        - [Keyboard Shortcuts](#keyboard-shortcuts)
        - [Selection Wrapping](#selection-wrapping)
        - [New File Templates](#new-file-templates)
        - [Generate Menu (Cmd+N / Alt+Insert)](#generate-menu-cmdn--altinsert)
        - [Table of Contents](#table-of-contents)
        - [Table Formatting](#table-formatting)
        - [Live Preview](#live-preview)
        - [Math & LaTeX](#math--latex)
        - [Toolbars & Context Menu](#toolbars--context-menu)
        - [Syntax Highlighting](#syntax-highlighting)
        - [Auto-Completion](#auto-completion)
        - [Smart Paste](#smart-paste)
        - [HTML Export](#html-export)
        - [Smart List Editing](#smart-list-editing)
        - [Settings](#settings)
    - [Installation](#installation)
        - [Auto-Update (Recommended)](#auto-update-recommended)
        - [From GitHub Release](#from-github-release)
        - [From Source](#from-source)
    - [Development](#development)
        - [Prerequisites](#prerequisites)
        - [Quick Start](#quick-start)
        - [All Commands](#all-commands)
    - [Contributing](#contributing)
    - [License](#license)
<!-- /TOC -->


### Keyboard Shortcuts

Shortcuts automatically take priority over built-in IDE actions when editing markdown files. In non-markdown files, the standard IDE shortcuts work as normal.

| Shortcut                  | Action                            |
| ------------------------- | --------------------------------- |
| `Cmd/Ctrl+B`              | Toggle **bold**                   |
| `Cmd/Ctrl+I`              | Toggle *italic*                   |
| `Alt+S`                   | Toggle ~~strikethrough~~          |
| `Cmd/Ctrl+`               | Toggle `code span`                |
| `Cmd/Ctrl+Shift+` `` ` `` | Toggle code block                 |
| `Ctrl+Shift+]`            | Increase heading level            |
| `Ctrl+Shift+[`            | Decrease heading level            |
| `Cmd/Ctrl+]`              | Indent list item                  |
| `Cmd/Ctrl+[`              | Outdent list item                 |
| `Alt+C`                   | Toggle task list checkbox         |
| `Ctrl+M`                  | Toggle math (`$...$` / `$$...$$`) |

### Selection Wrapping

Select text and type any of these characters to wrap the selection:

| Character | Result                                                         |
| --------- | -------------------------------------------------------------- |
| `*`       | `*selection*` (italic)                                         |
| `~`       | `~selection~` (strikethrough)                                  |
| `_`       | `_selection_` (emphasis)                                       |
| `` ` ``   | `` `selection` `` (code)                                       |
| `\|`      | `\| selection \|` (table cell — smart: detects preceding pipe) |
| `-`       | Fills with dashes inside table cells (header borders)          |

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

### Table of Contents

Generate and maintain a Table of Contents from your document's headings.

**Create TOC:** Use `Markdown > Create Table of Contents` from the Tools menu, or `Cmd+N` > Markdown > Table of Contents in the editor. The TOC is inserted at the cursor wrapped in `<!-- TOC -->` / `<!-- /TOC -->` markers.

**Update TOC:** Use `Markdown > Update Table of Contents`, or simply save the file — the TOC auto-updates on save (configurable in Settings).

**Configuration** (Settings > Languages > Markdown All-in-One):

| Setting | Default | Description |
| ------- | ------- | ----------- |
| Heading levels | `1..6` | Range of heading levels to include (e.g., `2..4`) |
| Ordered list | `false` | Use numbered list instead of bullets |
| List marker | `-` | Unordered list marker (`-`, `*`, `+`) |
| Slug mode | `github` | Anchor generation: GitHub, GitLab, Gitea, Azure DevOps, Bitbucket Cloud |
| Update on save | `true` | Auto-update TOC when saving |

**Omit headings** from the TOC using comment markers:

```markdown
## Visible Heading

<!-- omit from toc -->
## Hidden Heading

## Also Hidden <!-- omit in toc -->
```

**Omit entire sections** using range markers:

```markdown
<!-- omit from toc start -->
## Hidden Section 1
## Hidden Section 2
<!-- omit from toc end -->
```

**Multiple TOCs:** You can have multiple TOCs in one document, each with independent settings:

```markdown
<!-- TOC -->
- [Markdown All-in-One for PHPStorm](#markdown-all-in-one-for-phpstorm)
    - [Features](#features)
        - [Keyboard Shortcuts](#keyboard-shortcuts)
        - [Selection Wrapping](#selection-wrapping)
        - [New File Templates](#new-file-templates)
        - [Generate Menu (Cmd+N / Alt+Insert)](#generate-menu-cmdn--altinsert)
        - [Table of Contents](#table-of-contents)
        - [Table Formatting](#table-formatting)
        - [Live Preview](#live-preview)
        - [Math & LaTeX](#math--latex)
        - [Toolbars & Context Menu](#toolbars--context-menu)
        - [Syntax Highlighting](#syntax-highlighting)
        - [Auto-Completion](#auto-completion)
        - [Smart Paste](#smart-paste)
        - [HTML Export](#html-export)
        - [Smart List Editing](#smart-list-editing)
        - [Settings](#settings)
    - [Installation](#installation)
        - [Auto-Update (Recommended)](#auto-update-recommended)
        - [From GitHub Release](#from-github-release)
        - [From Source](#from-source)
    - [Development](#development)
        - [Prerequisites](#prerequisites)
        - [Quick Start](#quick-start)
        - [All Commands](#all-commands)
    - [Contributing](#contributing)
    - [License](#license)
<!-- /TOC -->

<!-- TOC name="api" type="ordered" level="2..4" -->
<!-- /TOC -->
```

Supported attributes: `name` (unique identifier), `type` (`bullet` or `ordered`), `level` (heading range).

**TOC content ranges:** Scope a named TOC to specific sections of the document:

```markdown
<!-- toc range name="api" start -->
## Endpoints
### GET /users
### POST /users
<!-- toc range end -->
```

A named TOC only includes headings from its matching range. An unnamed `<!-- TOC -->` always includes all document headings.

**Section numbering:** Use `Markdown > Add Section Numbers` / `Remove Section Numbers` to add or strip hierarchical numbering (e.g., `1.1.`, `1.2.`) from headings.

### Table Formatting

Auto-format GFM tables with consistent padding and alignment.

**Format Table:** Use `Markdown > Format Table` (`Ctrl+Shift+Alt+T`) to format the table at the cursor. Use `Markdown > Format All Tables` to format every table in the document.

**Format on save:** Tables are automatically formatted when saving (configurable in Settings > Languages > Markdown All-in-One > Table formatter enabled).

**Tab navigation:** Press `Tab` inside a table to jump to the next cell. `Shift+Tab` jumps to the previous cell. Navigation wraps between rows and skips the separator row.

**Alignment preservation:** Column alignment markers are detected and preserved during formatting:

```markdown
| Left | Center | Right |
| :--- | :----: | ----: |
| text |  text  |  text |
```

### Live Preview

JCEF-based live preview with split editor support — just like the built-in Markdown plugin, but with theming and customization.

**Split editor modes:** Click the editor/split/preview toggle in the top-right corner:
- **Editor** — text editor only
- **Split** — editor and preview side by side
- **Preview** — preview only

**Scroll sync:** In split mode, scrolling the editor automatically scrolls the preview to the matching position, and vice versa. Uses source-line mapping for accurate synchronization. Configurable via Settings > Preview > "Synchronize editor and preview scroll position".

**CSS themes** (Settings > Languages > Markdown All-in-One > Preview):

| Theme             | Description                                              |
| ----------------- | -------------------------------------------------------- |
| Auto (follow IDE) | GitHub Light in light themes, GitHub Dark in dark themes |
| GitHub            | GitHub's light markdown rendering                        |
| GitHub Dark       | GitHub's dark markdown rendering                         |
| GitLab            | GitLab's markdown style                                  |
| VSCode            | VSCode's markdown preview style                          |

**Custom CSS:** Set a path to a `.css` file in settings to apply additional style overrides on top of the selected theme.

### Math & LaTeX

Write math expressions using standard LaTeX syntax. They render in the preview using the bundled KaTeX library.

- **Inline math:** `$E = mc^2$` renders inline
- **Display math:** `$$\sum_{i=1}^n x_i$$` renders as a centered block
- **Toggle shortcut:** `Ctrl+M` cycles: plain text → `$inline$` → `$$display$$` → plain text
- **Auto-completion:** Type `\` inside a math environment to get LaTeX command completion (170+ commands including Greek letters, operators, relations, arrows, symbols, and environments)
- **Syntax highlighting:** Math delimiters (`$`, `$$`) and content are highlighted with distinct colors in the editor
- **Configurable:** Enable/disable via Settings > Math > "Enable math rendering in preview"

> **Note:** KaTeX fonts are loaded from a CDN (jsDelivr) for the preview. Internet access is needed for math to render correctly in the preview panel.

**Zoom:** The preview panel supports zoom in/out/reset for comfortable reading.

### Toolbars & Context Menu

**Floating toolbar:** Select text to see a Notion-style popup toolbar with quick formatting buttons (Bold, Italic, Strikethrough, Code, Heading Up/Down, Task Toggle).

**Editor toolbar:** A persistent toolbar at the top of markdown editors provides one-click access to formatting (B, I, S, Code, H+, H-), power tools (Table, TOC), and Settings.

**Right-click context menu:** Right-click in a markdown file to see the **Markdown** submenu with context-aware actions:
- Formatting actions appear when text is selected
- Table formatting appears when the cursor is inside a table
- TOC update appears when a TOC block exists in the document

### Syntax Highlighting
- Headings, code blocks, inline code, blockquotes, list markers, horizontal rules
- Inline formatting: **bold**, *italic*, ~~strikethrough~~
- Links and images with distinct colors for text and URL
- Theme-aware — adapts to your IDE color scheme

### Auto-Completion

Context-aware completions while editing markdown:

- **Heading references** — type `[text](#` to see all document headings with their anchor slugs
- **Reference link labels** — type `[text][` to see all `[label]: url` definitions in the document
- **File/image paths** — type `[text](` or `![alt](` to browse files relative to the current document; supports directory navigation

1. Type `[link text](` — after the `(`, start typing a path and press `Ctrl+Space` to trigger completion
2. Type `![alt text](` — same for images

The completion provider checks if the text before the cursor matches the pattern `[...](` or `![...](`, then lists files relative to the current document's directory.

- `[text](#` + `Ctrl+Space` → heading slugs
- `[text][` + `Ctrl+Space` → reference labels
- `[text](` + `Ctrl+Space` → file paths
- `![alt](` + `Ctrl+Space` → file paths (images)

### Smart Paste

Paste a URL while text is selected to automatically create a markdown link:

- **Regular URL** — `[selected text](https://example.com)`
- **Image URL** (`.png`, `.jpg`, `.gif`, `.svg`, etc.) — `![selected text](https://example.com/photo.png)`

Configurable via Settings > Languages > Markdown All-in-One > Smart paste enabled.

### HTML Export

Export markdown files to styled, standalone HTML documents.

**Export current file:** Use `Markdown > Export to HTML` from the Tools menu or the toolbar's tools popup (`...`). A file save dialog lets you choose the output location.

**Batch export:** Use `Markdown > Batch Export to HTML` to export all markdown files in a folder (with recursive subdirectory support) to a chosen output directory.

**Features:**

| Feature | Description |
| ------- | ----------- |
| Styled output | Exported HTML includes the selected preview theme CSS |
| Custom CSS | Any custom CSS overrides from settings are applied |
| Image path resolution | Relative image paths are resolved to absolute `file://` paths |
| Base64 image embed | Optionally embed local images as base64 for self-contained HTML |
| Link validation | Warns about broken anchor links, missing file references, undefined labels |
| Document title | HTML `<title>` extracted from first heading, falls back to filename |

### Smart List Editing
- **Smart Enter** — auto-continue unordered lists (`-`, `+`, `*`), ordered lists (auto-increments number), task lists (resets to `[ ]`), and blockquotes (`> `)
- **Empty list handling** — Enter on an empty list item outdents it (if indented) or removes the marker (if top-level)
- **Indent/Outdent** — `Cmd+]` / `Cmd+[` to indent/outdent list items (supports multi-line selection)
- **Smart Backspace** — outdents indented markers, removes top-level markers, strips task checkboxes

### Settings

All options are in **Settings > Languages > Markdown All-in-One**:

| Group             | Setting                             | Default           |
| ----------------- | ----------------------------------- | ----------------- |
| Formatting        | Bold indicator (`**` / `__`)        | `**`              |
| Formatting        | Italic indicator (`*` / `_`)        | `*`               |
| List Editing      | Auto-renumber ordered lists         | On                |
| List Editing      | Indentation size (adaptive/inherit) | adaptive          |
| Table of Contents | Heading levels                      | `1..6`            |
| Table of Contents | Update on save                      | On                |
| Table of Contents | Ordered list                        | Off               |
| Table of Contents | Unordered list marker (`-`/`*`/`+`) | `-`               |
| Table of Contents | Slug generation mode                | github            |
| Table Formatting  | Enable auto-formatting              | On                |
| Table Formatting  | Format on save                      | On                |
| Preview           | Render theme                        | Auto (follow IDE) |
| Preview           | Custom CSS path                     | (empty)           |
| Preview           | Scroll sync                         | On                |
| Toolbar           | Show editor toolbar                 | On                |
| Completion        | Auto-popup completion               | On                |
| Smart Paste       | Auto-create links from URLs         | On                |
| Math              | Enable math rendering in preview    | On                |
| Export            | Embed images as base64              | Off               |
| Export            | Validate links on export            | On                |

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

## Contributing

Contributions are welcome. Please see [CONTRIBUTING.md](CONTRIBUTING.md) for development setup, build commands, and guidelines.

## License

MIT License. See [LICENSE](LICENSE) for details.
