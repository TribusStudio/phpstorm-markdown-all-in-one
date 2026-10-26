# Design — Mermaid diagram rendering in the live preview

## Context

See `proposal.md` — Why. The constraints that shape the approach:

- The preview is a single `JBCefBrowser` driven by `loadHTML(fullHtml)` on every update (`MarkdownPreviewFileEditor.updatePreview`, `:305-358`). There is no incremental DOM patching: a document change means a **full page reload** after a 300 ms debounce (`UPDATE_DEBOUNCE_MS`). Everything the page needs must be inside that one HTML string — the existing comment at `:331` records that `loadHTML`'s base URL does not reliably resolve local subresources, which is why images are rewritten to absolute `file://` URLs and why highlight.js, KaTeX, and the theme CSS are all inlined by `wrapInDocument`.
- `wrapInDocument` already demonstrates the conditional-asset pattern: highlight.js is inlined only when `bodyHtml.contains("<code class=\"language-")`, KaTeX only when `mathEnabled` (`MarkdownHtmlConverter.kt:450-473`).
- `MarkdownHtmlConverter` is a hand-rolled, single-pass, line-based converter. Fenced blocks are handled by one open/close branch at `:48-64` plus an EOF fallback at `:209-211`.
- The preview must work with no network access and no external executable.
- **Mermaid's self-contained bundle is large.** Measured against the npm registry at the time of writing: `mermaid@12.2.0` ships `dist/mermaid.min.js` at **5.24 MB**. The 30 KB `mermaid.esm.min.mjs` is not an alternative — it is an ESM entry point that lazy-loads diagram-type chunks over the network, which an inlined single-file document cannot do.

That size is the central design problem: at 5.24 MB, inlining unconditionally would add megabytes to **every** preview reload in the IDE, including documents with no diagrams.

## Goals / Non-Goals

**Goals**

- Keep documents without diagrams byte-for-byte and millisecond-for-millisecond unchanged.
- Keep diagram rendering inside the existing single-string `loadHTML` architecture — no new browser plumbing, no change to the update/debounce/scroll-restore cycle.
- Per-diagram failure isolation: one bad diagram must not take out the others or the page.
- One conversion path shared by the live preview and HTML export.

