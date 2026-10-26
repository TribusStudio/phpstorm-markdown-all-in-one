# Serve preview assets over the IDE's built-in web server

## Why

**Symptom:** every preview refresh re-ships every asset the page needs. `MarkdownPreviewFileEditor.updatePreview` rebuilds the whole document and calls `loadHTML` on a 300 ms debounce (`:305-358`), and `wrapInDocument` inlines the theme CSS, any custom CSS, highlight.js (122 KB) and KaTeX (277 KB + 26 KB CSS) into that string (`MarkdownHtmlConverter.kt:450-489`). Typing in a document with math means assembling and re-parsing roughly 300 KB of JavaScript per keystroke-debounce. It also puts a hard ceiling on any larger asset: `add-mermaid-diagram-preview` needs a 5.24 MB runtime, which is simply not shippable this way.

**Root cause:** there is only one delivery mechanism, and it is string inlining. That choice was forced by a real constraint, recorded in the comment at `:331` — `loadHTML`'s base URL does not reliably resolve local subresources, which is also why images are rewritten to absolute `file://` URLs through `HtmlExporter.resolveImagePaths`.

**Fix:** serve the assets over HTTP from the IDE's own built-in web server and reference them by URL, so Chromium fetches and caches each one once per session instead of re-parsing it per refresh.

This is not a novel approach: it is precisely what JetBrains' own bundled Mermaid plugin does. `MermaidPreviewStaticServer` implements `org.jetbrains.ide.HttpRequestHandler` on a `/mermaidPreview` path prefix, resolves the port through `org.jetbrains.ide.BuiltInServerManager`, authorises requests with `addAuthToken`, and sends bytes with `org.jetbrains.io.FileResponses` — serving a 5,120,388-byte `mermaid.js` that way rather than inlining it. All of those are platform APIs, with no dependency on the bundled Markdown plugin, so a standalone plugin can use the same mechanism.

## What Changes

The work splits into two stages with deliberately different risk profiles, and the second is only worth doing because the first proves the server out.

**Stage 1 — assets by URL, page still delivered by `loadHTML`.**

- A new `HttpRequestHandler` serves the plugin's bundled preview assets under a `/mdaioPreview` path prefix, resolving its port from `BuiltInServerManager` and rejecting any request without a valid auth token.
- Theme CSS, highlight.js, and KaTeX move from inlined strings to `<link>`/`<script>` references at that server.
- Asset responses carry cache headers and are served from the plugin jar, so each is fetched once per IDE session rather than per refresh.
- This stage does **not** touch navigation handling: subresource loads do not pass through `onBeforeBrowse`, so the existing interceptor is unaffected.

**Stage 2 — the page itself delivered by `loadURL`.**

- The generated document is served by the same handler and the browser navigates to it, instead of being pushed in as a string.
- The page then has a real `http://localhost:<port>` origin, which is what lets relative subresource URLs resolve — retiring the `file://` image rewriting for the live preview.
- **The navigation interceptor must be amended first.** `onBeforeBrowse` currently blocks every `http://` and `https://` URL and hands it to the system browser on a user gesture (`MarkdownPreviewFileEditor.kt:128-134`). Unchanged, it would block the preview's own page from loading. It must allow exactly the built-in server's origin and path prefix, and keep blocking everything else.
- Custom user CSS is served through the same handler rather than inlined, so a stylesheet the user edits is picked up without special-casing.

Throughout both stages: the `JBCefJSQuery` bridges for scroll sync and link interception, the scroll-restore cycle, and the preview's observable behaviour stay as they are.

### Non-goals

- **Incremental DOM updates.** Keeping the full-reload model is what makes this change tractable; making reloads cheap is the point, not eliminating them.
- **Exposing anything beyond the plugin's own assets and the current document.** The handler serves a fixed set of bundled resources plus the active preview document. It is not a file server for the project.
- **Changing HTML export.** Exported HTML must stay a self-contained file that works offline with no IDE running, so export keeps inlining. This change makes the two paths differ, which is a cost it has to carry explicitly.
- **Serving to anything but the local JCEF preview.** No remote access, no sharing a preview URL.
- **Replacing the hand-rolled converter** or changing the Markdown-to-HTML output.

## Capabilities

### New Capabilities

- `preview/asset-delivery`: how preview assets and the preview document are delivered to the browser, what is reachable over the server and what must not be, cache and authorisation behaviour, and how the preview degrades when the server is unavailable.

### Modified Capabilities

None — `openspec/specs/` holds no existing requirement for preview delivery.

## Impact

**Code**

- A new request handler, registered as `httpRequestHandler` in `META-INF/plugin.xml`, serving from the plugin jar.
- `preview/MarkdownHtmlConverter.kt` — `wrapInDocument` (`:441-489`) gains URL-referencing output; `loadResource` (`:491`) stops being the delivery path for the live preview but stays for export.
- `preview/MarkdownPreviewFileEditor.kt` — `updatePreview` (`:305-358`), the navigation interceptor (`:113-139`), and the image-path rewriting at `:329-338`.
- `export/HtmlExporter` — must keep inlining; the shared `wrapInDocument` therefore needs an explicit delivery mode rather than one hard-coded behaviour.

**Platform APIs**

`org.jetbrains.ide.HttpRequestHandler` (`isSupported`/`process`), `org.jetbrains.ide.BuiltInServerManager` (`getInstance`, `getPort`, `addAuthToken`), `org.jetbrains.io.FileResponses`, Netty's `FullHttpRequest`/`QueryStringDecoder` as the handler's request types, and the existing `com.intellij.ui.jcef` surface (`JBCefBrowser.loadURL`, `CefRequestHandler.onBeforeBrowse`). The `httpRequestHandler` extension point must be declared `dynamic="true"`.

**Versions in scope**

PhpStorm 2025.1 through 2026.2. The built-in server is long-standing platform surface, but the auth-token behaviour and the handler contract need checking at both ends of the range.

**Security**

This adds a locally-reachable HTTP endpoint, which is new attack surface for a plugin that currently makes and accepts no network connections. The built-in server binds to loopback and the token guards the endpoint, but the handler must additionally refuse any path that escapes its prefix — the spec states this as a requirement rather than leaving it to review.

**Users**

No visible behaviour change intended; the gain is responsiveness while typing. The risk is the opposite: if the server or the token is unavailable the preview must still work, which is why the spec requires a fallback to inlining rather than an error.

**Relationship to other changes**

`add-mermaid-diagram-preview` depends on Stage 1 — its 5.24 MB runtime is the reason this change is worth making now. `fix-preview-scroll-sync-drift` touches the same `updatePreview` path and the same injected JS; whichever lands second will need rebasing, and sequencing them is cheaper than reconciling them.
