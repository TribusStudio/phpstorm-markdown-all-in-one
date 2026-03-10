# Known Issues

A list of known problems we are actively tracking and looking to fix.

## Preview Scroll Synchronization

Bidirectional scrolling from the preview window back to the editor is not as synchronized as editor-to-preview scrolling. This is likely due to how the system polls the scroll position and the placement of source-line mapping attributes on block-level elements. Editor-to-preview scrolling works reliably.

## Mermaid and Diagram Rendering

Mermaid diagrams, PlantUML, and other graphing/charting extensions embedded in markdown have not been tested and may not render properly in the live preview.

## Found Something Else?

If you encounter any other issues, please open an issue in the [GitHub issues queue](https://github.com/TribusStudio/phpstorm-markdown-all-in-one/issues) to let us know what needs fixing!
