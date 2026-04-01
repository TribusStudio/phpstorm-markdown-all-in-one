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
        - [Toolbars & Menus](#toolbars--menus)
        - [Text Formatting & Selection Wrapping](#text-formatting--selection-wrapping)
        - [Links & Images](#links--images)
        - [Smart List Editing](#smart-list-editing)
        - [Table Formatting & Operations](#table-formatting--operations)
        - [Table of Contents](#table-of-contents)
        - [Live Preview](#live-preview)
        - [Syntax Highlighting & Editor Decorations](#syntax-highlighting--editor-decorations)
        - [Structure View, Folding & Navigation](#structure-view-folding--navigation)
        - [Auto-Completion](#auto-completion)
        - [Math & LaTeX](#math--latex)
        - [HTML Export](#html-export)
        - [New File Templates & Generate Menu](#new-file-templates--generate-menu)
        - [Settings](#settings)
    - [Installation](#installation)
        - [Auto-Update (Recommended)](#auto-update-recommended)
        - [From GitHub Release](#from-github-release)
        - [From Source](#from-source)
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
| `Cmd/Ctrl+`` ` ``         | Toggle `code span`                |
| `Cmd/Ctrl+Shift+`` ` ``   | Toggle code block                 |
| `Ctrl+K`                  | Insert link (dialog)              |
| `Ctrl+Shift+K`            | Insert image (dialog)             |
| `Ctrl+Shift+]`            | Increase heading level            |
| `Ctrl+Shift+[`            | Decrease heading level            |
| `Ctrl+Shift+.`            | Toggle blockquote                 |
| `Cmd/Ctrl+]`              | Indent list item                  |
| `Cmd/Ctrl+[`              | Outdent list item                 |
| `Alt+C`                   | Toggle task list checkbox         |
| `Alt+Up/Down`             | Move line up/down                 |
| `Shift+Alt+Up/Down`       | Copy line up/down                 |
| `Ctrl+Enter`              | Exit list continuation            |
| `Shift+Enter`             | Soft break (`<br>`)               |
| `Ctrl+M`                  | Toggle math (`$...$` / `$$...$$`) |
| `Ctrl+Shift+Alt+T`        | Format table at cursor            |
| `Tab` / `Shift+Tab`       | Table cell navigation or list indent/outdent |

### Toolbars & Menus

**Editor toolbar:** A persistent toolbar at the top of the editor pane provides one-click access to formatting (Bold, Italic, Strikethrough, Code, Code Block), links and images, headings, blockquote, list toggle, indent/outdent, task toggle, math, table format, TOC update, plus a tools popup menu and settings gear.

**Floating toolbar:** Select text to see a popup toolbar above the selection with quick formatting buttons (Bold, Italic, Strikethrough, Code, Heading Up/Down, Insert Link, Insert Image). Uses the platform's ActionToolbar for consistent IDE styling.

**Right-click context menu:** Right-click in a markdown file to see the **Markdown** submenu with context-aware actions:
- Link and image insertion (always available)
- Formatting actions when text is selected
- Blockquote toggle and list operations
- Table row/column operations when cursor is in a table
- TOC creation and update

**Tools > Markdown menu:** All actions are also accessible from the main menu under Tools > Markdown, including export, section numbering, and table operations.

**Generate menu** (`Cmd+N` / `Alt+Insert`): Quick insertion of tables, code blocks, images, footnotes, link references, front matter, and TOC.

### Text Formatting & Selection Wrapping

Toggle formatting on the current selection or word at cursor:
- **Bold** (`Cmd/Ctrl+B`), **Italic** (`Cmd/Ctrl+I`), **Strikethrough** (`Alt+S`), **Code Span** (`` Cmd/Ctrl+` ``), **Code Block** (`` Cmd/Ctrl+Shift+` ``)
- **Heading level** — increase (`Ctrl+Shift+]`) or decrease (`Ctrl+Shift+[`)
- **Blockquote** (`Ctrl+Shift+.`) — toggles `> ` prefix on selected lines

**Selection wrapping:** Select text and type a character to wrap it:

| Character | Result                                                         |
| --------- | -------------------------------------------------------------- |
| `*`       | `*selection*` (italic)                                         |
| `~`       | `~selection~` (strikethrough)                                  |
| `_`       | `_selection_` (emphasis)                                       |
| `` ` ``   | `` `selection` `` (code)                                       |
| `\|`      | `\| selection \|` (table cell — smart: detects preceding pipe) |
| `-`       | Fills with dashes inside table cells (header borders)          |

### Links & Images

**Insert Link** (`Ctrl+K`) — opens a dialog with text, URL, and optional title fields. Pre-fills selected text as the link text. Inserts `[text](url)` or `[text](url "title")`.

**Insert Image** (`Ctrl+Shift+K`) — opens a dialog with alt text, image path/URL, and optional title fields. Pre-fills selected text as alt text. Inserts `![alt](src)` or `![alt](src "title")`.

**Smart paste** — paste a URL while text is selected to automatically create a markdown link. Image URLs (`.png`, `.jpg`, `.gif`, `.svg`, etc.) become `![text](url)`.

**Preview link navigation** — clicking a relative link (e.g., `other-doc.md`) in the preview opens the target file in PHPStorm's editor with its own preview. External URLs open in the system browser. Anchor links (`#heading`) scroll within the preview.

**Auto-completion** — type `[text](` to get file path completion, `[text](#` for heading anchor completion, `[text][` for reference label completion.

### Smart List Editing

- **Smart Enter** — auto-continue unordered lists (`-`, `+`, `*`), ordered lists (auto-increments number), task lists (resets to `[ ]`), and blockquotes (`> `)
- **Empty list handling** — Enter on an empty list item outdents it (if indented) or removes the marker (if top-level)
- **Indent/Outdent** — `Cmd+]` / `Cmd+[` to indent/outdent list items (supports multi-line selection)
- **Tab/Shift+Tab** — context-aware: indents/outdents list items when on a list line, navigates table cells when in a table
- **Toggle List** — cycle the current line through marker candidates (`-`, `*`, `+`, `1.`, `1)`) via toolbar or Tools menu
- **Move line** — `Alt+Up/Down` moves the current line up or down, auto-renumbering ordered lists
- **Copy line** — `Shift+Alt+Up/Down` duplicates the line, auto-renumbering ordered lists
- **Ctrl+Enter** — exit list continuation (insert a plain newline without a marker)
- **Shift+Enter** — soft break within a list item (trailing double-space + newline for `<br>`)
- **Ordered list style** — configurable: "ordered" (1. 2. 3.) or "one" (always 1.)
- **Auto-renumber** — ordered lists are renumbered after indent/outdent and move/copy operations
- **Smart Backspace** — outdents indented markers, removes top-level markers, strips task checkboxes

### Table Formatting & Operations

**Format Table** (`Ctrl+Shift+Alt+T`) — auto-format the GFM table at the cursor with consistent padding and alignment. **Format All Tables** formats every table in the document. **Format on save** runs automatically when saving.

**Tab navigation** — press `Tab` inside a table to jump to the next cell, `Shift+Tab` for the previous cell. Navigation wraps between rows and skips the separator row.

**Alignment** — column alignment markers (`:---`, `:---:`, `---:`) are detected and preserved during formatting. Set or change alignment via the right-click context menu.

**Table operations** (right-click context menu or Markdown > Table submenu):
- **Insert Row** above/below cursor
- **Insert Column** before/after cursor
- **Delete** current row or column
- **Move Row** up/down, **Move Column** left/right
- **Set Alignment** — left, center, right, or none for the current column

### Table of Contents

Generate and maintain a Table of Contents from your document's headings.

**Create TOC:** Use `Markdown > Create Table of Contents` from the Tools menu, or `Cmd+N` > Markdown > Table of Contents in the editor. The TOC is inserted at the cursor wrapped in `<!-- TOC -->` / `<!-- /TOC -->` markers.

**Update TOC:** Use `Markdown > Update Table of Contents`, or simply save the file — the TOC auto-updates on save.

**Configuration** (Settings > Languages > Markdown All-in-One):

| Setting | Default | Description |
| ------- | ------- | ----------- |
| Heading levels | `1..6` | Range of heading levels to include (e.g., `2..4`) |
| Ordered list | `false` | Use numbered list instead of bullets |
| List marker | `-` | Unordered list marker (`-`, `*`, `+`) |
| Slug mode | `github` | Anchor generation: GitHub, GitLab, Gitea, Azure DevOps, Bitbucket Cloud, Zola |
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

**Multiple TOCs** with independent settings, **named TOCs** scoped to content ranges, and **section numbering** (`Add/Remove Section Numbers`) are all supported. See the example in the [Table of Contents documentation](docs/003-feature-spec-toc.md).

### Live Preview

JCEF-based live preview with split editor support.

**Split editor modes:** Click the editor/split/preview toggle in the top-right corner:
- **Editor** — text editor only
- **Split** — editor and preview side by side (default for new files)
- **Preview** — preview only

**Scroll sync:** Bidirectional scroll synchronization between editor and preview using source-line mapping with interpolation for accuracy.

**CSS themes** (Settings > Preview):

| Theme             | Description                                              |
| ----------------- | -------------------------------------------------------- |
| Auto (follow IDE) | GitHub Light in light themes, GitHub Dark in dark themes |
| GitHub            | GitHub's light markdown rendering                        |
| GitHub Dark       | GitHub's dark markdown rendering                         |
| GitLab            | GitLab's markdown style                                  |
| VSCode            | VSCode's markdown preview style                          |

**Custom CSS:** Set a path to a `.css` file in settings to apply additional style overrides.

**Preview zoom:** Zoom in/out/reset for comfortable reading.

**Blockquotes with code blocks:** Fenced code blocks, headings, lists, and other block-level elements inside blockquotes render correctly in the preview.

### Syntax Highlighting & Editor Decorations

**Two-layer syntax highlighting:**
- **Block-level** — headings, code blocks, inline code, blockquotes, list markers, horizontal rules
- **Inline** — **bold**, *italic*, ~~strikethrough~~, [links](url), ![images](url), $math$
- Theme-aware — adapts to your IDE color scheme

**Editor decorations** (individually toggleable in Settings):
- **Code span background** — subtle background tint on inline `` `code` `` (theme-aware)
- **Strikethrough rendering** — actual line-through text effect on `~~text~~`
- **Formatting marker dimming** — `**`, `~~`, `*`, `_` markers in muted color
- **Trailing space indicator** — trailing whitespace highlighted with a soft background
- **Hard line break indicator** — trailing double-space highlighted with a blue-tinted background
- **File size limit** — decorations skipped on large files (configurable, default 500K chars)

**Color customization:** All markdown colors are customizable in **Settings > Editor > Color Scheme > Markdown**.

### Structure View, Folding & Navigation

**Structure View** (`Cmd+7` / `Alt+7`) — hierarchical heading outline in the Structure tool window. Click a heading to navigate to it in the editor.

**Go To Symbol** (`Ctrl+Shift+Alt+N`) — search headings across all markdown files in the project.

**Code Folding** — collapsible regions for heading sections, fenced code blocks (with language in placeholder), YAML front matter, and multi-line blockquotes.

**Breadcrumbs** — the editor breadcrumb bar shows the heading hierarchy path at the current cursor position.

### Auto-Completion

Context-aware completions while editing:

| Trigger | Completion |
| ------- | ---------- |
| `[text](#` + `Ctrl+Space` | Heading anchor slugs |
| `[text][` + `Ctrl+Space` | Reference link labels |
| `[text](` + `Ctrl+Space` | File/image paths (relative to current file) |
| `![alt](` + `Ctrl+Space` | File/image paths |
| `\` inside `$...$` | LaTeX commands (170+ symbols, environments) |

### Math & LaTeX

Write math expressions using standard LaTeX syntax, rendered in the preview using the bundled KaTeX library.

- **Inline math:** `$E = mc^2$` renders inline
- **Display math:** `$$\sum_{i=1}^n x_i$$` renders as a centered block
- **Toggle shortcut:** `Ctrl+M` cycles: plain text → `$inline$` → `$$display$$` → plain text
- **Auto-completion:** Type `\` inside a math environment for LaTeX command completion
- **Syntax highlighting:** Math delimiters and content highlighted with distinct colors

### HTML Export

Export markdown files to styled, standalone HTML documents.

**Export current file:** `Markdown > Export to HTML` or the toolbar's tools popup. **Batch export:** `Markdown > Batch Export to HTML` for entire folders.

| Feature | Description |
| ------- | ----------- |
| Styled output | Exported HTML includes the selected preview theme CSS |
| Custom CSS | Custom CSS overrides from settings are applied |
| Image resolution | Relative image paths resolved to absolute `file://` paths |
| Base64 embed | Optionally embed local images as base64 for self-contained HTML |
| Link validation | Warns about broken anchors, file links, and undefined references |
| Document title | From `<!-- title: ... -->` comment, first heading, or filename |
| Auto-export | Automatically generate `.html` alongside `.md` on save |
| Link conversion | Internal `.md` links rewritten to `.html` in exported output |
| Pure HTML mode | Export without any CSS stylesheets |

### New File Templates & Generate Menu

**New > Markdown File** — right-click a directory to create from a template:
- Blank, README, Document, Meeting Notes, Changelog, API Documentation

**Generate menu** (`Cmd+N` / `Alt+Insert` in editor):
- Table, Code Block, Image, Footnote, Link Reference, Front Matter, Table of Contents

### Settings

All options are in **Settings > Languages > Markdown All-in-One**:

| Group             | Setting                             | Default           |
| ----------------- | ----------------------------------- | ----------------- |
| Formatting        | Bold indicator (`**` / `__`)        | `**`              |
| Formatting        | Italic indicator (`*` / `_`)        | `*`               |
| List Editing      | Auto-renumber ordered lists         | On                |
| List Editing      | Indentation size (adaptive/inherit) | adaptive          |
| List Editing      | Ordered list marker style           | ordered           |
| List Editing      | Toggle list candidates              | `-, *, +, 1., 1)` |
| Table of Contents | Heading levels                      | `1..6`            |
| Table of Contents | Update on save                      | On                |
| Table of Contents | Ordered list                        | Off               |
| Table of Contents | Unordered list marker               | `-`               |
| Table of Contents | Slug generation mode                | github            |
| Table Formatting  | Enable auto-formatting              | On                |
| Table Formatting  | Format on save                      | On                |
| Preview           | Render theme                        | Auto (follow IDE) |
| Preview           | Custom CSS path                     | (empty)           |
| Preview           | Scroll sync                         | On                |
| Preview           | Auto-show preview on open           | On                |
| Toolbar           | Show editor toolbar                 | On                |
| Completion        | Auto-popup completion               | On                |
| Smart Paste       | Auto-create links from URLs         | On                |
| Math              | Enable math rendering in preview    | On                |
| Decorations       | Code span background                | On                |
| Decorations       | Strikethrough rendering             | On                |
| Decorations       | Formatting marker dimming           | On                |
| Decorations       | Trailing space indicator            | On                |
| Decorations       | Hard line break indicator           | On                |
| Decorations       | File size limit (chars)             | 500000            |
| Export            | Embed images as base64              | Off               |
| Export            | Validate links on export            | On                |
| Export            | Auto-export HTML on save            | Off               |
| Export            | Convert .md links to .html          | On                |
| Export            | Pure HTML export (no CSS)           | Off               |

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
