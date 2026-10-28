## Purpose

Defines the state the IDE's file-type associations must be left in when the plugin is disabled, uninstalled, or updated, so that removing the plugin never leaves Markdown files without a working editor, and so that cleaning up after ourselves never disturbs associations we did not create.

## ADDED Requirements

### Requirement: Removing the plugin leaves Markdown files working

After the plugin is disabled or uninstalled, Markdown files SHALL continue to open in a Markdown editor provided by whatever remains installed. They SHALL NOT be left associated with no file type, and SHALL NOT open as plain text while another installed and enabled plugin claims those extensions.

#### Scenario: Uninstall with the bundled Markdown plugin enabled

- **WHEN** the plugin is installed alongside an enabled bundled Markdown plugin, has taken the `md` association, and is then uninstalled
- **THEN** a `.md` file opens as Markdown in the bundled plugin's editor
- **AND** the file type shown for `.md` in the IDE's file type settings is the bundled plugin's Markdown type

#### Scenario: Disable with the bundled Markdown plugin enabled

- **WHEN** the same plugin is disabled rather than uninstalled
- **THEN** a `.md` file opens as Markdown

#### Scenario: Removal across a restart

- **WHEN** removal or disabling requires an IDE restart, and the IDE is restarted
- **THEN** a `.md` file opens as Markdown after that restart

#### Scenario: Every extension we claimed

- **WHEN** the plugin is removed
- **THEN** each extension it claimed is either associated with another installed plugin's file type, or left unassociated only where nothing else claims it
- **AND** no extension another enabled plugin claims is left unassociated

#### Scenario: Removal with no other Markdown plugin

- **WHEN** the plugin is removed while no other plugin claims `md`
- **THEN** `.md` files open without error, by whatever default the IDE applies to unclaimed extensions
- **AND** no error or warning is raised about a missing file type

### Requirement: Updating the plugin preserves the association

Updating the plugin SHALL NOT disturb its own file-type associations. After an update, Markdown files SHALL open in the plugin's editor exactly as before.

#### Scenario: Update in place

- **WHEN** the plugin is updated from one version to the next
- **THEN** `.md` files continue to open in the plugin's editor
- **AND** the association is not handed to another plugin's file type

#### Scenario: Update across a restart

- **WHEN** the update requires a restart and the IDE is restarted
- **THEN** `.md` files still open in the plugin's editor

### Requirement: Reinstalling recovers the association

A user who removes and later reinstalls the plugin SHALL end up with the plugin's editor handling Markdown files again, without manual file-type configuration.

#### Scenario: Remove then reinstall

- **WHEN** the plugin is removed and then reinstalled
- **THEN** `.md` files open in the plugin's editor

#### Scenario: Conflict is re-resolved after the bundled plugin returns

- **WHEN** the bundled Markdown plugin is re-enabled after having been disabled, while our plugin is installed
- **THEN** `.md` files continue to open in our plugin's editor

### Requirement: Cleanup touches only what the plugin caused

Where the plugin cleans up file-type state on teardown, it SHALL confine itself to associations displaced by its own file type. It SHALL NOT modify, remove, or restore any other association, including the user's own file-type customisations and conflicts between other plugins.

#### Scenario: User's own reassignments survive

- **WHEN** the user has manually reassigned unrelated extensions to other file types, and the plugin is removed
- **THEN** every one of those reassignments is unchanged

#### Scenario: Other plugins' conflicts survive

- **WHEN** two other plugins have an unresolved or resolved conflict over some pattern, and our plugin is removed
- **THEN** that conflict's recorded state is unchanged

#### Scenario: A user's deliberate unmapping of our extension survives

- **WHEN** the user has deliberately reassigned `md` away from the plugin's file type, and the plugin is then removed
- **THEN** the user's choice is not overridden

#### Scenario: Extensions never claimed are untouched

- **WHEN** the plugin is removed
- **THEN** associations for extensions it never registered are unchanged

### Requirement: Teardown is silent and safe

Teardown SHALL NOT interrupt the user, and SHALL NOT fail the disable, uninstall, or update it is part of.

#### Scenario: No prompts on removal

- **WHEN** the plugin is disabled or uninstalled
- **THEN** no dialog, balloon, or notification is shown by the plugin

#### Scenario: Teardown failure does not block removal

- **WHEN** teardown cannot complete for any reason
- **THEN** the disable or uninstall still completes
- **AND** the reason is recorded in the IDE log

#### Scenario: No errors in the log

- **WHEN** the plugin is disabled, uninstalled, and updated
- **THEN** no error or exception attributable to the plugin appears in the IDE log for any of those operations

### Requirement: The association state after teardown is asserted, not assumed

The project SHALL hold the post-teardown association state under automated test, so that a future change to the plugin's registrations, lifecycle, or unloadability cannot silently reintroduce a broken removal path.

#### Scenario: Regression test over conflict residue

- **WHEN** the test suite runs
- **THEN** it asserts that after the plugin's file type is unregistered, no record remains that would suppress another plugin's claim on the extensions it had taken

#### Scenario: Test covers each claimed extension

- **WHEN** the plugin's set of registered extensions changes
- **THEN** the test covers the new set, failing if an extension is claimed without being accounted for on teardown
