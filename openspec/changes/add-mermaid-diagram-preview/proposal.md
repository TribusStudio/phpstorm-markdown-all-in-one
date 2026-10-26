# Render Mermaid diagrams in the live preview

## Why

**Symptom:** a fenced block tagged ```` ```mermaid ```` renders in the live preview as a plain, syntax-highlighted code block — the diagram source, not a diagram. `KNOWN-ISSUES.md` describes diagram rendering as "not tested and may not render properly"; the accurate statement is that there is no diagram support at all.

**Root cause:** `MarkdownHtmlConverter.convert` treats every fenced block identically (`MarkdownHtmlConverter.kt:48-62`), emitting `<pre><code class="language-$lang">` and leaving the rest to highlight.js. Nothing in `src/` references Mermaid, PlantUML, or Graphviz. `wrapInDocument` inlines bundled highlight.js and KaTeX assets conditionally, but has no diagram branch.

**Fix:** detect `mermaid` (and the `{mermaid}` / `mermaid {...}` info-string forms) on a fenced block, emit the source into a `<pre class="mermaid">` container instead of a highlighted code block, and have a bundled Mermaid runtime render those containers after load — delivered over the preview's asset server, like every other bundled asset.

**Note on the bundled JetBrains plugin.** PhpStorm bundles a Mermaid plugin (`com.intellij.mermaid`), but it cannot render diagrams in our preview. Its Markdown integration is a separate optional content module registering into the *bundled Markdown plugin's* extension namespace — `org.intellij.markdown.browserPreviewExtensionProvider`, `fenceGeneratingProvider`, `fenceLanguageProvider` — so it feeds only JetBrains' own preview. Nor can we depend on it: `com.intellij.mermaid` declares `<module name="intellij.markdown"/>`, a content module owned by `org.intellij.plugins.markdown`, so depending on it would transitively require the bundled Markdown plugin this project is built to work without. We bundle our own copy of the runtime. What we do take from it is its delivery technique — see `serve-preview-assets-over-http`.

Mermaid is the format that matters here: it is the de facto standard for diagrams in Markdown, is rendered natively by GitHub and GitLab, and is pure client-side JavaScript, so it works inside JCEF with no network access and no external binary.

## What Changes

- Fenced blocks whose info string names `mermaid` become diagram containers rather than code blocks.
- A bundled Mermaid runtime (`src/main/resources/preview/mermaid/mermaid.min.js`) is added to the preview asset server's served set and referenced by the preview document only when the rendered body contains at least one Mermaid container.
- Mermaid is initialised with `startOnLoad: false` and rendered explicitly, with its theme chosen from the preview's existing light/dark state so diagrams match the surrounding `PreviewTheme`.
- A diagram whose source fails to parse renders a visible, styled error box containing the Mermaid error and the original source, rather than vanishing or blanking the preview.
- A new setting, **Render Mermaid diagrams** (default on), mirroring the existing `mathEnabled` setting in `MarkdownSettings` and its Kotlin UI DSL v2 row in `MarkdownSettingsConfigurable`. With it off, Mermaid blocks fall back to today's highlighted-code rendering.
- Mermaid containers carry the same `data-source-line` annotation as the `<pre>` they replace, so scroll sync keeps working across diagrams.
- HTML export (`com.tribus.markdown.export`) reuses the same conversion path, so exported HTML contains diagram containers and the inlined runtime.
- `KNOWN-ISSUES.md` is corrected: Mermaid moves from "untested" to supported; PlantUML and Graphviz are stated as unsupported with the reason.

### Non-goals

- **PlantUML and Graphviz.** Both need something the plugin cannot assume: a remote render server (which would send document contents to a third party from a plugin that currently makes no network calls) or a local `plantuml.jar` plus a `dot` binary. Either is a separate change with its own privacy and dependency decisions.
- **Diagram export to SVG/PNG files**, interactive pan/zoom, and click handlers inside diagrams.
- **Editor-side support**: no completion, inspections, or injected-language highlighting for Mermaid source. This change is preview-only.
- **Replacing the hand-rolled Markdown converter.** The fence-detection change is local to the existing line-based loop.
- **Reusing or depending on the bundled `com.intellij.mermaid` plugin** for rendering, for its runtime asset, or for anything else. Editor-side reuse of its Mermaid *language*, which is safe because it is optional, is the separate `add-optional-mermaid-language-support`.

## Capabilities

### New Capabilities

- `preview/diagram-rendering`: how fenced diagram blocks are recognised, converted to preview containers, rendered by the bundled runtime, themed, and degraded when the source is invalid or the feature is disabled.

### Modified Capabilities

None — `openspec/specs/` is empty at the time of writing, so no existing requirement changes.

## Impact

**Code**

- `preview/MarkdownHtmlConverter.kt` — fence branch (`:48-62`), the EOF fence fallback (`:209-211`), and `wrapInDocument` (`:441-489`).
- The preview asset server's served set gains the Mermaid runtime.
- `preview/MarkdownPreviewFileEditor.kt` — passes the new setting through to `wrapInDocument` alongside `mathEnabled` (`:342-347`).
- `settings/MarkdownSettings.kt`, `settings/MarkdownSettingsConfigurable.kt` — one new persisted boolean and one checkbox.
- `export/` — inherits the change through the shared converter.

**Platform APIs**

`com.intellij.ui.jcef.JBCefBrowser`, `JBCefBrowserBase`, and `com.intellij.ui.jcef` JCEF availability — which on 2026.2+ comes from the separately bundled JCEF plugin that `META-INF/jcef-support.xml` already declares. Asset delivery itself is `serve-preview-assets-over-http`'s surface (`org.jetbrains.ide.HttpRequestHandler`, `BuiltInServerManager`), not this change's. No new extension points.

**Dependencies**

One new bundled asset, and a dependency on `serve-preview-assets-over-http` Stage 1, which this change is the main reason to do first. Mermaid's minified UMD bundle is roughly twenty times KaTeX's 277 KB: `mermaid@12.2.0` ships `dist/mermaid.min.js` at 5.24 MB, and JetBrains' bundled plugin independently ships `mermaid.js` at 5,120,388 bytes — so there is no small build to find. The 30 KB `mermaid.esm.min.mjs` is an ESM entry point that lazy-loads diagram chunks over the network and is not a substitute. MIT licensed, so `LICENSE`/third-party attribution needs an entry.

**Versions in scope**

PhpStorm 2025.1 through 2026.2 — the full declared `sinceBuild..untilBuild` range. Where JCEF is unavailable the preview already degrades to the "Preview not available" panel and this change inherits that behaviour unchanged.

**Users**

Additive and on by default. No settings migration, no change to existing documents.
