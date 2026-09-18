# Known Issues

A list of known problems we are actively tracking and looking to fix.

## Preview Scroll Synchronization

~~Bidirectional scrolling from the preview window back to the editor is not as synchronized as editor-to-preview scrolling.~~ **Improved in v0.16.1** — preview-to-editor scroll now uses linear interpolation between source-line elements for much better accuracy. Minor discrepancies may still occur on very long blocks without intermediate annotations.

## Mermaid and Diagram Rendering

Mermaid diagrams, PlantUML, and other graphing/charting extensions embedded in markdown have not been tested and may not render properly in the live preview.

## Color Scheme Customizations Reset in v0.23.0

Syntax highlighting colors customized under **Settings > Editor > Color Scheme > Markdown** reset to defaults when upgrading to v0.23.0. The underlying color keys were renamed from `MARKDOWN_*` to `MDAIO_MARKDOWN_*` to stop them colliding with PhpStorm's bundled Markdown plugin, which was silently discarding our color definitions and logging errors at every IDE start. Re-applying customizations is a one-time step. See [docs/009-platform-compatibility-and-leaks.md](docs/009-platform-compatibility-and-leaks.md).

## PhpStorm 2026.2 — Resolved in v0.24.0

~~Opening a `.md` file on PhpStorm 2026.2 could hang the IDE.~~ **Fixed in v0.24.0.**

In 2026.2 JetBrains moved JCEF — which backs the live preview — out of the platform core into a separate bundled plugin. Without a declared dependency on it, `JBCefBrowser` failed to resolve, and the resulting error killed the editor composite and left the IDE waiting on it forever. v0.24.0 declares the dependency and makes a missing preview backend degrade to a "Preview not available" panel instead of taking out editor creation.

If you are on 2026.2: **update to v0.24.0 or later.** Versions 0.23.0–0.23.1 can hang; 0.23.2–0.23.4 deliberately refuse to load there. If an IDE is currently hanging, start PhpStorm in Safe Mode (or disable the plugin from another instance) and then update.

## Found Something Else?

If you encounter any other issues, please open an issue in the [GitHub issues queue](https://github.com/TribusStudio/phpstorm-markdown-all-in-one/issues) to let us know what needs fixing!
