## Purpose

Defines the observable contract for bidirectional scroll synchronisation between the Markdown editor and its live preview: which position in each pane corresponds to which in the other, how that correspondence behaves inside and across long blocks, and what must never happen — dead zones, backwards jumps, feedback loops, or a moved caret.

## ADDED Requirements

### Requirement: The two panes agree on one reference point

Both directions of synchronisation SHALL use the top of the visible region as the reference point. When synchronisation moves one pane in response to the other, the source line at the top of the editor's viewport SHALL correspond to the content at the top of the preview's viewport, in both directions.

#### Scenario: Editor drives the preview

- **WHEN** the user scrolls the editor so that a given source line is the topmost visible line
- **THEN** the preview scrolls so that the content rendered from that line is at the top of the preview viewport

#### Scenario: Preview drives the editor

- **WHEN** the user scrolls the preview so that content rendered from a given source line is at the top of the preview viewport
- **THEN** the editor scrolls so that that source line is the topmost visible line

#### Scenario: Round trip is stable

- **WHEN** the user scrolls the editor to any position, and then does not touch either pane
- **THEN** neither pane moves again after the synchronisation settles
- **AND** repeating the same editor scroll produces the same preview position every time

### Requirement: The editor always responds to preview scrolling

Scrolling the preview SHALL move the editor continuously. The editor SHALL NOT remain stationary while the preview scrolls and then jump; in particular, synchronisation SHALL NOT be skipped merely because the mapped source line already happens to be visible somewhere in the editor viewport.

#### Scenario: Small preview scroll within the editor's current viewport

- **WHEN** the user scrolls the preview by a small amount that maps to a line already visible in the editor
- **THEN** the editor still scrolls, bringing that line to the top of its viewport

#### Scenario: Continuous drag

- **WHEN** the user drags the preview scrollbar slowly from top to bottom
- **THEN** the editor's scroll position advances throughout the drag
- **AND** the editor does not jump by more than roughly one viewport at any single step

### Requirement: Mapping is monotonic with no dead zones

The correspondence between source lines and preview scroll positions SHALL be monotonically non-decreasing: scrolling further down in either pane SHALL never move the other pane backwards. Every scrollable region of the preview SHALL map to some source line, and no region of either pane SHALL be unreachable.

#### Scenario: No backwards movement

- **WHEN** the user scrolls the preview downwards through a document containing headings, paragraphs, lists, tables, code blocks, blockquotes, and images
- **THEN** the editor's scroll position never decreases at any point of the scroll

#### Scenario: Reaching the document start

- **WHEN** either pane is scrolled to its top
- **THEN** the other pane is also at its top

#### Scenario: Reaching the document end

- **WHEN** either pane is scrolled to its bottom
- **THEN** the other pane is at, or scrolled as far as it can go toward, its bottom
- **AND** this holds when the document's final element is a long code block, a long table, or a blockquote

### Requirement: Long blocks synchronise internally

A block spanning many source lines — a fenced code block, a table, or a blockquote — SHALL synchronise across its own extent, not only at its first line. Scrolling within such a block SHALL move the other pane proportionally within the same block.

#### Scenario: Scrolling within a long code block

- **WHEN** a document contains a fenced code block of at least 100 lines and the user scrolls the preview from the block's first visible line to its last
- **THEN** the editor scrolls progressively through that code block's source lines
- **AND** the editor's position at the block's end is at or near the block's closing fence, not at its opening fence

#### Scenario: Scrolling within a long table

- **WHEN** a document contains a table of at least 50 rows and the user scrolls the editor through those rows
- **THEN** the preview scrolls progressively through the rendered table

#### Scenario: Scrolling within a long blockquote

- **WHEN** a document contains a blockquote spanning at least 40 source lines and the user scrolls either pane through it
- **THEN** the other pane scrolls progressively through the same blockquote

#### Scenario: Blockquote content is addressable

- **WHEN** the editor caret region is at a line in the middle of a long blockquote and the preview is synchronised to it
- **THEN** the preview shows that region of the blockquote, not the blockquote's opening line

### Requirement: No systematic offset

Synchronisation SHALL NOT exhibit a constant offset between panes. The correspondence SHALL hold regardless of the preview's page padding, margins, or user-supplied custom preview CSS.

#### Scenario: Default theme

- **WHEN** the editor is scrolled so a heading is the topmost visible line
- **THEN** that heading is at the top of the preview viewport, not offset below or above it by a fixed distance

#### Scenario: Custom preview CSS

- **WHEN** the user has configured custom preview CSS that changes the page's padding or margins
- **THEN** synchronisation remains correct, with no offset introduced by the custom styling

#### Scenario: Every bundled theme

- **WHEN** synchronisation is exercised under each bundled preview theme
- **THEN** the behaviour is the same under all of them

### Requirement: No feedback loops

Synchronisation SHALL NOT oscillate. A scroll driven by one pane SHALL NOT be echoed back as a scroll of the originating pane, regardless of how long the responding pane takes to settle or how fast the user scrolls.

#### Scenario: Slow responding pane

- **WHEN** one pane takes longer than usual to complete its scroll — for example in a very large document
- **THEN** the resulting scroll is not fed back to the originating pane
- **AND** neither pane oscillates

#### Scenario: Rapid continuous scrolling

- **WHEN** the user scrolls one pane rapidly and continuously for several seconds
- **THEN** the other pane follows without oscillating
- **AND** scroll events the user initiated are not discarded, leaving the panes out of sync when scrolling stops

### Requirement: Synchronisation never moves the caret

Synchronisation SHALL move the editor's viewport only. It SHALL NOT move the caret, alter the selection, or take focus from the pane the user is interacting with.

#### Scenario: Preview scrolled while caret is set

- **WHEN** the caret is at a known position and the user scrolls the preview
- **THEN** the editor viewport moves and the caret stays where it was
- **AND** any selection is unchanged

#### Scenario: Focus is not stolen

- **WHEN** the user is scrolling the preview
- **THEN** focus remains with the preview

### Requirement: Layout changes after load are accounted for

Where preview content changes height after the page's initial layout — images finishing loading, or deferred content rendering — synchronisation SHALL correct itself rather than continue against stale measurements.

#### Scenario: Images load after initial render

- **WHEN** a document containing several large images is previewed and the images finish loading after the page's first layout
- **THEN** synchronisation reflects the images' real heights
- **AND** scrolling to a line below the images lands correctly

### Requirement: Position survives a preview refresh

Editing a document SHALL NOT lose the reader's place. After the preview re-renders in response to a document change, the preview SHALL return to the position corresponding to the editor's current top visible line.

#### Scenario: Typing mid-document

- **WHEN** the user has scrolled to the middle of a long document and types a character
- **THEN** after the preview refreshes, the preview is still showing the same region
- **AND** the preview does not return to the top of the document

### Requirement: Disabling synchronisation stops it completely

When scroll synchronisation is disabled in settings, neither pane SHALL move in response to the other, and the preview SHALL NOT report its scroll position back to the IDE.

#### Scenario: Disabled

- **WHEN** scroll synchronisation is disabled and the user scrolls either pane
- **THEN** the other pane does not move

#### Scenario: Re-enabled

- **WHEN** the user re-enables scroll synchronisation
- **THEN** synchronisation resumes without requiring an IDE restart

#### Scenario: Position restore still works when disabled

- **WHEN** synchronisation is disabled and the user edits a document after scrolling to its middle
- **THEN** the preview still returns to its previous position after refreshing rather than jumping to the top
