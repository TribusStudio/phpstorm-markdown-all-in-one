# 005 — Marketplace Assets & Screenshot Guide

This document lists all screenshots and animated demos required for the JetBrains Marketplace listing and the README.

## Storage

All assets live in `docs/screenshots/` within the repository. They are referenced in the README and plugin description via raw GitHub URLs:

```
https://raw.githubusercontent.com/TribusStudio/phpstorm-markdown-all-in-one/main/docs/screenshots/split-editor.png
```

## Required Screenshots

These are static PNG screenshots captured from a running PHPStorm instance with the plugin installed.

| # | Filename | Description | What to show |
|---|----------|-------------|--------------|
| 1 | `split-editor.png` | Split editor with live preview | Editor on left with markdown source, preview on right rendering the same content. Show a document with headings, lists, code blocks, and an image so the preview demonstrates rich rendering. |
| 2 | `toolbar.png` | Editor toolbar | Close-up of the toolbar at the top of the editor, showing all button groups (formatting, headings, lists, table/TOC, tools menu, settings gear). |
| 3 | `floating-toolbar.png` | Floating selection toolbar | Text selected in the editor with the Notion-style popup toolbar visible above the selection. |
| 4 | `toc-generation.png` | Table of Contents | A document with a generated TOC showing proper indentation and anchor links. Ideally show the TOC markers (`<!-- TOC -->`) and the generated list. |
| 5 | `table-formatting.png` | Table auto-format | A before/after showing a messy table and the formatted result with aligned columns. Could be two screenshots or a split image. |
| 6 | `completion.png` | Auto-completion popup | The completion popup showing file paths or heading references while typing a link `[text](#`. |
| 7 | `settings.png` | Settings page | The full settings panel (Settings > Languages > Markdown All-in-One) showing all option groups. |
| 8 | `syntax-highlighting.png` | Syntax highlighting | Editor showing colored syntax for headings, bold, italic, links, code blocks, etc. Use a dark or light theme that shows the highlighting clearly. |
| 9 | `scroll-sync.png` | Scroll synchronization | Split view where both editor and preview are scrolled to the same section, demonstrating synchronized scrolling. |
| 10 | `context-menu.png` | Right-click context menu | The Markdown submenu visible in the right-click context menu. |

## Required Animated Demos

GIF animations demonstrating interactive workflows. Keep each under 10 seconds and 5MB.

| # | Filename | Description | What to show |
|---|----------|-------------|--------------|
| 1 | `demo-overview.gif` | Feature overview | Quick sequence: type some markdown, toggle bold/italic via shortcuts, create a list, watch it auto-continue on Enter, format a table, show the preview updating live. |
| 2 | `demo-smart-editing.gif` | Smart editing | Focus on: list continuation (Enter continues list), task toggle (Alt+C), selection wrapping (select text, type `*`), table Tab navigation. |

## Capture Instructions

### Tools
- **Screenshots:** PHPStorm's built-in screenshot tool, or macOS `Cmd+Shift+4` / Windows `Win+Shift+S`
- **GIFs:** [Kap](https://getkap.co/) (macOS), [ScreenToGif](https://www.screentogif.com/) (Windows), or [Peek](https://github.com/phw/peek) (Linux)

### Setup for Capture
1. Use a clean PHPStorm window with minimal distractions
2. Use the **New UI** (modern PHPStorm look)
3. Set font size to 14–16pt for readability
4. Use a light theme for screenshots (GitHub theme for preview) — dark theme variants are optional extras
5. Use the `test.md` file in the project root as a demo document, or create a purpose-built `docs/screenshots/demo.md`
6. Window size: 1280x800 or 1440x900 for full-window shots; crop toolbar/popup shots tightly
7. Retina/HiDPI preferred (2x resolution)

### Naming Convention
- Static screenshots: `kebab-case.png`
- Animated demos: `demo-description.gif`
- Dark variants (optional): `split-editor-dark.png`

## Plugin Description Images

The JetBrains Marketplace plugin description supports HTML. Screenshots can be embedded:

```html
<img src="https://raw.githubusercontent.com/TribusStudio/phpstorm-markdown-all-in-one/main/docs/screenshots/split-editor.png" alt="Split editor with live preview" width="800">
```

The marketplace listing supports up to 6 images in the "Screenshots" section (uploaded separately from the description). Prioritize:
1. `split-editor.png` (hero shot)
2. `demo-overview.gif` (animated feature demo)
3. `toolbar.png`
4. `table-formatting.png`
5. `settings.png`
6. `completion.png`
