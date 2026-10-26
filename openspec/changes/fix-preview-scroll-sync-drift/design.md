# Design — Scroll-sync anchor map

## Context

See `proposal.md` — Why for the four root causes. The structural facts the design has to work with:

- The preview is reloaded in full on every document change (`loadHTML`, 300 ms debounce), so any page-side state is rebuilt per update. Nothing can be cached across reloads.
- The only channel from page to IDE is a `JBCefJSQuery` carrying a single integer line number (`MarkdownPreviewFileEditor.kt:87-93`). The only channel the other way is `executeJavaScript` calling `window.__scrollToSourceLine(line)`.
- Both directions are therefore *line*-valued at the boundary. Pixel arithmetic has to happen on whichever side owns the pixels: the page owns preview pixels, the editor owns editor pixels. This is the right split and the design keeps it.
- `annotateSourceLines` is already a parameter threaded through `convert`, and `sl(line, annotate)` is the single place where annotations are emitted.
- A discovered sub-bug worth fixing here: `lastVisibleSourceLine` is tracked unconditionally so that preview refreshes can restore position (`MarkdownSplitEditor.kt:92-93`), but the restore calls `window.__scrollToSourceLine`, which `buildScrollSyncJs` only defines when `scrollSyncEnabled` is true (`:491`). With sync switched off, every edit therefore throws the reader back to the top of the document. Position restore and scroll sync are separate concerns and need separating.

## Goals / Non-Goals

**Goals**

- One mapping, used by both directions, so they are inverses by construction rather than two heuristics tuned separately.
- Monotonic and total: no backwards movement, no dead zones, no unreachable regions.
- No per-scroll-tick layout reads.
- Unit-testable where it can be: the annotations the converter emits are pure string output.

**Non-Goals (design level)**

- Moving pixel arithmetic across the JS/JVM boundary. Passing pixel offsets or ratios instead of lines would couple the IDE to the page's layout.
- Replacing the full-reload update cycle, even though a persistent page would let the anchor map be cached.
- Anything that fires on raw scroll events without debouncing.

## Decisions

### 1. Anchor map: `(pixelTop, line)` pairs from both ends of every block

On page load the bootstrap walks `[data-source-line]` once and produces a sorted array of anchors. Each annotated element contributes:

- `(top, startLine)` — its top edge;
- `(bottom, endLine + 1)` — its bottom edge, where `endLine` comes from the new `data-source-line-end` attribute, falling back to `startLine` when the element is single-line.

Plus a terminal anchor `(scrollHeight, totalLines)` so the trailing region is covered. The array is then made monotonic by a single forward pass that drops any anchor whose pixel or line value does not advance, which handles coincident anchors — a `<ul>` and its first `<li>` share both a top edge and a start line (`MarkdownHtmlConverter.kt:397`, `:179`) — without special-casing them.

Lookup in either direction is a binary search followed by linear interpolation within the bracketing pair.

*Why both ends:* this is the actual fix for the `KNOWN-ISSUES` symptom. Interpolating between *starts* of consecutive blocks silently assumes the gap between two starts is filled by the first block's content, which is false whenever a tall block is followed by a short one. Anchoring the bottom edge makes the interior of every long block a real interpolation interval, and makes the final block's interior reachable.

*Alternative rejected:* annotating every line inside code blocks and every table row. It would give finer anchors, but it means wrapping each code line in a span — which breaks highlight.js's expectation of a single text node inside `<code>` — and bloats the HTML on exactly the documents that are already largest.

### 2. `totalLines` is passed in, not inferred

The page cannot know the document's line count — the last annotated element may be followed by trailing blank lines. `wrapInDocument` emits the count as a `data-total-lines` attribute on the body, from `document.text`'s line count, which the terminal anchor reads.

### 3. Measure with `getBoundingClientRect`, not `offsetTop`

Anchors are measured as `el.getBoundingClientRect().top + window.scrollY` (and `.bottom` for the lower edge), which is in the same coordinate space as `window.scrollY` whatever the page's padding, margins, or positioning. The `+ 10` fudge at `:524` is deleted rather than retuned — it was compensating for `.markdown-body`'s `padding: 32px`, and any value chosen here would be wrong under custom CSS.

