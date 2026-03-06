# 002 — VSCode Markdown All-in-One Reference Analysis

**Date:** 2026-03-06
**Status:** Complete
**Source:** `/Users/wilco/Docker/tribus/development/vscode-markdown`

## Overview

The VSCode Markdown All-in-One plugin is our reference implementation. This document catalogs its full feature set to guide our PHPStorm port.

## Feature Summary

| Category | Count |
|----------|-------|
| Commands | 19 direct + 14 keybinding-specific |
| Keyboard shortcuts | 24 |
| Configuration options | 29+ |
| Slug generation modes | 7 |
| Completion types | 3 (file paths, math functions, reference links) |

## Commands

### Formatting
- `toggleBold` — Wrap selection in `**text**` or `__text__`
- `toggleItalic` — Wrap selection in `*text*` or `_text_`
- `toggleStrikethrough` — Wrap selection in `~~text~~`
- `toggleCodeSpan` — Wrap in backticks
- `toggleCodeBlock` — Wrap in fenced code block
- `toggleHeadingUp` — Increase heading level (# to ##)
- `toggleHeadingDown` — Decrease heading level (## to #)
- `toggleMath` — Cycle: none > $inline$ > $$display$$ > $$\nmultiline\n$$
- `toggleMathReverse` — Same in reverse
- `toggleList` — Cycle through list markers: `-`, `*`, `+`, `1.`, `1)`
- `paste` — Smart paste for links

### List Editing (Context-Aware)
- `onEnterKey` — Auto-continue lists, blockquotes, update markers
- `onCtrlEnterKey` — Insert line without auto-continuing list
- `onShiftEnterKey` — Hard line break
- `onTabKey` — Indent list items
- `onShiftTabKey` — Outdent list items
- `onBackspaceKey` — Smart backspace (remove empty items, unindent)
- `checkTaskList` — Toggle checkbox (`[ ]` / `[x]`)
- `onMoveLineUp/Down` — Move list items
- `onCopyLineUp/Down` — Duplicate lines
- `onIndentLines/onOutdentLines` — Multi-line indent/outdent

### TOC
- `toc.create` — Insert TOC at cursor
- `toc.update` — Update existing TOC
- `toc.addSecNumbers` — Add section numbering
- `toc.removeSecNumbers` — Remove section numbering

### Export
- `printToHtml` — Export to HTML
- `printToHtmlBatch` — Batch export folder

### Preview
- `closePreview` — Close preview panel

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| Cmd/Ctrl+B | Toggle Bold |
| Cmd/Ctrl+I | Toggle Italic |
| Alt+S | Toggle Strikethrough |
| Ctrl+Shift+] | Heading Up |
| Ctrl+Shift+[ | Heading Down |
| Cmd/Ctrl+M | Toggle Math |
| Alt+C | Toggle Task Checkbox |
| Enter | Smart Enter (list continuation) |
| Ctrl/Cmd+Enter | Insert line without list continuation |
| Shift+Enter | Hard line break |
| Tab | Indent list item |
| Shift+Tab | Outdent list item |
| Backspace | Smart backspace |
| Alt+Up/Down | Move line up/down |
| Shift+Alt+Up/Down | Copy line up/down |
| Cmd/Ctrl+]/[ | Indent/outdent lines |
| Cmd/Ctrl+V | Smart paste |

## Configuration Options (29+)

### Formatting
- `bold.indicator` — `**` or `__`
- `italic.indicator` — `*` or `_`

### Completion
- `completion.enabled` — Enable auto-completion
- `completion.respectVscodeSearchExclude` — Respect search.exclude
- `completion.root` — Root folder for path completions

### Math
- `math.enabled` — Enable KaTeX support
- `katex.macros` — Custom KaTeX macros

### List Editing
- `list.indentationSize` — `adaptive` or `inherit`
- `list.toggle.candidate-markers` — List markers to cycle
- `orderedList.autoRenumber` — Auto-fix numbering
- `orderedList.marker` — `one` or `ordered`

### Table
- `tableFormatter.enabled` — Enable formatter
- `tableFormatter.normalizeIndentation` — Normalize indentation
- `tableFormatter.delimiterRowNoPadding` — No padding in delimiter

### Export/Print
- `print.absoluteImgPath` — Absolute image paths
- `print.imgToBase64` — Embed images as base64
- `print.includeVscodeStylesheets` — Include editor styles
- `print.onFileSave` — Auto-export on save
- `print.theme` — Light/dark
- `print.validateUrls` — Validate URLs
- `print.pureHtml` — Pure HTML without styles

### TOC
- `toc.levels` — Heading level range (e.g., "2..4")
- `toc.omittedFromToc` — Headings to exclude per file
- `toc.orderedList` — Use numbered list
- `toc.plaintext` — TOC as plain text
- `toc.slugifyMode` — github/gitlab/azure/bitbucket/gitea/vscode/zola
- `toc.unorderedList.marker` — `-`, `*`, or `+`
- `toc.updateOnSave` — Auto-update on save

### Theming/Decorations
- `theming.decoration.renderCodeSpan` — Code span borders
- `theming.decoration.renderStrikethrough` — Strikethrough rendering
- `theming.decoration.renderHardLineBreak` — Show line break arrows
- `theming.decoration.renderLink` — Link indicators
- `theming.decoration.renderParagraph` — Paragraph markers
- `theming.decoration.renderTrailingSpace` — Trailing space highlight
- `syntax.decorationFileSizeLimit` — Disable decorations on large files
- `syntax.plainTheme` — Distraction-free theme

## Key Architecture Patterns

### Smart List Editing (`listEditing.ts`)
- Context detection via `editor-context-service/` — flags for inList, inFencedCodeBlock, inMathEnv
- Enter key auto-continues lists with proper marker incrementation
- Adaptive indentation based on CommonMark spec
- Handles nested lists, blockquotes, and task lists

### Text Formatting (`formatting.ts`)
- Without selection: wraps word at cursor
- With selection: wraps selection
- Toggle behavior: re-applying removes formatting
- Extended word pattern includes `*_` characters

### TOC (`toc.ts`)
- 7 slug generation modes for cross-platform compatibility
- Section numbering with hierarchical auto-generation
- Heading omission via `<!-- omit from toc -->` comments
- Auto-update on save with CodeLens integration

### Table Formatter (`tableFormatter.ts`)
- Parses GFM table structure
- Detects and preserves column alignment
- Auto-pads cells for visual alignment

### Completion (`completion.ts`)
- File/image paths triggered by `(`, `[`, `/`
- Reference links triggered by `[`
- 200+ KaTeX function completions triggered by `\`

## Notable Innovations
1. Smart paste — paste URL with selection creates `[text](url)`
2. Async decoration system with cancellable workers
3. Wasm-based Zola slug generation
4. Third-party extension discovery and loading
5. Context-aware key handlers (same key behaves differently per context)
