# VSCode Markdown All-in-One — Feature Gap Analysis

**Date:** 2026-03-10
**Reference:** https://github.com/yzhang-gh/vscode-markdown
**Purpose:** Identify features in VSCode's extension that we don't yet have, and plan their implementation.

## Feature Parity Summary

| Category | VSCode | Ours | Gap |
|----------|--------|------|-----|
| Text formatting (bold, italic, strike, code) | ✅ | ✅ | None |
| Heading up/down | ✅ | ✅ | None |
| Task list toggle | ✅ | ✅ | None |
| Selection wrapping | ❌ | ✅ | We're ahead |
| Smart Enter (lists, blockquotes) | ✅ | ✅ | None |
| Smart Backspace | ✅ | ✅ | None |
| List indent/outdent | ✅ | ✅ | None |
| Toggle List (cycle markers) | ✅ | ❌ | **Gap** |
| Move/copy line with auto-renumber | ✅ | ❌ | **Gap** |
| Ctrl+Enter to exit list | ✅ | ❌ | **Gap** |
| Shift+Enter soft break in list | ✅ | ❌ | **Gap** |
| Tab/Shift+Tab in list context | ✅ | ❌ | **Gap** (we use Ctrl+]/[) |
| Ordered list marker style (ordered vs one) | ✅ | ❌ | **Gap** |
| Adaptive list indentation | ✅ | ❌ | **Gap** |
| Math/LaTeX toggle | ✅ | ❌ | **Major gap** |
| KaTeX preview rendering | ✅ | ❌ | **Major gap** |
| Math completions (LaTeX commands) | ✅ | ❌ | **Major gap** |
| TOC (create, update, levels, slugs, omit) | ✅ | ✅ | None |
| Named TOCs, content ranges, multiple TOCs | ❌ | ✅ | We're ahead |
| Bitbucket Cloud slugs | ❌ | ✅ | We're ahead |
| Zola slugs | ✅ | ❌ | Minor gap |
| Table formatting | ✅ | ✅ | None |
| Table Tab navigation | ✅ | ✅ | None |
| Smart table width limiting | ❌ | ✅ | We're ahead |
| Escaped pipe handling | ✅ | ✅ | None |
| Completion (paths, headings, refs) | ✅ | ✅ | None |
| Smart paste | ✅ | ✅ | None |
| Code span border decoration | ✅ | ❌ | **Gap** |
| Strikethrough line-through in editor | ✅ | ❌ | **Gap** |
| Formatting mark dimming | ✅ | ❌ | **Gap** |
| Trailing space shading | ✅ | ❌ | **Gap** |
| JCEF preview with themes | ✅ | ✅ | None |
| Custom CSS | ✅ | ✅ | None |
| Preview zoom | ❌ | ✅ | We're ahead |
| Scroll sync | ✅ (via VSCode API) | ✅ | None |
| Auto-show preview on file open | ✅ | ❌ | **Gap** |
| Export to HTML (single + batch) | ✅ | ✅ | None |
| Base64 image embedding | ✅ | ✅ | None |
| Link validation on export | ✅ | ✅ | None |
| Auto-export on save | ✅ | ❌ | **Gap** |
| `<!-- title -->` in HTML export | ✅ | ❌ | **Gap** |
| `.md` → `.html` link conversion | ✅ | ❌ | **Gap** |
| Pure HTML export (no styles) | ✅ | ❌ | **Gap** |
| Toolbar (persistent editor bar) | ✅ | ✅ | None |
| Floating selection toolbar | ❌ | ✅ | We're ahead |
| Context menu | ❌ | ✅ | We're ahead |
| Generate menu | ❌ | ✅ | We're ahead |
| File templates | ❌ | ✅ | We're ahead |
| Mermaid / diagram rendering | ❌ | ❌ | Both missing |

## Detailed Gap Breakdown

### 1. Math/LaTeX Support (Major — Phase 10)

VSCode provides full math editing and rendering:

- **Toggle Math** (`Ctrl+M`): Cycles cursor/selection through no-math → `$...$` (inline) → `$$...$$` (display)
- **Toggle Math Reverse**: Cycles in opposite direction
- **KaTeX rendering**: Math blocks render as formatted equations in the preview pane
- **Math completions**: LaTeX command autocompletion inside math environments (e.g., `\frac`, `\sum`, `\alpha`)
- **Settings**: `math.enabled` (boolean), `katex.macros` (custom macro definitions)
- **Syntax injection**: Math-specific grammar for highlighting inside `$` delimiters

**Implementation approach:**
- Add `ToggleMathAction` with `Ctrl+M` keybinding
- Embed KaTeX JS library in JCEF preview (CDN fallback or bundled)
- Detect `$...$` and `$$...$$` in `MarkdownHtmlConverter` and wrap in KaTeX-compatible markup
- Add `MathCompletionProvider` with common LaTeX commands
- Add lexer tokens for math delimiters
- Settings: `mathEnabled`, `katexMacros`

