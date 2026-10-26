## Purpose

Defines how the live preview's stylesheets, scripts, and document reach the embedded browser: what is reachable over the local server and what must never be, how assets are cached and authorised, and how the preview behaves when the server cannot be used.

## ADDED Requirements

### Requirement: Preview assets are fetched once per session

Bundled preview assets SHALL be delivered by reference rather than embedded in the preview document, so that refreshing the preview does not re-transfer or re-parse them. A refresh caused by editing SHALL reuse the already-fetched assets.

#### Scenario: Repeated refreshes reuse assets

- **WHEN** the user types continuously in a document that uses syntax highlighting and math, causing many preview refreshes
- **THEN** each asset is fetched at most once for the session
- **AND** subsequent refreshes issue no further request for it

#### Scenario: Typing stays responsive

- **WHEN** the user types in a document large enough that asset re-delivery was previously noticeable
- **THEN** preview refresh latency is lower than before this change
- **AND** no refresh blocks the editor's UI thread

#### Scenario: Asset changes are picked up

- **WHEN** the plugin is updated to a version carrying different bundled assets
- **THEN** the new assets are served, and a stale cached copy from the previous version is not used

### Requirement: Only the plugin's own resources are reachable

The server endpoint SHALL expose exactly the plugin's bundled preview assets and the active preview document, and nothing else. It SHALL refuse any request whose path escapes its designated prefix, resolves outside the bundled resources, or names a resource not on its served set.

#### Scenario: Path traversal is refused

- **WHEN** a request is made to the endpoint with a path containing traversal segments intended to reach a file outside the bundled resources
- **THEN** the request is refused
- **AND** no file content is returned

#### Scenario: Absolute and encoded paths are refused

- **WHEN** a request names an absolute filesystem path, or an encoded form of one, or an encoded traversal sequence
- **THEN** the request is refused

#### Scenario: Unserved resources are refused

- **WHEN** a request names a resource that exists in the plugin but is not part of the preview asset set
- **THEN** the request is refused

#### Scenario: Project files are not reachable

- **WHEN** a request attempts to read a file from the open project through the endpoint
- **THEN** the request is refused

### Requirement: Requests are authorised

The endpoint SHALL serve only authorised requests. An unauthorised request SHALL be refused without disclosing whether the named resource exists.

#### Scenario: Unauthorised request

- **WHEN** a request for a valid asset path arrives without valid authorisation
- **THEN** it is refused
- **AND** the response does not reveal whether that asset exists

#### Scenario: The preview's own requests succeed

- **WHEN** the preview page requests its assets
- **THEN** those requests are authorised and succeed

### Requirement: The preview works when the server does not

Where the local server is unavailable, disabled, or its port cannot be resolved, the preview SHALL still render correctly by falling back to embedding assets in the document. The user SHALL NOT see an error, a broken page, or an unstyled page.

#### Scenario: Server unavailable

- **WHEN** the built-in server cannot be reached or its port cannot be resolved, and the user opens a Markdown file
- **THEN** the preview renders fully styled, with working syntax highlighting and math
- **AND** no error notification is shown

#### Scenario: Degradation is logged, not surfaced

- **WHEN** the fallback is used
- **THEN** the reason is recorded in the IDE log for diagnosis
- **AND** the user's preview is unaffected

#### Scenario: Missing preview backend is unchanged

- **WHEN** the preview backend itself cannot be initialised
- **THEN** the existing "preview not available" panel is shown, exactly as before this change

### Requirement: Exported HTML remains self-contained

HTML export SHALL continue to produce a single file that renders correctly with no IDE running and no network connection. Exported files SHALL NOT reference the local server.

#### Scenario: Export with highlighting and math

- **WHEN** a document using syntax highlighting and math is exported to HTML and opened in a browser on a machine with no IDE running and no network access
- **THEN** it renders fully, with highlighting and math intact

#### Scenario: No local references in export

- **WHEN** an exported file is inspected
- **THEN** it contains no reference to a local server address or port

### Requirement: Relative resources in the document resolve

Once the preview document is served rather than pushed in as a string, resources it references relative to the document SHALL resolve without rewriting. Images referenced relative to the Markdown file SHALL display.

#### Scenario: Relative image

- **WHEN** a document references an image by a path relative to the Markdown file
- **THEN** the image displays in the preview

#### Scenario: Image in a parent directory

- **WHEN** a document references an image above its own directory
- **THEN** the image displays

#### Scenario: Missing image

- **WHEN** a document references an image that does not exist
- **THEN** the preview renders the rest of the document normally and shows the broken image in place, without failing the page

### Requirement: Navigation remains restricted

The preview SHALL continue to refuse to navigate anywhere other than its own served document. A link the user clicks SHALL continue to open in the system browser rather than inside the preview, and the preview's own origin SHALL NOT become a channel for navigating elsewhere.

#### Scenario: External link clicked

- **WHEN** the user clicks an external link in the preview
- **THEN** it opens in the system browser
- **AND** the preview itself does not navigate

#### Scenario: Preview page loads

- **WHEN** the preview navigates to its own served document
- **THEN** that navigation is permitted

#### Scenario: Other local addresses are refused

- **WHEN** the preview is directed to a local address outside the plugin's own served prefix
- **THEN** the navigation is blocked

#### Scenario: Anchor links still work

- **WHEN** the user clicks an in-document anchor link
- **THEN** the preview scrolls to that heading without navigating away

### Requirement: Existing preview behaviour is preserved

This change SHALL be invisible in the preview's behaviour apart from responsiveness. Scroll synchronisation, link interception, position restore across refreshes, theme switching, and custom CSS SHALL all behave as they did before.

#### Scenario: Scroll sync unaffected

- **WHEN** scroll synchronisation is enabled and the user scrolls either pane
- **THEN** it behaves as it did before this change

#### Scenario: Position restore unaffected

- **WHEN** the user edits a document after scrolling to its middle
- **THEN** the preview returns to the same region after refreshing

#### Scenario: Theme switching unaffected

- **WHEN** the user changes the preview theme
- **THEN** the preview re-renders in the new theme

#### Scenario: Custom CSS applies

- **WHEN** the user has configured a custom preview stylesheet
- **THEN** it applies to the preview

#### Scenario: Custom CSS edits are picked up

- **WHEN** the user edits their custom stylesheet on disk and the preview refreshes
- **THEN** the updated styling applies rather than a cached copy
