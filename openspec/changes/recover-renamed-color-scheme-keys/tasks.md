# Tasks — Recovering orphaned Markdown color keys

## 1. Settle how a scheme's own values are read

- [ ] 1.1 In a running IDE (`./dev gradle runIde`), customise two or three Markdown colors on a build that still uses the pre-rename key names, then run the current build against that config and confirm the orphaned `MARKDOWN_*` entries are still present in the scheme and readable via `TextAttributesKey.find(legacyName)`.
- [ ] 1.2 Determine whether a scheme's directly-defined attributes include values contributed as defaults through `additionalTextAttributes`, by comparing a scheme the user has customised against one they have not.
- [ ] 1.3 Repeat 1.1 and 1.2 on both ends of the supported range, 2025.1 and 2026.2, since the API is semi-internal.
- [ ] 1.4 Record the findings in a new numbered doc, `docs/011-color-key-migration.md`, and state in it which detection mechanism the implementation uses. If the platform API proves unusable, switch to the config-file fallback in `design.md` Decision 2 and say so there before continuing.

## 2. Legacy key mapping

- [ ] 2.1 Add a function to `lang/highlighting/` that derives `(legacyName, currentKey)` pairs from the keys declared in `MarkdownHighlightingColors` by stripping the `MDAIO_` prefix.
- [ ] 2.2 Unit-test that all 22 keys carry the prefix and that the derived legacy names match exactly the 22 pre-rename names from the commit before `9f9162f`, so a future key rename cannot silently drop out of the migration.

## 3. Detection and import

- [ ] 3.1 Implement detection over `EditorColorsManager.getInstance().allSchemes`, producing, per scheme, the keys with an orphaned legacy value and no current value, and the keys skipped because a current value exists.
- [ ] 3.2 Implement the import as plan-then-apply: compute the full plan across all schemes, then write it, cloning each `TextAttributes` rather than sharing the instance.
- [ ] 3.3 Re-apply the global scheme afterwards so open editors repaint without a restart.
- [ ] 3.4 Handle a write failure by reporting the scheme involved and leaving the offer flag in a state that does not suppress a retry through the action.
- [ ] 3.5 Unit-test detection and planning against synthetic schemes: a scheme with legacy values only, one with current values only, one with both for the same key, one with a partial re-application, two schemes with different values for the same key, and a scheme with no Markdown entries at all.
- [ ] 3.6 Unit-test that foreground, background, font style, and effects all survive the copy.
- [ ] 3.7 Unit-test that entries outside the plugin's 22 keys are untouched, including the legacy entries themselves.

## 4. Entry points

- [ ] 4.1 Add the **Import Legacy Markdown Colors** action, registered in `plugin.xml`, invoking the shared import and showing the result report.
- [ ] 4.2 Add a notification group and a `ProjectActivity` that runs detection off the EDT, bails out immediately when `legacyColorsOfferShown` is set, and otherwise posts the offer with the action attached.
- [ ] 4.3 Add `var legacyColorsOfferShown: Boolean = false` to `MarkdownSettings.State`, set when the notification is posted.
- [ ] 4.4 Mark the new extension points `dynamic="true"`.
- [ ] 4.5 Add the notification, action, and result strings to the message bundle — no hard-coded user-visible text.
- [ ] 4.6 Test that a persisted state without the new field deserialises to `false`, and that once set the startup check does no further work.

## 5. Verify in a running IDE

- [ ] 5.1 Walk the upgrade path end to end: customise colors on a pre-rename build, upgrade, confirm the offer appears and states its scope, accept it, and confirm the colors come back in open editors with no restart.
- [ ] 5.2 Confirm declining changes nothing and that the offer does not reappear after a restart.
- [ ] 5.3 Confirm a fresh install with no customisation shows no offer.
- [ ] 5.4 Confirm the action reports "nothing to import" when there is nothing to import.
- [ ] 5.5 Confirm a key the user has already re-applied under the new name survives the import untouched and is reported as skipped.
- [ ] 5.6 Confirm the recovered values appear as current values on the Markdown colour settings page, and persist across a restart.
- [ ] 5.7 Confirm no measurable startup delay, and no error in the IDE log, on both 2025.1 and 2026.2.

## 6. Ship

- [ ] 6.1 Run `./dev test` and fix any regressions.
- [ ] 6.2 Rewrite the `KNOWN-ISSUES.md` entry: re-applying by hand is no longer the only option; point at the import and the action's name.
- [ ] 6.3 Update `docs/009-platform-compatibility-and-leaks.md` to cross-reference the recovery, and `README.md` if the action warrants a mention.
- [ ] 6.4 Bump the minor version with `./scripts/bump-version.sh minor` and add the `CHANGELOG.md` entry, written so a user who lost colors in v0.23.0 recognises their problem in it.
- [ ] 6.5 Update `ROADMAP.md` per the project's phase conventions.
- [ ] 6.6 Commit on a `fix/recover-renamed-color-keys` branch using Conventional Commits, and open a PR whose test plan is section 5.
