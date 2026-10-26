# Tasks — Preview asset delivery over the built-in server

## 1. Confirm the platform surface

- [ ] 1.1 Verify at both ends of the supported range (2025.1 and 2026.2) that `org.jetbrains.ide.HttpRequestHandler`, `BuiltInServerManager.getInstance().getPort()`/`addAuthToken`, and `org.jetbrains.io.FileResponses` are present and behave as the bundled Mermaid plugin's `MermaidPreviewStaticServer` uses them.
- [ ] 1.2 Confirm what a JCEF page loaded via `loadHTML` can fetch from `http://localhost:<port>` — specifically that a `<script src>` and a `<link href>` succeed from its opaque origin, since this is what Stage 1 rests on.
- [ ] 1.3 Determine how the auth token must be presented for a subresource fetch the page issues itself, as opposed to a URL the IDE opens, and record the answer.
- [ ] 1.4 Record the findings in a new numbered doc, `docs/010-preview-asset-delivery.md`, including the JetBrains prior art and the measured sizes of the assets being moved.

## 2. The request handler

- [ ] 2.1 Implement the handler on a `/mdaioPreview` path prefix with `isSupported`/`process`, registered as an `httpRequestHandler` in `plugin.xml` with `dynamic="true"`.
- [ ] 2.2 Resolve requests through a fixed allow-list map from served name to bundled resource path, never by concatenating a request path onto a root.
- [ ] 2.3 Serve bytes via `FileResponses`, delegating conditional-request and cache-header handling to it, with URLs keyed by plugin version.
- [ ] 2.4 Reject unauthorised requests with the same response as unknown resources.
- [ ] 2.5 Unit-test rejection of: traversal segments, encoded traversal, absolute paths, a bundled resource outside the served set, a project file path, and an unauthorised request for a valid asset.
- [ ] 2.6 Unit-test that every name in the allow-list actually resolves to a resource present in the jar, so a renamed asset fails a test rather than a user's preview.

## 3. Stage 1 — assets by reference

- [ ] 3.1 Add an explicit delivery mode to `wrapInDocument`: by-reference with a base URL, or inlined.
- [ ] 3.2 Emit `<link>`/`<script>` references for theme CSS, highlight.js, and KaTeX when by-reference; keep the existing conditional gating so unused assets are still not referenced.
- [ ] 3.3 In `updatePreview`, resolve the port and token and select by-reference delivery, falling back to inlined when either is unavailable, logging the reason once rather than per refresh.
- [ ] 3.4 Leave `HtmlExporter` on inlined delivery, and test that exported HTML contains no `http://` or port reference.
- [ ] 3.5 Unit-test `wrapInDocument` in both modes: by-reference emits references and no inlined payload; inlined emits payloads and no references.
- [ ] 3.6 Confirm by hand that highlighting, math, and themes all still render, and that a second refresh issues no new asset request.

## 4. Stage 2 — the document by URL

- [ ] 4.1 Narrow the navigation interceptor **first**, before switching delivery: permit exactly the served origin and path prefix matched against the resolved port, keep blocking every other `http(s)` URL, and re-examine whether the `file:` and `data:` allowances are still needed.
- [ ] 4.2 Test the interceptor directly: the served document is permitted; another path on the same port is blocked; another local port is blocked; an external URL is blocked and routed to the system browser.
- [ ] 4.3 Serve the generated document per editor under an opaque identifier, never keyed by filesystem path.
- [ ] 4.4 Switch `updatePreview` to `loadURL` against that identifier, keeping the scroll-restore cycle and the `JBCefJSQuery` bridges working.
- [ ] 4.5 Remove the `file://` image rewriting from the live preview path, keeping it for export, and confirm relative, parent-directory, and missing images all behave per spec.
- [ ] 4.6 Serve custom user CSS through the handler per editor, not cached across refreshes, and confirm an edit to the stylesheet is picked up on the next refresh.
- [ ] 4.7 Confirm anchor links still scroll within the page and external links still open in the system browser.

## 5. Verify in a running IDE

- [ ] 5.1 With `./dev gradle runIde`, walk every scenario in `specs/preview/asset-delivery/spec.md`.
- [ ] 5.2 Measure refresh latency while typing in a document using highlighting and math, before and after, and record the numbers in the doc from 1.4.
- [ ] 5.3 Disable the built-in server in IDE settings and confirm the preview still renders fully styled, with the reason logged and no notification.
- [ ] 5.4 Confirm scroll sync, position restore, theme switching, and custom CSS all behave as before.
- [ ] 5.5 Export a document with highlighting and math, open it on a machine with no IDE running and no network, and confirm it renders.
- [ ] 5.6 Smoke-test both stages on 2025.1 and 2026.2, including the 2026.2 JCEF-as-bundled-plugin path.
- [ ] 5.7 Confirm no error appears in the IDE log across open, edit, theme switch, and close.

## 6. Ship

- [ ] 6.1 Run `./dev test` and fix any regressions, including converter tests whose expected HTML changes under by-reference delivery.
- [ ] 6.2 Update `README.md` only if the behaviour is user-visible; otherwise confine documentation to `docs/010-preview-asset-delivery.md`.
- [ ] 6.3 Note in `docs/` that the preview and export delivery paths deliberately differ, and why.
- [ ] 6.4 Bump the minor version with `./scripts/bump-version.sh minor` and add the `CHANGELOG.md` entry, framed as preview responsiveness.
- [ ] 6.5 Update `ROADMAP.md` per the project's phase conventions.
- [ ] 6.6 Commit Stage 1 and Stage 2 as separate Conventional Commits on a `feature/preview-asset-delivery` branch so Stage 2 can be reverted alone, and open a PR whose test plan is section 5.
