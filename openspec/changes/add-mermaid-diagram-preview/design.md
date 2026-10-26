# Design — Mermaid diagram rendering in the live preview

## Context

See `proposal.md` — Why. The constraints that shape the approach:

- The preview is a single `JBCefBrowser` driven by `loadHTML(fullHtml)` on every update (`MarkdownPreviewFileEditor.updatePreview`, `:305-358`). There is no incremental DOM patching: a document change means a **full page reload** after a 300 ms debounce (`UPDATE_DEBOUNCE_MS`). Everything the page needs must be inside that one HTML string — the existing comment at `:331` records that `loadHTML`'s base URL does not reliably resolve local subresources, which is why images are rewritten to absolute `file://` URLs and why highlight.js, KaTeX, and the theme CSS are all inlined by `wrapInDocument`.
- `wrapInDocument` already demonstrates the conditional-asset pattern: highlight.js is inlined only when `bodyHtml.contains("<code class=\"language-")`, KaTeX only when `mathEnabled` (`MarkdownHtmlConverter.kt:450-473`).
- `MarkdownHtmlConverter` is a hand-rolled, single-pass, line-based converter. Fenced blocks are handled by one open/close branch at `:48-64` plus an EOF fallback at `:209-211`.
- The preview must work with no network access and no external executable.
- **Mermaid's self-contained bundle is large.** Measured against the npm registry at the time of writing: `mermaid@12.2.0` ships `dist/mermaid.min.js` at **5.24 MB**. The 30 KB `mermaid.esm.min.mjs` is not an alternative — it is an ESM entry point that lazy-loads diagram-type chunks over the network. JetBrains' bundled Mermaid plugin independently ships `mermaid.js` at **5,120,388 bytes**, which confirms there is no small build to find.
- **That size is no longer the central design problem.** It was, while inlining into the `loadHTML` string was the only delivery mechanism. `serve-preview-assets-over-http` adds a second: bundled assets served over the IDE's built-in web server and fetched once per session. This change depends on its Stage 1 and treats the runtime as one more served asset — which is exactly what JetBrains does with the same file.

## Goals / Non-Goals

**Goals**

- Keep documents without diagrams byte-for-byte and millisecond-for-millisecond unchanged.
- Add no browser plumbing of this change's own: the runtime is one more entry in the asset server's served set, and the update/debounce/scroll-restore cycle is untouched.
- Keep the converter's output independent of how assets are delivered, so this change survives either delivery path.
- Per-diagram failure isolation: one bad diagram must not take out the others or the page.
- One conversion path shared by the live preview and HTML export.

