## Purpose

Defines how Markdown syntax colors that were orphaned when the plugin's color keys were renamed are detected and recovered: when the user is offered the recovery, what the import copies, what it must never overwrite, and what the user is told about the outcome.

## ADDED Requirements

### Requirement: Orphaned customisations are detected

The plugin SHALL detect editor color schemes that still carry Markdown color values stored under the plugin's pre-rename key names. Detection SHALL cover every color scheme available to the user, not only the active one, and SHALL cover all of the plugin's renamed keys.

#### Scenario: Scheme carrying legacy values

- **WHEN** a color scheme holds values stored under the pre-rename names
- **THEN** those values are detected and reported as recoverable

#### Scenario: Several schemes

- **WHEN** two different schemes each carry legacy values
- **THEN** both are detected, and each scheme's values are accounted for separately

#### Scenario: No legacy values anywhere

- **WHEN** no scheme carries any legacy value
- **THEN** nothing is detected and the user is not prompted

#### Scenario: Detection is cheap and non-blocking

- **WHEN** the detection runs at IDE startup
- **THEN** it does not block the UI and does not measurably delay startup

### Requirement: Recovery is offered, never imposed

The plugin SHALL NOT change any color value without the user's explicit consent. Where orphaned values are detected, the plugin SHALL offer recovery and wait for the user to accept.

#### Scenario: Offer on first run after upgrade

- **WHEN** orphaned values are detected for the first time
- **THEN** the user is shown an offer to import them
- **AND** no color value has changed at the moment the offer appears

#### Scenario: Offer states its scope

- **WHEN** the offer is shown
- **THEN** it states how many values can be recovered and which schemes are affected

#### Scenario: Declining changes nothing

- **WHEN** the user dismisses or declines the offer
- **THEN** no color value is changed
- **AND** the offer is not shown again

#### Scenario: Offer is made at most once

- **WHEN** the offer has already been shown, whether accepted or declined, and the IDE is restarted
- **THEN** the offer is not shown again

#### Scenario: Fresh install

- **WHEN** the plugin is installed for the first time on an IDE with no prior Markdown color customisation
- **THEN** no offer is shown

### Requirement: Recovery is available on demand

The plugin SHALL provide an action that performs the same recovery at any time, so that a user who dismissed the offer, or who upgrades across several versions, can still recover their colors. The action SHALL be discoverable by name from the IDE's action search.

#### Scenario: Running the action after dismissing the offer

- **WHEN** the user dismissed the offer and later invokes the action
- **THEN** the recovery runs and the orphaned values are imported

#### Scenario: Running the action with nothing to recover

- **WHEN** the user invokes the action and no orphaned values exist
- **THEN** the user is told that there is nothing to import
- **AND** no color value is changed

### Requirement: Import copies each legacy value to its current key

On the user's acceptance, each detected legacy value SHALL be copied to the key that replaced it, preserving the stored attributes — foreground, background, font style, and effects — exactly.

#### Scenario: A customised color is recovered

- **WHEN** a scheme holds a customised bold color under the legacy name and the user accepts the import
- **THEN** after the import, Markdown bold text in the editor renders in that color

#### Scenario: Font style and effects are preserved

- **WHEN** a legacy value specifies a font style and an effect in addition to colors
- **THEN** the imported value carries the same font style and effect

#### Scenario: Every renamed key is covered

- **WHEN** a scheme holds a legacy value for each of the plugin's renamed keys
- **THEN** all of them are imported

#### Scenario: Import applies per scheme

- **WHEN** two schemes carry different legacy values for the same key
- **THEN** each scheme receives its own value, and no value crosses between schemes

### Requirement: Existing customisations are never overwritten

The import SHALL NOT overwrite a value the user has already set under a current key name. Where both a legacy value and a current customisation exist for the same key in the same scheme, the current one SHALL win and the legacy one SHALL be skipped.

#### Scenario: User already re-applied a color

- **WHEN** a scheme holds a legacy value for a key and the user has already customised the same key under its current name, and the user accepts the import
- **THEN** the current customisation is left untouched
- **AND** that key is reported as skipped

#### Scenario: Partial re-application

- **WHEN** the user has re-applied some of their colors but not others
- **THEN** the import fills in only the keys with no current customisation

### Requirement: The result is reported

After an import, the plugin SHALL tell the user what happened: how many values were copied, which schemes were affected, and how many keys were skipped because a current value already existed. A failure SHALL be reported rather than silently swallowed.

#### Scenario: Successful import

- **WHEN** an import copies values across one or more schemes
- **THEN** the user is shown the number of values copied and the schemes affected

#### Scenario: Import with skips

- **WHEN** an import skips keys that already had current values
- **THEN** the number of skipped keys is included in the report

#### Scenario: Import fails

- **WHEN** the import cannot complete — for example a scheme cannot be written
- **THEN** the user is told it failed and which scheme was involved
- **AND** no partial state is left that would cause the offer to be considered complete

### Requirement: Imported colors take effect without a restart

Colors recovered by an import SHALL apply to open editors without requiring an IDE restart, and SHALL persist across restarts.

#### Scenario: Open editors update

- **WHEN** the import completes and a Markdown file is already open
- **THEN** that editor repaints with the recovered colors

#### Scenario: Colors persist

- **WHEN** the IDE is restarted after an import
- **THEN** the recovered colors are still in effect

#### Scenario: Visible in settings

- **WHEN** the user opens the Markdown colour settings page after an import
- **THEN** the recovered values are shown as the current values for their keys

### Requirement: Unrecognised and unrelated entries are left alone

The import SHALL touch only the plugin's own renamed color keys. It SHALL NOT modify, remove, or reinterpret any other entry in the user's schemes, including the legacy entries themselves, which other plugins may still be using.

#### Scenario: Other plugins' colors untouched

- **WHEN** an import runs on a scheme that also customises colors belonging to other languages and plugins
- **THEN** none of those values are changed

#### Scenario: Legacy entries are left in place

- **WHEN** an import completes
- **THEN** the legacy entries remain in the scheme, unmodified
