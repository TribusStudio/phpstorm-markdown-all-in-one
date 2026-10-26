## Purpose

Defines what the user gets when a language is injected into a Markdown fenced block's content in our editor, which text range that injection covers, and how the editor behaves when the plugin providing that language is unavailable or the feature is switched off.

## ADDED Requirements

### Requirement: Diagram fence content gets language support when available

Where the Mermaid language is available in the IDE, the content of a fenced block recognised as a diagram fence SHALL be treated as Mermaid source, so that the language's editor features apply inside it.

#### Scenario: Highlighting inside a fence

- **WHEN** the Mermaid language is available and the user opens a document containing a diagram fence
- **THEN** the fence's content is syntax-highlighted as Mermaid rather than shown as plain text

#### Scenario: Completion inside a fence

- **WHEN** the caret is inside a diagram fence and the user invokes code completion
- **THEN** Mermaid completions are offered

#### Scenario: Structural editing inside a fence

- **WHEN** the caret is inside a diagram fence
- **THEN** brace matching, commenting, and folding behave as they do when editing a standalone Mermaid file

#### Scenario: Inspections inside a fence

- **WHEN** a diagram fence contains source the Mermaid language reports a problem in
- **THEN** that problem is reported in our editor, at the corresponding position inside the fence

#### Scenario: Recognition matches the preview

- **WHEN** a fence's info string is one the preview recognises as a diagram
- **THEN** the editor injects the language into it
- **AND** there is no info string the preview treats as a diagram but the editor does not, or vice versa

### Requirement: Only the fence body is injected

Injection SHALL cover the fenced block's content and nothing else. Fence markers, the info string, and surrounding Markdown SHALL remain Markdown.

#### Scenario: Markers are not injected

- **WHEN** the caret is on a diagram fence's opening or closing marker line
- **THEN** that line is treated as Markdown, not as Mermaid source

#### Scenario: Surrounding Markdown is unaffected

- **WHEN** a document contains a diagram fence between paragraphs
- **THEN** the paragraphs are highlighted and edited as Markdown exactly as before

#### Scenario: Other fences are unaffected

- **WHEN** a document contains fences for other languages alongside a diagram fence
- **THEN** those fences behave exactly as they did before this change

#### Scenario: Unterminated fence

- **WHEN** a diagram fence is not closed before the end of the document
- **THEN** the editor does not treat the remainder of the document as Mermaid beyond the fence's content
- **AND** no error is raised

### Requirement: Absence of the providing plugin changes nothing

Where the Mermaid language is not available — not installed, disabled, or failing to load — the editor SHALL behave exactly as it did before this change. The plugin's own functionality SHALL be unaffected, and the user SHALL NOT be told anything about it.

#### Scenario: Plugin not available

- **WHEN** the Mermaid language is unavailable and the user opens a document containing a diagram fence
- **THEN** the fence content renders as plain fenced text
- **AND** every other plugin feature works normally

#### Scenario: No error or log noise

- **WHEN** the Mermaid language is unavailable
- **THEN** no error, warning, or notification is produced on opening a Markdown file
- **AND** nothing is written to the IDE log about the missing language

#### Scenario: No prompting

- **WHEN** the Mermaid language is unavailable and the user opens a document containing a diagram fence
- **THEN** the user is not prompted, nudged, or notified to install or enable anything
- **AND** no plugin is enabled or installed on the user's behaviour

#### Scenario: Plugin disabled after being used

- **WHEN** the user disables the Mermaid plugin and restarts
- **THEN** diagram fences revert to plain fenced text with no error

#### Scenario: Plugin enabled later

- **WHEN** the user enables the Mermaid plugin and restarts
- **THEN** diagram fences gain language support without any further action

### Requirement: Preview rendering is independent of injection

Diagram rendering in the preview SHALL NOT depend on the Mermaid language being available, and injection SHALL NOT depend on diagram rendering being enabled. The two features SHALL be independently switchable.

#### Scenario: Preview without the language

- **WHEN** the Mermaid language is unavailable and diagram rendering is enabled
- **THEN** diagrams still render in the preview

#### Scenario: Injection without preview rendering

- **WHEN** diagram rendering is disabled and the Mermaid language is available
- **THEN** fence content still gets language support in the editor

### Requirement: Injection is user-controllable

The plugin SHALL expose a persisted setting controlling fence injection, defaulting to enabled, and SHALL present it only where the providing language is actually available.

#### Scenario: Setting disabled

- **WHEN** the user turns injection off
- **THEN** diagram fences are treated as plain fenced text
- **AND** no Mermaid inspections are reported inside them

#### Scenario: Setting hidden when irrelevant

- **WHEN** the Mermaid language is unavailable and the user opens the plugin's settings
- **THEN** the injection setting is not presented as an available option

#### Scenario: Setting persists

- **WHEN** the user changes the setting and restarts the IDE
- **THEN** the chosen value is still in effect

### Requirement: Injection does not degrade editing performance

Injection SHALL NOT make editing a Markdown document measurably slower, including in documents with many or large diagram fences.

#### Scenario: Document with many fences

- **WHEN** a document contains twenty diagram fences and the user types in prose elsewhere in it
- **THEN** typing latency is indistinguishable from the same document with injection disabled

#### Scenario: Large fence

- **WHEN** a diagram fence contains several hundred lines of Mermaid source
- **THEN** editing inside and around it remains responsive
