# Tasks — Mermaid diagram rendering in the live preview

## 0. Prerequisite

- [ ] 0.1 Confirm `serve-preview-assets-over-http` Stage 1 has landed: bundled assets are served over the built-in server and referenced by URL. This change has no acceptable delivery mechanism without it — do not start by inlining 5 MB as an interim.

## 1. Bundle the runtime

- [ ] 1.1 Pin the current Mermaid release, download `dist/mermaid.min.js`, and commit it to `src/main/resources/preview/mermaid/mermaid.min.js` alongside the existing `katex/` and `highlight/` asset directories.
- [ ] 1.2 Record the pinned version and its upstream URL in a new numbered doc, `docs/010-mermaid-diagram-rendering.md`, and note the MIT licence in the project's third-party attribution.
- [ ] 1.3 Confirm `./dev build` packages the new resource into the plugin jar, and note the resulting change in plugin zip size.

## 2. Fence detection in the converter

- [ ] 2.1 Add a private `isDiagramFence(info: String): Boolean` helper to `MarkdownHtmlConverter` that lowercases the info string, strips surrounding braces, takes the first whitespace-separated token, and matches `mermaid` exactly.
- [ ] 2.2 In the fence-close branch (`MarkdownHtmlConverter.kt:58-62`), emit `<pre class="mermaid" data-source-line="N">` with HTML-escaped source when the fence is a diagram fence and diagram rendering is enabled; otherwise keep today's `<pre><code class="language-…">` output.
- [ ] 2.3 Apply the same branch to the unterminated-fence fallback at `:209-211`.
- [ ] 2.4 Thread a `diagramsEnabled` flag into `convert` so disabling the setting produces the code-block output — defaulting it so existing callers and tests compile unchanged.
- [ ] 2.5 Unit-test the helper against `mermaid`, `Mermaid`, `{mermaid}`, `mermaid {theme=neutral}`, `mermaidjs`, `plantuml`, `kotlin`, and the empty info string.
- [ ] 2.6 Add `before.md` / `after.md` fixtures under `src/test/testData/` for: a backtick diagram fence, a tilde fence, a four-backtick fence, a diagram fence nested inside an outer fence (must NOT be detected), a document with three diagrams, and an unterminated diagram fence at EOF.
- [ ] 2.7 Unit-test that the emitted container carries the fence's opening `data-source-line`, matching the `<pre>` it replaces.

## 3. Page bootstrap and document assembly

- [ ] 3.1 Add the Mermaid runtime to the asset server's served set, and extend the allow-list resolution test so a renamed asset fails a test rather than a user's preview.
- [ ] 3.2 Add a `diagramsEnabled: Boolean = false` parameter to `wrapInDocument` and emit the runtime's `<script src>` reference only when it is true **and** `bodyHtml` contains a `pre class="mermaid"` container.
- [ ] 3.2a Under the inlining fallback — built-in server unavailable — emit Mermaid blocks as highlighted code blocks rather than inlining 5 MB, per `design.md` Decision 2.
- [ ] 3.3 Write the bootstrap script: `mermaid.initialize({ startOnLoad: false, securityLevel: 'strict', theme: darkMode ? 'dark' : 'default' })`, then per-container `mermaid.render` in `try/catch`, swapping in the SVG on success.
- [ ] 3.4 On failure, replace the container with an error region carrying the parser message and the original source, styled distinctly; add the error styles to the preview CSS so they work on every bundled theme.
- [ ] 3.5 Unit-test `wrapInDocument`: runtime reference absent for a document with no diagram container, absent when `diagramsEnabled` is false, absent under the inlining fallback, present when the setting is on and a container exists, and the Mermaid theme string tracking `darkMode`.
- [ ] 3.6 Pass the setting through from `MarkdownPreviewFileEditor.updatePreview` (`:342-347`) next to `mathEnabled`, and through the `export` path so exported HTML carries containers and runtime.

## 4. Setting

- [ ] 4.1 Add `var diagramsEnabled: Boolean = true` to `MarkdownSettings.State`.
- [ ] 4.2 Add the **Render Mermaid diagrams** checkbox to `MarkdownSettingsConfigurable` with Kotlin UI DSL v2, bound with `.bindSelected(state::diagramsEnabled)`, next to the existing math row.
- [ ] 4.3 Test that a persisted state serialised without the new field deserialises to `true`, so upgrading users get the feature on by default.
- [ ] 4.4 Verify by hand that toggling the setting re-renders open previews without an IDE restart.

## 5. Verify in a running IDE

- [ ] 5.1 Via `./dev gradle runIde`, confirm: a flowchart, a sequence diagram, and a Gantt chart all render; an invalid diagram shows the error region while its neighbours still render; diagrams are legible in both a light and a dark preview theme.
- [ ] 5.2 Confirm scroll sync still tracks correctly in a document where a diagram sits between long prose sections, in both directions.
- [ ] 5.3 Measure preview-update latency while typing in a document containing a diagram, and confirm the runtime is fetched once for the session rather than per refresh.
- [ ] 5.3a Disable the built-in server and confirm Mermaid blocks degrade to highlighted code blocks, the rest of the preview renders fully styled, and nothing inlines the runtime.
- [ ] 5.4 Confirm diagrams render with the machine offline, and that the preview issues no outbound request.
- [ ] 5.5 Export a document with diagrams to HTML and confirm it renders in a browser with no network connection.
- [ ] 5.6 Smoke-test on both the minimum and maximum supported platform versions in the declared range (2025.1 and 2026.2), including the 2026.2 JCEF-as-bundled-plugin path.

## 6. Ship

- [ ] 6.1 Run the full suite with `./dev test` and fix any regressions.
- [ ] 6.2 Correct `KNOWN-ISSUES.md`: Mermaid is supported as of this version; state that PlantUML and Graphviz remain unsupported and why; and note that our rendering is independent of the bundled JetBrains Mermaid plugin, which users may reasonably expect to be what powers it.
- [ ] 6.3 Update `README.md` feature list and any in-plugin help text to mention Mermaid diagram rendering and the new setting.
- [ ] 6.4 Bump the minor version with `./scripts/bump-version.sh minor` and add the `CHANGELOG.md` section, noting the plugin-size increase.
- [ ] 6.5 Update `ROADMAP.md` per the project's phase conventions.
- [ ] 6.6 Commit on a `feature/mermaid-diagram-preview` branch using Conventional Commits, and open a PR with a test plan drawn from section 5.
