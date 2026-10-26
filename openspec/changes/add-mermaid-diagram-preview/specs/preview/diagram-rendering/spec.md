## Purpose

Defines how fenced diagram blocks in a Markdown document are turned into rendered diagrams in the live preview and in exported HTML, including how they are themed, how invalid diagram source is surfaced, and how the preview behaves when diagram rendering is unavailable or switched off.

## ADDED Requirements

### Requirement: Mermaid fenced blocks render as diagrams

The preview SHALL render a fenced code block whose info string names `mermaid` as a graphical diagram rather than as source text.

Recognition SHALL be case-insensitive and SHALL accept the info-string forms in common use: a bare language name, a brace-wrapped name, and a language name followed by additional attributes. Recognition SHALL apply to both backtick and tilde fences of any valid fence length.

#### Scenario: Backtick fence tagged mermaid

- **WHEN** a document contains a fenced block opened with three backticks and the info string `mermaid`, holding a valid flowchart definition
- **THEN** the preview displays the rendered flowchart
- **AND** the preview does not display the diagram's source text

#### Scenario: Info-string variants

- **WHEN** a fenced block's info string is `Mermaid`, `{mermaid}`, or `mermaid {theme=neutral}`
- **THEN** the block is recognised as a diagram and rendered

#### Scenario: Tilde fence and long fences

- **WHEN** a diagram block is opened with a tilde fence, or with four or more backticks
- **THEN** the block is recognised as a diagram and rendered

#### Scenario: Other languages are unaffected

- **WHEN** a fenced block's info string is any language other than `mermaid` — including `kotlin`, `plantuml`, `dot`, or an empty info string
- **THEN** the block renders as a code block exactly as it does today, with syntax highlighting where the language is supported

#### Scenario: Unterminated diagram fence at end of document

- **WHEN** a document ends while a `mermaid` fence is still open
- **THEN** the preview renders the partial content without raising an error and without losing any document content that follows in later edits

### Requirement: Multiple diagrams per document

The preview SHALL render every Mermaid block in a document independently, and a failure in one SHALL NOT prevent the others from rendering.

#### Scenario: Several diagrams in one document

- **WHEN** a document contains three Mermaid blocks
- **THEN** all three render as diagrams

#### Scenario: One invalid diagram among valid ones

- **WHEN** a document contains two valid Mermaid blocks and one with invalid syntax between them
- **THEN** the two valid blocks render as diagrams
- **AND** the invalid block renders as an error, per the error-reporting requirement
- **AND** the rest of the document's Markdown continues to render normally

### Requirement: Invalid diagram source is reported visibly

When diagram source cannot be parsed, the preview SHALL display a visually distinct error region in the diagram's place, containing both the parse error and the original diagram source. The preview SHALL NOT silently omit the block, blank the document, or leave an empty gap.

#### Scenario: Syntax error in diagram source

- **WHEN** a Mermaid block contains source that fails to parse
- **THEN** the preview shows an error region in that block's position
- **AND** the error region contains the diagram's original source text
- **AND** the error region contains the parser's message

#### Scenario: Error recovers on correction

- **WHEN** the user corrects the invalid source
- **THEN** the next preview update replaces the error region with the rendered diagram

### Requirement: Diagrams follow the preview theme

Rendered diagrams SHALL use a colour palette consistent with the active preview theme, so that a diagram in a dark preview is legible against a dark background and a diagram in a light preview is legible against a light background.

#### Scenario: Dark preview

- **WHEN** the preview is displaying with a dark theme
- **THEN** diagram text, strokes, and fills are rendered in a dark-appropriate palette and remain legible

#### Scenario: Theme switch

- **WHEN** the user switches the IDE between a light and a dark theme
- **THEN** the next preview update renders diagrams in the palette matching the new theme

### Requirement: Diagram rendering is user-controllable

The plugin SHALL expose a persisted setting controlling whether Mermaid blocks render as diagrams, defaulting to enabled. When the setting is disabled, Mermaid blocks SHALL render as code blocks with the same appearance as any other fenced block.

#### Scenario: Setting disabled

- **WHEN** diagram rendering is turned off in settings and a document contains a Mermaid block
- **THEN** the block renders as a syntax-highlighted code block showing the diagram source
- **AND** no diagram runtime is loaded into the preview document

#### Scenario: Setting re-enabled

- **WHEN** the user turns diagram rendering back on
- **THEN** open previews render Mermaid blocks as diagrams without requiring an IDE restart

#### Scenario: Setting persists

- **WHEN** the user changes the setting and restarts the IDE
- **THEN** the chosen value is still in effect

### Requirement: Diagram support does not degrade documents without diagrams

A document containing no Mermaid block SHALL NOT pay any cost for diagram support: the preview document generated for it SHALL NOT include the diagram runtime, and its render time SHALL be unchanged.

#### Scenario: Document with no diagrams

- **WHEN** a document containing headings, paragraphs, and non-diagram code blocks is previewed
- **THEN** the generated preview document contains no diagram runtime

#### Scenario: First diagram added to a document

- **WHEN** the user adds the first Mermaid block to such a document
- **THEN** the following preview update includes the runtime and renders the diagram

### Requirement: Diagrams preserve scroll synchronisation

A rendered diagram SHALL carry the same source-line association as the fenced block it replaces, so that scroll synchronisation between editor and preview continues to work across documents containing diagrams.

#### Scenario: Scrolling to a diagram

- **WHEN** scroll sync is enabled and the user moves the editor caret to the first line of a diagram's fence
- **THEN** the preview scrolls to that diagram

#### Scenario: Scrolling past a diagram

- **WHEN** the user scrolls the preview past a rendered diagram to content below it
- **THEN** the editor scrolls to a position within that following content, not to the diagram's fence line

### Requirement: Diagrams render offline and in exported HTML

Diagram rendering SHALL require no network access and no external executable: everything needed SHALL be present in the plugin distribution. HTML exported from a document containing diagrams SHALL render those diagrams when opened in a standard browser with no network connection.

#### Scenario: Offline rendering in the preview

- **WHEN** the IDE has no network connectivity and a document containing a diagram is previewed
- **THEN** the diagram renders

#### Scenario: Exported HTML opened offline

- **WHEN** a document containing a diagram is exported to HTML and the exported file is opened in a browser with no network connection
- **THEN** the diagram renders in the browser

#### Scenario: No outbound requests

- **WHEN** a diagram is rendered
- **THEN** no request is made to any external host

### Requirement: Missing preview backend degrades safely

Where the preview backend is unavailable, the presence of diagrams in a document SHALL NOT change the existing degradation behaviour: the editor SHALL continue to open and SHALL present the existing "preview not available" state rather than failing.

#### Scenario: Preview backend unavailable

- **WHEN** the preview backend cannot be initialised and the user opens a document containing diagrams
- **THEN** the editor opens with the existing "preview not available" panel
- **AND** no error dialog is shown and the IDE remains responsive
