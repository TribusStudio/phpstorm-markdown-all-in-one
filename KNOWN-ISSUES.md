# Known Issues

A list of known problems we are actively tracking and looking to fix.

## Preview Scroll Synchronization

~~Bidirectional scrolling from the preview window back to the editor is not as synchronized as editor-to-preview scrolling.~~ **Improved in v0.16.1** — preview-to-editor scroll now uses linear interpolation between source-line elements for much better accuracy. Minor discrepancies may still occur on very long blocks without intermediate annotations.

## Mermaid and Diagram Rendering

Mermaid diagrams, PlantUML, and other graphing/charting extensions embedded in markdown have not been tested and may not render properly in the live preview.

## Color Scheme Customizations Reset in v0.23.0

Syntax highlighting colors customized under **Settings > Editor > Color Scheme > Markdown** reset to defaults when upgrading to v0.23.0. The underlying color keys were renamed from `MARKDOWN_*` to `MDAIO_MARKDOWN_*` to stop them colliding with PhpStorm's bundled Markdown plugin, which was silently discarding our color definitions and logging errors at every IDE start. Re-applying customizations is a one-time step. See [docs/009-platform-compatibility-and-leaks.md](docs/009-platform-compatibility-and-leaks.md).

## Found Something Else?

If you encounter any other issues, please open an issue in the [GitHub issues queue](https://github.com/TribusStudio/phpstorm-markdown-all-in-one/issues) to let us know what needs fixing!