**Non-Goals (design level, beyond the proposal's)**

- Incremental preview updates. Switching from `loadHTML` to DOM patching would make the runtime cost a one-time load instead of per-update, and is the right long-term fix, but it is a far larger change to `MarkdownPreviewFileEditor` and is out of scope here.
- A custom JCEF scheme handler for asset delivery (see Decision 3).
- Shrinking the Mermaid bundle by building a custom subset of diagram types.

## Decisions

### 1. Detect the fence language in the converter; render in the page

Fence detection stays where fences are already parsed: the branch at `MarkdownHtmlConverter.kt:48-64`. When the info string's first token matches `mermaid` case-insensitively — after stripping surrounding braces and ignoring any trailing attributes — the converter emits

```
<pre class="mermaid" data-source-line="N">ESCAPED SOURCE</pre>
```

instead of `<pre><code class="language-mermaid">`. Rendering itself happens in the page, by the inlined runtime, replacing each container's contents with SVG.

*Why:* the container is the smallest possible change to the converter, it keeps the `data-source-line` annotation that scroll sync depends on (`buildScrollSyncJs` queries `[data-source-line]`), and because the source is HTML-escaped text content, Mermaid reads it back correctly via `textContent`.

*Alternative rejected:* rendering diagrams to SVG on the JVM side and inlining the SVG. There is no pure-JVM Mermaid implementation; it would mean an embedded JS engine or a headless browser per diagram.

### 2. Inline the runtime, gated on the document actually containing a diagram

`wrapInDocument` gains a `diagramsEnabled: Boolean = false` parameter (appended, consistent with `mathEnabled`). The runtime is inlined when **both** the setting is on and `bodyHtml.contains("<pre class=\"mermaid\"")` — the same shape as the existing `hasCodeBlocks` test. The 5.24 MB resource is read from the jar once and memoised in a lazily-initialised field rather than re-read per update, since `loadResource` currently reads the stream on every call.

*Cost accepted:* a document that does contain diagrams pays ~5.24 MB of string assembly plus Chromium parse/compile on every 300 ms-debounced reload. This is the known trade-off and is bounded to diagram documents only.

*Alternative rejected:* inlining unconditionally alongside KaTeX. It would tax every preview in the IDE for a feature most documents do not use.

### 3. No custom JCEF scheme handler — for now, with a stated trigger

The architecturally cleaner delivery mechanism is to register a custom scheme (e.g. `mdaio://assets/mermaid.min.js`) via `com.intellij.ui.jcef.JBCefApp`'s custom-scheme-handler registration, so the page references the runtime by URL and Chromium caches it across reloads.

We are not doing that in this change: the registration must happen before CEF initialisation, it has to be verified across the whole 2025.1–2026.2 range — where 2026.2 moved JCEF out of the platform core into the separately bundled plugin that `META-INF/jcef-support.xml` already handles — and it introduces a second asset-delivery path alongside the inlining used by every other preview asset.

**Trigger for revisiting:** if the measurement task shows added preview-update latency on a diagram document above ~400 ms (beyond the existing 300 ms debounce, i.e. visible lag while typing in a document with diagrams), the scheme handler becomes a follow-up change rather than a rewrite of this one — the converter-side contract is identical either way, which is precisely why it is safe to defer.

### 4. Render per diagram with explicit error capture, not `startOnLoad`

The inlined bootstrap calls `mermaid.initialize({ startOnLoad: false, securityLevel: 'strict', theme: <light|dark> })`, then walks `document.querySelectorAll('pre.mermaid')` and for each container calls `mermaid.render(...)` inside `try/catch`, replacing the container's contents with the returned SVG on success and with an error region on failure.

*Why not `mermaid.run()` / `startOnLoad: true`:* the per-container loop is what buys the spec's failure isolation and the error region carrying both the parser message and the original source. `mermaid.run` with `suppressErrors` keeps the page alive but does not hand us a per-diagram message, and without it a single parse failure aborts the batch.

*`securityLevel: 'strict'`* disables HTML labels and click bindings in diagram source. The preview renders the user's own file, but a Markdown document is also something you receive from elsewhere, and click handlers are an explicit non-goal — so strict costs us nothing here.

### 5. Theme from the existing light/dark decision

`updatePreview` already computes `isDark` from `PreviewTheme` (`:325-327`) and passes it to `wrapInDocument`. The bootstrap selects Mermaid's `dark` or `default` theme from that same flag. No new theme plumbing, and theme switches propagate on the next reload like every other preview style change.

### 6. Setting shape mirrors `mathEnabled`

One `var diagramsEnabled: Boolean = true` in `MarkdownSettings.State`, one `.bindSelected(state::diagramsEnabled)` checkbox row in `MarkdownSettingsConfigurable` next to the math row (`:140`). A new field defaulting to `true` needs no migration: an existing persisted state without the field deserialises to the default.

## Risks / Trade-offs

- **5.24 MB re-shipped per reload on diagram documents** → Gated to diagram documents; the runtime resource string is memoised; the existing 300 ms debounce still coalesces typing. Decision 3 records the measured trigger and the escape hatch.
- **Plugin distribution grows by ~5.24 MB** → Accepted; it is the price of offline rendering and is in line with other Markdown plugins that bundle Mermaid. Worth a note in the release CHANGELOG entry.
- **Mermaid's own render time on large diagrams can block the page** → Rendering is asynchronous and per-diagram, so the document's Markdown is visible before diagrams resolve; a slow diagram delays only itself.
- **Mermaid major-version churn** → Pin the exact bundled version, record it in `docs/`, and check it in the test fixture so an upgrade that breaks the container contract fails a test rather than a user's preview.
- **Mermaid can be configured to fetch icon packs over the network** → Not configured, and `securityLevel: 'strict'`; the spec's "no outbound requests" scenario is the regression guard.
- **False-positive fence detection** (e.g. a `mermaid` fence inside a larger fenced block showing example Markdown) → Detection runs only on the opening fence of a block the existing parser recognises, so nested content inside an outer fence is never examined. Covered by a fixture.
- **The 5 MB asset in the repo** → Check whether the repo and CI prefer a build-time download (Gradle dependency/verification) over a committed binary; a committed file is simpler and keeps offline builds working, which matches the devcontainer setup.

## Migration Plan

No data or settings migration. The change is additive and default-on, so the rollout is a normal SemVer **minor** bump with a CHANGELOG entry. Rollback is the new setting: turning **Render Mermaid diagrams** off restores the previous rendering exactly, which also makes it the user-side mitigation if the latency trade-off bites a particular document.

## Open Questions

- Which exact Mermaid version to pin. `12.2.0` is current as of writing; the task list resolves this at implementation time by pinning whatever is current and recording it. This changes neither the specs nor the approach.
- Whether to recognise `mmd` as an alias is **resolved: no.** Neither GitHub nor GitLab renders it, so accepting it would make the preview disagree with where the document is published. The detection predicate stays `mermaid` only; adding an alias later is a one-line, purely additive follow-up.
