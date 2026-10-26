# Design — Recovering orphaned Markdown color keys

## Context

See `proposal.md` — Why. The facts that constrain the design:

- The rename is a mechanical 1:1 prefix across all 22 keys: `MARKDOWN_X` → `MDAIO_MARKDOWN_X` (commit `9f9162f`). `MarkdownHighlightingColors` already lists all 22, so the legacy mapping is derivable from one place and needs no hand-maintained table of pairs.
- `TextAttributesKey` external names are a flat, IDE-wide namespace, and PhpStorm's bundled JetBrains Markdown plugin registers a subset of the *unprefixed* names. That is what forced the rename, and it is also what makes a silent migration unsafe: a legacy-named value found in a scheme may be the user's customisation, or it may be the bundled plugin's own default.
- The plugin ships explicit defaults for all 22 new keys through `<additionalTextAttributes>` for the `Default` and `Darcula` schemes (`plugin.xml:124-127`). Any key therefore always resolves to *something*, which rules out the otherwise-elegant approach of giving each new key its legacy key as a fallback — an explicit default always wins over a fallback, so the fallback would never be consulted.
- The plugin currently registers no startup activity.

## Goals / Non-Goals

**Goals**

- Recover what is recoverable, without ever changing a color the user did not ask us to change.
- One code path shared by the notification offer and the manual action, so they cannot drift.
- Ask at most once, and never on a fresh install.
- Keep the startup cost of the check negligible.

**Non-Goals (design level)**

- Distinguishing *perfectly* between a user's customisation and a bundled-plugin default. The design makes the distinction as precisely as the platform allows and then puts the decision in front of the user, which is what makes residual imprecision harmless.
- Cleaning up the orphaned legacy entries.
- Any general-purpose settings-migration framework. This is one rename, once.

## Decisions

### 1. Derive the legacy mapping from the existing key list

The pairing is computed as `externalName.removePrefix("MDAIO_")` over the keys already declared in `MarkdownHighlightingColors`, exposed as an ordered list of `(legacyName, currentKey)` pairs. No second list to keep in sync; a key added or renamed in future cannot silently fall out of the migration.

A unit test asserts that every key in `MarkdownHighlightingColors` carries the `MDAIO_` prefix and that the derived legacy name is exactly the pre-rename name, pinned against the 22 names taken from the commit before `9f9162f`.

### 2. Read a scheme's *own* values, not its resolved values — and treat the method as the thing to verify first

`EditorColorsScheme.getAttributes(key)` resolves through defaults and fallbacks, so it answers "what colour is this?" and not "did anyone set this?". What the detection needs is the latter: values the scheme itself defines. `com.intellij.openapi.editor.colors.impl.AbstractColorsScheme` exposes directly-defined attributes for this purpose, and `TextAttributesKey.find(legacyName)` addresses an unregistered name without registering a new semantic key.

**This is the open uncertainty of the whole change**, and the first task is to settle it empirically in a running IDE rather than to reason about it: does a scheme retain an entry whose key is no longer registered by any plugin, and does "directly defined" include values contributed as defaults through `additionalTextAttributes`? The answer sets how precise detection can be. It does not change the approach — the import is user-confirmed either way — but it does decide how confidently the offer can be worded, and whether a scheme-by-scheme preview is needed.

*Fallback if the platform API proves unusable:* read the user's scheme files from the config directory directly, where a user's own modifications are written and plugin-contributed defaults are not. That is a more precise signal but a more fragile mechanism, so it is the second choice, not the first.

### 3. Offer via notification, execute through one action

A `ProjectActivity` runs the detection off the EDT on first project open, and on finding anything posts a notification from a dedicated group carrying a single action. That action is the same `AnAction` registered for manual invocation, so the offer and the on-demand path execute identical code. Both extension points are declared `dynamic="true"`.

*Why a notification rather than a modal:* a modal dialog at startup over something the user may not care about is the wrong trade. A notification is dismissible, non-blocking, and re-findable in the event log.

### 4. A single persisted flag, with "offered" and not "migrated" semantics

`MarkdownSettings.State` gains `var legacyColorsOfferShown: Boolean = false`, set when the notification is posted — not when the import succeeds. The offer is a once-only interruption; the *import* stays available for ever through the action, so the flag does not need to track whether it ran. Shown-at-most-once is then true whether the user accepted, declined, or ignored.

An existing persisted state deserialises the new field to `false`, which is what upgrading users need; a fresh install has no legacy entries to find, so detection returns nothing and the flag's value never matters.

### 5. Skip-not-overwrite is decided per key, per scheme

For each `(legacyName, currentKey)` pair in each scheme: if the scheme has no directly-defined value for `currentKey` and does have one for `legacyName`, copy it; otherwise skip and count it. Copying uses `setAttributes` with a clone of the legacy `TextAttributes`, so the two entries do not end up sharing a mutable instance.

The whole import for all schemes is computed first and applied second, so the result report is accurate and a failure mid-way can be reported against a known plan.

### 6. Repaint rather than restart

After writing, re-set the global scheme through `EditorColorsManager` so the platform fires its scheme-changed notification and open editors repaint. This is the same path the colour settings page uses when the user clicks Apply, which is why it needs no restart.

## Risks / Trade-offs

- **A legacy value may be the bundled plugin's default rather than the user's choice** → The import is user-confirmed and names its scope; and it only ever fills keys with no current value, so the worst case is that a user who never customised anything accepts an offer and gets the bundled plugin's defaults — visibly, reversibly, and only because they said yes. Resetting a scheme's Markdown section restores our shipped defaults.
- **The platform API for directly-defined attributes is semi-internal and could differ across 2025.1–2026.2** → Task 1 verifies it at both ends of the range before anything is built on it, and Decision 2 records the fallback.
- **Writing to a scheme marks it modified** → Expected for a user-requested import; the spec states it, and the result report tells the user which schemes were touched.
- **A startup activity is new surface in this plugin** → Keep it to a detection that bails out immediately once the flag is set, run off the EDT, with no classloading beyond the colour manager.
- **Users have already lost colors in the field** → This recovers them only while the orphaned entries survive in the scheme. A user who has since reset their Markdown colours has nothing to recover; the offer correctly will not appear, and the on-demand action will say there is nothing to import.

## Migration Plan

Ships as a SemVer **minor** bump — new user-facing action, new notification, new setting field. No forward migration of its own.

Rollback is a revert: the flag is additive and an older build ignores it, and any import already performed stays in the user's scheme, which is where they want it.

## Open Questions

- Whether the offer should list the affected schemes individually or only count them. Decided at implementation time from what Task 1 learns about detection precision; it changes wording, not behaviour or specs.