*Cost:* `getBoundingClientRect` forces layout. That is why the map is built once per load rather than per scroll tick — which is also a net improvement over today's per-tick `querySelectorAll` plus `offsetTop` reads.

### 4. Editor → preview resolves through the same map, then scrolls to a pixel offset

`__scrollToSourceLine(line)` becomes: look up `line` in the map, interpolate to a pixel offset, `window.scrollTo(0, offset)`. It no longer calls `scrollIntoView` on an element, which is what pinned the preview to a block's top for the block's whole extent.

### 5. Editor side scrolls to put the line at the viewport top

On the preview→editor side, `ScrollType.MAKE_VISIBLE` is replaced by an explicit vertical scroll: compute `editor.logicalPositionToXY(LogicalPosition(line, 0)).y` and scroll the viewport there with `ScrollingModel.scrollVertically`. None of the `ScrollType` values express "put this line at the top" — `MAKE_VISIBLE` is a no-op when the line is on screen, and `CENTER*` disagree with the top-of-viewport reference the editor→preview direction already uses (`MarkdownSplitEditor.kt:88`).

This also keeps the caret untouched: `scrollVertically` moves the viewport and nothing else.

### 6. Re-entrancy: last programmatic target, with the timer as a backstop

Today both sides use fixed settle timers (`SCROLL_SETTLE_MS`, and a 100 ms `__scrollFromEditor` window in JS). A fixed window is wrong in both directions at once: too short and a slow scroll echoes back as a loop; too long and the user's own scrolling is swallowed.

Instead each side records the position it last commanded the other to take. An incoming sync request is ignored when it would land within a small tolerance of the position this side last commanded — that is the echo — and honoured otherwise. The existing timers stay as a backstop so a dropped echo cannot wedge the guard.

### 7. Rebuild the map when layout changes after load

Images are rewritten to `file://` URLs and load asynchronously, so the first layout under-measures the page. The bootstrap rebuilds the anchor map on `window.load`, on each image's `load`, and on a `ResizeObserver` of the body — debounced, and marked so a rebuild is never mistaken for a user scroll. This also covers the Mermaid containers of `add-mermaid-diagram-preview` without further work, since diagrams resolve after load and change body height.

### 8. Separate position restore from scroll sync

`__scrollToSourceLine` and the anchor map are always emitted; only the *scroll listener* that reports back to the IDE is gated on `scrollSyncEnabled`. That fixes the sub-bug in Context without adding a setting: with sync off, edits still preserve the reader's place, and neither pane follows the other.

## Risks / Trade-offs

- **This is the hottest path in the plugin's UI** → The anchor map is built once per load instead of per tick, so the change is net cheaper; the scenarios in the spec are written to be walked by hand in `runIde`; and the converter half is unit-tested from pure string output.
- **Annotating blockquote interiors requires passing a line offset into the recursive `convert` call** (`:153-156`), which currently exists precisely to avoid that → Add an explicit `lineOffset` parameter defaulting to `0`; the blockquote branch already carries each inner line's true index in `innerLines` (`it.second`), so the offset is available and does not need recomputing.
- **The map is only as monotonic as the document order of annotations** → The forward-pass monotonicity filter makes that an invariant of the map rather than an assumption about the converter, so a future converter change degrades accuracy instead of breaking direction.
- **Interpolation is still linear within an interval** → Inherent, and stated as a non-goal in the proposal. What this change removes is systematic error and dead zones, not the non-linearity of rendered height.
- **`data-source-line-end` appears in exported HTML** → Inert, consistent with the existing `data-source-line`, and only present when annotations are on.
- **Replacing the settle-timer guard could reintroduce oscillation if the tolerance is wrong** → Keep the timers as a backstop rather than deleting them, and make the tolerance a named constant that the rapid-scroll scenario exercises.

## Migration Plan

No migration. Behaviour-only change to an existing feature, shipped as a SemVer **patch** or **minor** depending on what else rides along; `KNOWN-ISSUES.md` loses the drift entry. Rollback is a revert — there is no flag to turn off, since the existing `scrollSyncEnabled` governs the feature as a whole either way.