### 2. Advanced List Editing (Moderate — Phase 11)

Several list editing conveniences we're missing:

- **Toggle List**: Command that cycles the current line through marker candidates (`-`, `*`, `+`, `1.`, `1)`) — configurable via `list.toggle.candidate-markers`
- **Move line up/down** (`Alt+Up/Down`): Moves lines and auto-renumbers ordered lists
- **Copy line up/down** (`Shift+Alt+Up/Down`): Duplicates lines and auto-renumbers
- **Ctrl+Enter**: Exit list continuation (insert plain line instead of list item)
- **Shift+Enter**: Insert soft break (`<br>`) within list item without creating new item
- **Tab/Shift+Tab in list context**: Indent/outdent list items with Tab key (currently only works in tables for us)
- **Ordered list marker style**: Setting to choose between incrementing (`1. 2. 3.`) or always-one (`1. 1. 1.`)
- **Adaptive indentation**: Align sublists with parent content rather than using fixed tab size
- **Auto-renumber on indent/outdent**: Already tracked as incomplete in Phase 2

**Implementation approach:**
- `ToggleListAction` with marker rotation logic
- Override `MoveLineUp/Down` and `CopyLineUp/Down` editor actions for markdown context
- Extend `MarkdownEnterHandler` with Ctrl+Enter and Shift+Enter variants
- Extend `TableTabHandler` to also handle list context (when cursor is in list, not table)
- Add `orderedListMarker` and `listIndentationSize` settings

### 3. Editor Decorations & Visual Theming (Moderate — Phase 12)

VSCode renders visual decorations in the editor itself (not just in preview):

- **Code span background/border**: Inline `code` gets a visible background/border in the editor
- **Strikethrough rendering**: `~~text~~` shown with line-through decoration in editor
- **Formatting mark dimming**: `**`, `~~`, `` ` `` markers shown in muted/faded color
- **Trailing space shading**: Trailing whitespace highlighted with background color
- **Hard line break rendering**: Visual indicator for trailing double-space line breaks
- **Link rendering**: Visual distinction for link text vs URL
- Settings for each decoration type (enable/disable individually)
- Custom color tokens for theme integration

**Implementation approach:**
- Extend `MarkdownAnnotator` with `TextAttributes` that include background colors, strikethrough effects, and foreground dimming
- Use `EditorCustomElementRenderer` for more complex decorations if needed
- Add per-decoration toggle settings
- Define color scheme keys in plugin descriptor for theme integration
- `decorationFileSizeLimit` setting to skip large files

### 4. Export & Preview Polish (Small — Phase 13)

Minor export and preview features:

- **Auto-export on save**: `exportOnSave` setting — automatically export HTML when saving `.md` file
- **HTML title from comment**: `<!-- title: Your Title -->` sets the `<title>` in exported HTML
- **`.md` → `.html` link conversion**: Internal `.md` links rewritten to `.html` in export output
- **Pure HTML export**: Export without any CSS — just raw HTML content
- **Auto-show preview**: `autoShowPreview` setting — automatically open split preview when opening a `.md` file
- **Zola slug mode**: Additional slugify mode for Zola static site generator

**Implementation approach:**
- Add `FileDocumentManagerListener` for export-on-save
- Parse `<!-- title: ... -->` comments in `HtmlExporter`
- Post-process links in export to rewrite `.md` extensions
- Add `pureHtml` export option
- Add `autoShowPreview` setting with `FileEditorManagerListener`
- Add `Slugify.Mode.ZOLA`

### 5. Diagram Rendering — Mermaid & Beyond (Stretch — Phase 14)

Neither extension has this natively, but it's a natural next step:

- **Mermaid diagrams**: Render ```` ```mermaid ```` code blocks as diagrams in preview
- **PlantUML**: Render ```` ```plantuml ```` blocks
- Embed rendering libraries (Mermaid.js) in JCEF preview
- Detect diagram language tags in fenced code blocks
- Fall back to showing raw code if rendering fails

## Features Where We're Ahead

These features exist in our plugin but NOT in VSCode's extension:

1. **Selection wrapping** — type formatting characters with text selected
2. **Floating selection toolbar** — Notion-style popup on selection
3. **Right-click context menu** — context-aware Markdown submenu
4. **Generate menu** — `Cmd+N` submenu for tables, code blocks, images, footnotes, etc.
5. **File templates** — 6 built-in markdown file templates
6. **Named TOCs** — attribute overrides, content ranges, multiple independent TOCs
7. **Bitbucket Cloud slugs** — slug mode not available in VSCode
8. **Smart table width limiting** — compact formatting for wide tables
9. **Preview zoom** — zoom in/out/reset controls
10. **Pipe wrapper** — type `|` with selection to create table cells
11. **Dash wrapper** — type `-` in table to fill separator
