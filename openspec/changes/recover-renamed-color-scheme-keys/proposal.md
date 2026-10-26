# Recover Markdown colors lost to the v0.23.0 key rename

## Why

**Symptom:** a user who had customised colors under **Settings → Editor → Color Scheme → Markdown** found them silently reset to defaults on upgrading to v0.23.0, with no warning and no way to get them back short of redoing every one by hand. `KNOWN-ISSUES.md` documents this as a one-time step users must absorb. Anyone still upgrading from v0.22 or earlier hits it today.

**Root cause:** v0.23.0 renamed all 22 `TextAttributesKey` external names by adding an `MDAIO_` prefix — `MARKDOWN_BOLD` became `MDAIO_MARKDOWN_BOLD`, and so on for every key (commit `9f9162f`, `MarkdownHighlightingColors.kt:25-...`). The rename was necessary and correct: `TextAttributesKey` external names are a single flat namespace shared across the whole IDE, PhpStorm still bundles JetBrains' own Markdown plugin registering the same unprefixed names, and the collision meant whichever plugin loaded first won while the loser's definitions were silently discarded and a SEVERE error was logged at every IDE start.

What was missed is that a user's color customisations are stored in their editor scheme **against the external key name**. Renaming the key orphans every stored value: the old entries are still sitting in the user's scheme, now addressed by a name nothing reads, while the new keys fall through to the plugin's shipped defaults from `colorSchemes/MarkdownAllInOneDefault.xml` and `MarkdownAllInOneDarcula.xml`.

**Fix:** the orphaned values are still there, so offer to bring them across. On first run after upgrading, detect schemes still carrying legacy `MARKDOWN_*` entries and offer a one-click import that copies each legacy value onto its `MDAIO_`-prefixed counterpart. The mapping is a mechanical 1:1 prefix for all 22 keys, so nothing has to be guessed.

Critically, this is **offered, not applied**. A user who has already redone their colors must not have them overwritten, and — because the bundled JetBrains plugin contributes its own defaults under the very same legacy names — a silent migration risks copying the *bundled plugin's* defaults over ours for a user who never customised anything at all. Asking removes both hazards.

## What Changes

- A one-time check, run once per IDE installation after the plugin starts, looks for legacy `MARKDOWN_*` color entries in the user's editor color schemes.
- Where any are found, a notification offers to import them, naming how many values and which schemes are affected. Dismissing it is final; it never reappears.
- Accepting copies each legacy value onto its `MDAIO_MARKDOWN_*` counterpart, **skipping any key the user has already given an explicit value under the new name**, so re-applied customisations are never clobbered.
- The same import is also reachable permanently as an action, **Import Legacy Markdown Colors**, for users who dismissed the notification or who upgrade several versions at once; when it finds nothing to import it says so rather than failing silently.
- The import reports what it did: how many values were copied, per scheme, and which keys were skipped because a new value already existed.
- Nothing is changed for a fresh install, for a user who never customised Markdown colors, or for a user who has already upgraded and redone them.
- `KNOWN-ISSUES.md` is updated: the entry stops being "re-applying is a one-time step you must do by hand" and becomes a pointer to the import.

### Non-goals

- **Reverting the rename.** The `MDAIO_` prefix is the fix for a real collision with the bundled plugin and stays.
- **Registering the legacy keys as fallbacks** for the new ones. It would re-couple us to the bundled plugin's namespace, and because the plugin ships explicit defaults for all 22 keys via `additionalTextAttributes`, a fallback would never be consulted anyway.
- **Removing the orphaned legacy entries** from the user's schemes. They are inert; deleting entries we do not own from a shared scheme is not ours to do, and the bundled plugin may still be using some of those names.
- **Migrating anything other than these 22 color keys.** Not preview themes, not plugin settings, not editor settings.
- **Automatic, unattended migration.** See the Why — the ambiguity between a user's customisation and the bundled plugin's defaults is why this asks.

## Capabilities

### New Capabilities

- `highlighting/color-scheme-migration`: detecting colors orphaned by the key rename, offering their recovery, what the import copies and what it refuses to overwrite, and what the user is told about the result.

### Modified Capabilities

None — `openspec/specs/` holds no existing requirement for color-scheme handling.

## Impact

**Code**

- `lang/highlighting/` — a new legacy-name map alongside `MarkdownHighlightingColors`, which already holds all 22 keys and is the natural single source for the pairing.
- A new startup check and a new action, plus their registration in `META-INF/plugin.xml` and a notification group. The plugin currently declares no startup activity, so this adds the first one; it must do nothing but a cheap check on a background thread.
- `settings/MarkdownSettings.kt` — one persisted flag recording that the offer has been made, so it is made at most once.
- `messages/` — notification, action, and result strings.

**Platform APIs**

`com.intellij.openapi.editor.colors.EditorColorsManager` (`allSchemes`), `EditorColorsScheme.getAttributes`/`setAttributes`, `com.intellij.openapi.editor.colors.TextAttributesKey.find` for addressing the orphaned names, `com.intellij.openapi.editor.colors.impl.AbstractColorsScheme` for distinguishing a value a scheme defines itself from one it inherits, `com.intellij.openapi.startup.ProjectActivity` for the one-time check, `com.intellij.notification.NotificationGroupManager`, and `AnAction` for the manual entry point. The extension points added must be marked `dynamic="true"`.

Reading a value a scheme defines itself, as opposed to one contributed as a default, is the one genuinely uncertain piece of platform behaviour here; `design.md` records it as the question the first task settles.

**Versions in scope**

PhpStorm 2025.1 through 2026.2 — the full declared range. The legacy keys are the same in all of them, but the API used to read a scheme's own values must be checked at both ends of the range.

**Users**

Upgrade-path-only and strictly additive: without user consent no color changes. Writing to a scheme does mark it modified, which is visible in the settings UI — that is expected for an import the user asked for, and is called out in the spec.
