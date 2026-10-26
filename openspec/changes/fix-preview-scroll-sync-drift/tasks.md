# Tasks — Scroll-sync anchor map

## 1. End-line annotations in the converter

- [ ] 1.1 Extend the `sl` helper in `MarkdownHtmlConverter` with a form that emits `data-source-line` plus `data-source-line-end`, leaving the single-line form unchanged.
- [ ] 1.2 Emit an end line on fenced code blocks: the closing fence's index in the main branch (`:58-62`) and the document's last index in the EOF fallback (`:209-211`).
- [ ] 1.3 Emit an end line on tables — `convertTable` already computes and returns its end index (`:309`).
- [ ] 1.4 Emit an end line on blockquotes, from the last index collected in `innerLines`.
- [ ] 1.5 Emit an end line on `<ul>`/`<ol>` containers in `adjustListNesting` (`:397`, `:403`), closing them at the index of their last item.
- [ ] 1.6 Add a `lineOffset: Int = 0` parameter to `convert` and use it so the recursive blockquote call (`:153-156`) can run with `annotateSourceLines = true` and produce true document line numbers for inner content.
- [ ] 1.7 Emit `data-total-lines` on the body in `wrapInDocument`, from the source document's line count.
- [ ] 1.8 Unit-test the emitted annotations for: a 100-line code block, a 50-row table, a 40-line blockquote with nested paragraphs and a nested list, a nested ordered/unordered list, consecutive blocks with no blank line between them, and an unterminated code fence at EOF. Assert start and end lines against the fixture's real line numbers.
- [ ] 1.9 Add `before.md` / `after.md` fixtures in `src/test/testData/` for the above.

## 2. Anchor map in the page

- [ ] 2.1 Rewrite `buildScrollSyncJs` to build, on load, a sorted anchor array of `(pixelTop, line)` pairs taking each annotated element's top edge with its start line and its bottom edge with `end + 1`, measured via `getBoundingClientRect().top/.bottom + window.scrollY`.
- [ ] 2.2 Append the terminal anchor `(document.body.scrollHeight, totalLines)` read from `data-total-lines`.
- [ ] 2.3 Apply a forward pass that drops non-advancing anchors so the array is strictly monotonic in both pixel and line.
- [ ] 2.4 Implement `lineForOffset(px)` and `offsetForLine(line)` as binary search plus linear interpolation within the bracketing pair, each clamped at both ends.
- [ ] 2.5 Delete the per-scroll `querySelectorAll` and `offsetTop` reads, and the `+ 10` fudge, from the scroll handler; the handler now consults the prebuilt map only.
- [ ] 2.6 Rewrite `window.__scrollToSourceLine(line)` to `window.scrollTo(0, offsetForLine(line))` instead of `scrollIntoView` on an element.
- [ ] 2.7 Rebuild the map, debounced, on `window.load`, on each `img` `load`, and from a `ResizeObserver` on the body — flagged so a rebuild is not reported as a user scroll.
- [ ] 2.8 Emit the anchor map and `__scrollToSourceLine` unconditionally, and gate only the scroll *listener* on `scrollSyncEnabled`, so preview position restore works with sync switched off.

## 3. Editor side

- [ ] 3.1 In `MarkdownSplitEditor.setupScrollSync`, replace `scrollTo(LogicalPosition(line, 0), ScrollType.MAKE_VISIBLE)` with an explicit vertical scroll derived from `editor.logicalPositionToXY(LogicalPosition(line, 0)).y`, putting the line at the viewport top.
- [ ] 3.2 Confirm the new path does not touch the caret or the selection, and add a `BasePlatformTestCase` test asserting caret offset and selection are unchanged after a simulated preview→editor sync.
- [ ] 3.3 Record the last position each side commanded the other to take, and ignore an incoming request that lands within a tolerance of it; keep `SCROLL_SETTLE_MS` as a backstop and make the tolerance a named constant.
- [ ] 3.4 Clamp the incoming line to the document's valid range before constructing a `LogicalPosition`, so a stale page cannot produce an out-of-range position.

## 4. Verify in a running IDE

- [ ] 4.1 With `./dev gradle runIde`, walk every scenario in `specs/preview/scroll-synchronization/spec.md` against a fixture document containing headings, long prose, a 100-line code block, a 50-row table, a 40-line blockquote, nested lists, and several large images.
- [ ] 4.2 Confirm the editor advances continuously during a slow preview scrollbar drag, with no stationary-then-jump behaviour.
- [ ] 4.3 Confirm both panes reach the top and the bottom together, with the document ending in a long code block.
- [ ] 4.4 Confirm no oscillation under several seconds of rapid continuous scrolling in each direction.
- [ ] 4.5 Confirm no offset under each bundled preview theme, and under a custom CSS file that changes body padding.
- [ ] 4.6 Confirm the caret does not move while scrolling the preview.
- [ ] 4.7 Confirm that typing mid-document keeps the preview in place, both with scroll sync enabled and disabled.
- [ ] 4.8 Confirm synchronisation corrects itself once large images finish loading.
- [ ] 4.9 Check for dropped frames or input lag while scrolling a 5,000-line document, comparing against the current release.

## 5. Ship

- [ ] 5.1 Run `./dev test` and fix any regression, paying attention to existing converter tests whose expected HTML now carries end-line attributes.
- [ ] 5.2 Remove the scroll-synchronisation entry from `KNOWN-ISSUES.md`, or narrow it to whatever measurably remains.
- [ ] 5.3 Update `docs/006-phase9-scroll-sync-toolbar-spec.md` to describe the anchor-map model, and add a numbered doc if the detail outgrows that file.
- [ ] 5.4 Bump the version with `./scripts/bump-version.sh` and add the `CHANGELOG.md` entry, naming the four fixed causes.
- [ ] 5.5 Update `ROADMAP.md` per the project's phase conventions.
- [ ] 5.6 Commit on a `fix/preview-scroll-sync-drift` branch using Conventional Commits, and open a PR whose test plan is section 4.
