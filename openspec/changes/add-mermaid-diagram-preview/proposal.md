# Render Mermaid diagrams in the live preview

## Why

**Symptom:** a fenced block tagged ```` ```mermaid ```` renders in the live preview as a plain, syntax-highlighted code block — the diagram source, not a diagram. `KNOWN-ISSUES.md` describes diagram rendering as "not tested and may not render properly"; the accurate statement is that there is no diagram support at all.

**Root cause:** `MarkdownHtmlConverter.convert` treats every fenced block identically (`MarkdownHtmlConverter.kt:48-62`), emitting `<pre><code class="language-$lang">` and leaving the rest to highlight.js. Nothing in `src/` references Mermaid, PlantUML, or Graphviz. `wrapInDocument` inlines bundled highlight.js and KaTeX assets conditionally, but has no diagram branch.

**Fix:** detect `mermaid` (and the `{mermaid}` / `mermaid {...}` info-string forms) on a fenced block, emit the source into a `<pre class="mermaid">` container instead of a highlighted code block, and inline a bundled Mermaid runtime that renders those containers after load — gated on the document actually containing a Mermaid block, exactly as KaTeX is gated on `mathEnabled` and highlight.js on `hasCodeBlocks`.

Mermaid is the format that matters here: it is the de facto standard for diagrams in Markdown, is rendered natively by GitHub and GitLab, and is pure client-side JavaScript, so it works inside JCEF with no network access and no external binary.

## What Changes

- Fenced blocks whose info string names `mermaid` become diagram containers rather than code blocks.
- A bundled Mermaid runtime (`src/main/resources/preview/mermaid/mermaid.min.js`) is inlined into the preview document only when the rendered body contains at least one Mermaid container.
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

## Capabilities

### New Capabilities

- `preview/diagram-rendering`: how fenced diagram blocks are recognised, converted to preview containers, rendered by the bundled runtime, themed, and degraded when the source is invalid or the feature is disabled.

### Modified Capabilities

None — `openspec/specs/` is empty at the time of writing, so no existing requirement changes.

## Impact

**Code**

- `preview/MarkdownHtmlConverter.kt` — fence branch (`:48-62`), the EOF fence fallback (`:209-211`), and `wrapInDocument` (`:441-489`).
- `preview/MarkdownPreviewFileEditor.kt` — passes the new setting through to `wrapInDocument` alongside `mathEnabled` (`:342-347`).
- `settings/MarkdownSettings.kt`, `settings/MarkdownSettingsConfigurable.kt` — one new persisted boolean and one checkbox.
- `export/` — inherits the change through the shared converter.

**Platform APIs**

`com.intellij.ui.jcef.JBCefBrowser.loadHTML` (the whole document is inlined as a string; `loadHTML`'s base URL does not reliably resolve local subresources, per the existing comment at `MarkdownPreviewFileEditor.kt:331`), `JBCefBrowserBase`, `com.intellij.ui.jcef` JCEF availability — which on 2026.2+ comes from the separately bundled JCEF plugin that `META-INF/jcef-support.xml` already declares. No new extension points.

**Dependencies**

One new bundled asset. Mermaid's minified UMD bundle is roughly an order of magnitude larger than KaTeX's 277 KB, which makes unconditional inlining unacceptable and conditional inlining a hard requirement; the exact size and the resulting first-render latency are measured in `design.md`. MIT licensed, so `LICENSE`/third-party attribution needs an entry.

**Versions in scope**

PhpStorm 2025.1 through 2026.2 — the full declared `sinceBuild..untilBuild` range. Where JCEF is unavailable the preview already degrades to the "Preview not available" panel and this change inherits that behaviour unchanged.

**Users**

Additive and on by default. No settings migration, no change to existing documents.
