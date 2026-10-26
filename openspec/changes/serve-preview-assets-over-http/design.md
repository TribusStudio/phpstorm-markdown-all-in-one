# Design — Preview asset delivery over the built-in server

## Context

See `proposal.md` — Why. The constraints and the prior art:

- Today `wrapInDocument` is the single delivery mechanism and it inlines everything: theme CSS, custom CSS, highlight.js, KaTeX, and the scroll-sync and link-interception scripts (`MarkdownHtmlConverter.kt:441-489`). `MarkdownPreviewFileEditor.updatePreview` assembles that string and calls `loadHTML` on a 300 ms debounce.
- The same `wrapInDocument` is used by HTML export, which must stay self-contained. One function currently serves two audiences whose requirements now diverge.
- `loadHTML` gives the page an `about:blank`-class origin with no usable base URL — the reason images are rewritten to absolute `file://` URLs (`:329-338`, comment at `:331`).
- `onBeforeBrowse` blocks every `http://` and `https://` navigation and routes it to the system browser on a user gesture (`:113-139`). Subresource loads — `<script src>`, `<link href>`, `<img src>` — do not pass through it. That asymmetry is what makes Stage 1 safe and Stage 2 delicate.
- **Prior art, verified in the installed IDE:** JetBrains' bundled Mermaid plugin serves a 5,120,388-byte `mermaid.js` exactly this way. `MermaidPreviewStaticServer` implements `org.jetbrains.ide.HttpRequestHandler` on the `/mermaidPreview` prefix, with `BuiltInServerManager.getInstance()` for `getPort` and `addAuthToken`, and `org.jetbrains.io.FileResponses` to send bytes. Its standalone viewer navigates to `/mermaidPreview/index.html?isStandaloneViewer=true` — so both the by-reference asset pattern and the navigate-to-served-page pattern are in production use in the platform.

## Goals / Non-Goals

**Goals**

- Make the cost of an asset proportional to the session, not to the number of refreshes.
- Raise the ceiling on asset size, so a multi-megabyte runtime becomes viable.
- Keep export self-contained, and make the divergence between preview and export explicit rather than incidental.
- Fail soft: a preview that cannot use the server must still be a correct preview.

**Non-Goals (design level)**

- Serving anything that is not a bundled asset or the active document.
- Incremental DOM updates.
- Unifying preview and export delivery. They genuinely want different things, and pretending otherwise is how export breaks.

## Decisions

### 1. Two stages, shipped in order, each independently valuable

Stage 1 (assets by URL, page still `loadHTML`) and Stage 2 (page by `loadURL`) are separated because their risk is not comparable. Stage 1 cannot break navigation — subresources bypass `onBeforeBrowse` — and its failure mode is a missing stylesheet, caught immediately. Stage 2 touches the interceptor that stops the preview becoming a browser, and getting it wrong either blocks the preview entirely or punches a hole in link containment.

Stage 1 alone unblocks `add-mermaid-diagram-preview`. If Stage 2 proves troublesome it can be dropped without losing that, and the `file://` image rewriting simply stays.

### 2. Delivery mode is an explicit parameter, not an ambient condition

`wrapInDocument` gains an explicit delivery mode — by-reference with a base URL, or inlined. Export always passes inlined; the live preview passes by-reference and falls back to inlined. Making this a parameter rather than a global or a `isUnitTestMode`-style check is what keeps export's contract testable: a test can assert that an exported document contains no `http://` reference at all.

### 3. Served set is a fixed allow-list, not a path mapping

The handler resolves a request by looking its path up in a fixed map from served name to bundled resource path. It never concatenates a request path onto a resource root. Traversal, absolute paths, and encoded variants therefore fail by construction rather than by sanitisation, which is the difference between a check someone can forget to update and a design that cannot express the unsafe case.

The active preview document is served per editor, keyed by an opaque identifier rather than by file path, so the endpoint never accepts a filesystem path from the page.

### 4. Authorisation follows the platform, including its failure mode

Requests are authorised with `BuiltInServerManager.addAuthToken`, as the Mermaid plugin does. An unauthorised request gets the same response as an unknown resource, so the endpoint does not confirm what exists.

Because the token and the port are both obtainable only from a live built-in server, every call site has to tolerate their absence — which is Decision 5 rather than an error path.

### 5. Fallback to inlining is a first-class path, not an error branch

If the port cannot be resolved, the server is disabled, or the token cannot be obtained, `updatePreview` builds an inlined document instead. The user sees a correct preview; the reason goes to the log. This keeps the change from turning a configuration the plugin does not control — the built-in server can be switched off in settings — into a broken preview.

*Consequence worth stating:* the inlining path must stay maintained, not left to rot, because it is also export's path. That is a cost of this design and the reason Decision 2 makes the mode explicit.

### 6. Cache keyed by plugin version

Assets are served with long-lived cache headers and a URL carrying the plugin version, so an IDE session reuses them freely while a plugin update cannot serve a stale copy from the previous version. `FileResponses` already implements conditional-request handling, which the handler delegates to rather than reimplementing.

### 7. Stage 2 narrows the navigation interceptor rather than widening it

The interceptor keeps blocking all `http(s)` navigation, with one exception: the exact origin and path prefix the plugin serves, matched against the resolved port — not a prefix match on "localhost". Everything else, including other paths on the same port, stays blocked. The existing `file:` and `data:` allowances are re-examined at the same time, since serving the document removes the reason `file:` navigation was permitted.

### 8. Custom CSS is served, not inlined, in Stage 2

A user's stylesheet is read from disk and served through the handler under a per-editor name. That removes the special-casing in `wrapInDocument` and fixes the staleness question directly: the file is read when the document is generated, and its served copy is not cached across refreshes.

## Risks / Trade-offs

- **New locally-reachable HTTP endpoint in a plugin that currently makes no network connections** → Fixed allow-list (Decision 3), platform auth token (Decision 4), loopback-bound server, and explicit spec requirements for traversal and unauthorised access rather than leaving them to review.
- **Stage 2 could block the preview entirely or weaken link containment** → Staged separately, interceptor narrowed to an exact origin and prefix (Decision 7), with the external-link and blocked-local-address scenarios in the spec as the guard.
- **Two delivery paths means export can silently diverge** → Explicit mode parameter (Decision 2) plus a test asserting exported HTML contains no server reference.
- **The built-in server can be disabled by the user** → Decision 5 makes that a supported configuration, not a failure.
- **Collides with `fix-preview-scroll-sync-drift`** → Both rewrite parts of `updatePreview` and the injected JS. Sequence them; whichever lands second rebases. This is cheaper than reconciling two half-rewrites of the same function.
- **Caching could serve a stale asset after a plugin update** → Version-keyed URLs (Decision 6), with a scenario covering it.
- **Port changes within a session** → The port is resolved when a document is generated rather than cached for the editor's lifetime, so the next refresh picks up a change; a stale page's requests fail closed and the fallback covers the gap.

## Migration Plan

No user-visible migration and no settings change. Ships as a SemVer **minor** bump, ideally with Stage 1 and Stage 2 as separate commits so Stage 2 can be reverted alone.

Rollback for either stage is a revert; the inlining path remains present and exercised by export, so reverting cannot strand the preview.

## Open Questions

- Whether to serve the scroll-sync and link-interception scripts by URL as well, or keep injecting them inline. They are small, they are generated per document rather than bundled, and `fix-preview-scroll-sync-drift` is actively rewriting one of them — so the default is to keep them inline and revisit once that change lands. This affects neither the specs nor the task breakdown.
