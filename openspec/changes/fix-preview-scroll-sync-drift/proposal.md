# Fix scroll-sync drift between editor and preview

## Why

**Symptom:** scrolling the preview moves the editor approximately, not accurately. `KNOWN-ISSUES.md` records the residual drift after the v0.16.1 improvement as "minor discrepancies on very long blocks without intermediate annotations". Three behaviours are visible in practice: the editor often does not move at all for a while and then jumps a screenful; dragging through a long code block, table, or blockquote barely moves the editor; and scrolling into the last block of a document cannot reach its end.

**Root causes** — four distinct ones, which is why the symptom has resisted a single fix:

1. **`ScrollType.MAKE_VISIBLE` does nothing when the line is already visible.** `MarkdownSplitEditor.kt:113-116` scrolls the editor with `scrollTo(LogicalPosition(line, 0), ScrollType.MAKE_VISIBLE)`. By contract that is a no-op while the target line is anywhere on screen, so the editor stays put through nearly a full viewport of preview scrolling and then jumps when the line finally falls off the edge. The opposite direction uses the *top* visible line as its reference (`:88`), so the two directions do not even agree on what "synchronised" means.

2. **Long blocks carry one annotation for the whole block.** A fenced code block is annotated only at its opening fence (`MarkdownHtmlConverter.kt:61`); a table only at its `<table>` (`:310`); a blockquote only at its `<blockquote>`, because its inner content is converted with `convert(innerMarkdown, false)` — annotations explicitly off, and over a re-indexed substring whose line numbers would not match the document anyway (`:153-156`). The interpolation in `buildScrollSyncJs` can only interpolate *between* annotated elements, so it has no idea where within such a block the viewport sits.

3. **`offsetTop` is not measured in the same coordinate space as `window.scrollY`.** The JS compares `elements[i].offsetTop` against `window.scrollY + 10` (`MarkdownPreviewFileEditor.kt:525-531`). `offsetTop` is measured from the offset parent's padding edge, and `.markdown-body` carries `padding: 32px`, so every anchor is systematically offset from the scroll position it is compared against — the hard-coded `+ 10` is a partial mask over a constant error, and any custom CSS that positions a container shifts it again.

4. **Editor → preview does not interpolate at all.** `__scrollToSourceLine` picks the last element whose line is `<= targetLine` and calls `scrollIntoView` (`:498-511`). Within a 100-line code block, the preview is pinned to that block's top for all 100 lines and then jumps. The directions are asymmetric by construction.

A fifth, smaller one: past the last annotated element the code reports that element's line verbatim (`:552-554`), so the entire trailing region of a document maps to a single line.

**Fix:** build one monotonic, piecewise-linear map between source lines and pixel offsets, anchored at both the **start and end** of every multi-line block plus the document's end, and drive **both** directions from that same map — so the two directions are inverses of each other by construction rather than two separately-tuned heuristics. Then correct the coordinate space, and scroll the editor so the mapped line lands at the viewport top instead of relying on `MAKE_VISIBLE`.

## What Changes

- Multi-line block elements gain an end-line annotation alongside the existing start-line one: fenced code blocks, tables, blockquotes, and list containers. `convertTable` already returns its end index, and the fence and blockquote loops already know theirs.
- Blockquote inner content is converted with annotations **enabled** and with a line offset, so nested content carries true document line numbers instead of none.
- The preview page builds a line↔pixel anchor map once per page load — rather than re-querying the DOM and reading layout on every scroll tick — from both ends of each annotated block, with a final anchor at the document's end mapped to the document's last line.
- Preview → editor resolves the viewport's top through that map; editor → preview resolves a line through the same map inverted, and scrolls to the computed pixel offset instead of snapping to an element's top.
- Positions are measured with `getBoundingClientRect().top + window.scrollY` so anchors and scroll position share one coordinate space, and the `+ 10` fudge is removed.
- The editor side scrolls so the mapped line sits at the top of the viewport, mirroring the reference point the editor→preview direction already uses, replacing `ScrollType.MAKE_VISIBLE`.
- Re-entrancy between the two directions is guarded by comparing against the last programmatic target rather than only by a fixed settle timeout, so a slow scroll cannot ping-pong and a fast one is not needlessly dropped.
- The anchor map is rebuilt when the page's layout changes after load — image loads and, once it lands, diagram rendering — because both change element heights after the initial measurement.

### Non-goals

- **Pixel-perfect synchronisation.** The relationship between source lines and rendered height is not linear and cannot be: one source line holding an image occupies hundreds of pixels, forty lines of code occupy a predictable block. The goal is a monotonic map with no systematic error and no dead zones, not exactness.
- **Soft-wrap-aware line mapping** in the editor. The editor reports and consumes logical lines; a logical line spanning several visual lines stays a single anchor.
- **Replacing the hand-rolled converter** or restructuring the full-reload preview update cycle.
- **Sub-line (column/character) precision**, and synchronising the caret. This change moves the viewport only; it must never move the caret.
- **A new user setting.** The existing `scrollSyncEnabled` continues to govern the whole feature.

## Capabilities

### New Capabilities

- `preview/scroll-synchronization`: the observable contract for bidirectional scroll sync — monotonicity, the agreed reference point, behaviour inside and across long blocks, document start and end, absence of feedback loops, and behaviour when sync is disabled.

### Modified Capabilities

None — `openspec/specs/` holds no existing requirement for this behaviour.

## Impact

**Code**

- `preview/MarkdownHtmlConverter.kt` — fence emission (`:61`, `:210`), blockquote conversion (`:153-156`), `convertTable` (`:309-310`), `adjustListNesting` (`:397`, `:403`), and the `sl` helper (`:500`), which gains an end-line form.
- `preview/MarkdownPreviewFileEditor.kt` — `buildScrollSyncJs` (`:486-568`) is substantially rewritten; `scrollToSourceLine` (`:363`) and the scroll-restore path (`:350-357`) keep their signatures.
- `preview/MarkdownSplitEditor.kt` — `setupScrollSync` (`:83-123`): the reference point on the preview→editor side and the re-entrancy guards on both.

**Platform APIs**

`com.intellij.openapi.editor.ScrollingModel` (`scrollTo`/`ScrollType` replaced by an explicit vertical scroll derived from `logicalPositionToXY`), `Editor.xyToLogicalPosition`, `VisibleAreaListener`, `com.intellij.ui.jcef.JBCefJSQuery` for the preview→editor channel, `com.intellij.util.Alarm` for the settle timers. No new extension points.

**Versions in scope**

PhpStorm 2025.1 through 2026.2 — the full declared range. No version-specific behaviour; where JCEF is unavailable the preview already degrades to the "preview not available" panel and scroll sync is simply absent.

**Users**

Behaviour-only change to an existing feature, no settings migration. The `data-source-line-end` attributes also appear in exported HTML, which is inert there.

**Risk**

This touches the code path that runs on every scroll event in every open Markdown editor, so a regression is highly visible. The scenarios in the spec are written to be checkable by hand in a running IDE, and the anchor-map construction is pure enough to unit-test from the converter's output.