**Non-Goals (design level, beyond the proposal's)**

- Incremental preview updates. Full reloads stay; `serve-preview-assets-over-http` is what makes them cheap.
- Asset delivery itself. It belongs to `serve-preview-assets-over-http`; this change consumes it and adds one asset.
- A custom JCEF scheme handler for asset delivery (rejected under Decision 2).
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

### 2. Serve the runtime; reference it only when the document needs it

The runtime joins the served set in `serve-preview-assets-over-http` and is referenced by `<script src>` rather than inlined. `wrapInDocument` gains a `diagramsEnabled: Boolean = false` parameter (appended, consistent with `mathEnabled`), and emits the reference when **both** the setting is on and `bodyHtml.contains("<pre class=\"mermaid\"")` — the same shape as the existing `hasCodeBlocks` test.

Conditional referencing still matters even though the cost per refresh is now near zero: a document with no diagrams should not fetch 5 MB on its first preview either.

Under the fallback path of that change — built-in server unavailable, assets inlined — a diagram document would inline 5 MB per refresh. Rather than accept that, the fallback **does not render diagrams**: Mermaid blocks degrade to highlighted code blocks, as they do when the setting is off. Diagram rendering is the one asset whose size makes inlining the wrong answer, so it opts out of the fallback instead of being slow.

*Alternative rejected:* inlining unconditionally alongside KaTeX. It would tax every preview in the IDE for a feature most documents do not use.

*Alternative rejected:* a custom JCEF scheme handler (`mdaio://assets/…`) via `JBCefApp`. It would also give cached, by-URL delivery, but it must be registered before CEF initialisation, needs verifying across a range where 2026.2 moved JCEF into a separate bundled plugin, and would add a *third* delivery path. The built-in server is already proven in the platform for this exact asset.

### 4. Render per diagram with explicit error capture, not `startOnLoad`

The inlined bootstrap calls `mermaid.initialize({ startOnLoad: false, securityLevel: 'strict', theme: <light|dark> })`, then walks `document.querySelectorAll('pre.mermaid')` and for each container calls `mermaid.render(...)` inside `try/catch`, replacing the container's contents with the returned SVG on success and with an error region on failure.

*Why not `mermaid.run()` / `startOnLoad: true`:* the per-container loop is what buys the spec's failure isolation and the error region carrying both the parser message and the original source. `mermaid.run` with `suppressErrors` keeps the page alive but does not hand us a per-diagram message, and without it a single parse failure aborts the batch.

*`securityLevel: 'strict'`* disables HTML labels and click bindings in diagram source. The preview renders the user's own file, but a Markdown document is also something you receive from elsewhere, and click handlers are an explicit non-goal — so strict costs us nothing here.

### 5. Theme from the existing light/dark decision

`updatePreview` already computes `isDark` from `PreviewTheme` (`:325-327`) and passes it to `wrapInDocument`. The bootstrap selects Mermaid's `dark` or `default` theme from that same flag. No new theme plumbing, and theme switches propagate on the next reload like every other preview style change.

### 6. Setting shape mirrors `mathEnabled`

One `var diagramsEnabled: Boolean = true` in `MarkdownSettings.State`, one `.bindSelected(state::diagramsEnabled)` checkbox row in `MarkdownSettingsConfigurable` next to the math row (`:140`). A new field defaulting to `true` needs no migration: an existing persisted state without the field deserialises to the default.

## Risks / Trade-offs

- **Depends on `serve-preview-assets-over-http` Stage 1** → That is the sequencing cost of not inlining 5 MB per refresh. If that change is dropped, this one has no acceptable delivery mechanism and should be reconsidered rather than fall back to inlining.
- **5 MB fetched on first preview of a diagram document** → Once per session, cached by Chromium, and only for documents that actually contain a diagram. The spec's "fetched at most once per session" scenario is the guard.
- **Plugin distribution grows by ~5.24 MB** → Accepted; it is the price of offline rendering, and JetBrains' own bundled plugin carries the same weight. Worth a note in the release CHANGELOG entry.
- **Diagrams silently absent under the inlining fallback** → Stated in Decision 2 and surfaced the same way an unparseable diagram is: the block renders as a code block rather than vanishing, so the user sees their source rather than an empty gap.
- **Mermaid's own render time on large diagrams can block the page** → Rendering is asynchronous and per-diagram, so the document's Markdown is visible before diagrams resolve; a slow diagram delays only itself.
- **Mermaid major-version churn** → Pin the exact bundled version, record it in `docs/`, and check it in the test fixture so an upgrade that breaks the container contract fails a test rather than a user's preview.
- **Mermaid can be configured to fetch icon packs over the network** → Not configured, and `securityLevel: 'strict'`; the spec's "no outbound requests" scenario is the regression guard.
- **False-positive fence detection** (e.g. a `mermaid` fence inside a larger fenced block showing example Markdown) → Detection runs only on the opening fence of a block the existing parser recognises, so nested content inside an outer fence is never examined. Covered by a fixture.
- **The 5 MB asset in the repo** → Check whether the repo and CI prefer a build-time download (Gradle dependency/verification) over a committed binary; a committed file is simpler and keeps offline builds working, which matches the devcontainer setup.

## Migration Plan

No data or settings migration. The change is additive and default-on, so the rollout is a normal SemVer **minor** bump with a CHANGELOG entry. It ships after `serve-preview-assets-over-http` Stage 1. Rollback is the new setting: turning **Render Mermaid diagrams** off restores the previous rendering exactly.

## Open Questions

- Which exact Mermaid version to pin. `12.2.0` is current as of writing; the task list resolves this at implementation time by pinning whatever is current and recording it. This changes neither the specs nor the approach.
- Whether to recognise `mmd` as an alias is **resolved: no.** Neither GitHub nor GitLab renders it, so accepting it would make the preview disagree with where the document is published. The detection predicate stays `mermaid` only; adding an alias later is a one-line, purely additive follow-up.
