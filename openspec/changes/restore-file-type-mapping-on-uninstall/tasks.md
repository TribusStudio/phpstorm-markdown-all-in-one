# Tasks — File-type association teardown

## 1. Settle whether the plugin unloads dynamically

- [ ] 1.1 In a running IDE (`./dev gradle runIde`), disable the plugin and record whether the platform unloads it or demands a restart. Everything below branches on this answer — do not start writing a teardown hook before it is known.
- [ ] 1.2 If a restart is demanded, capture the platform's stated reason — the IDE log names what prevented unloading — and identify the holder. Start with the JCEF browsers held by preview editors, the `editorFactoryListener`, the `editorActionHandler`s, and any `Alarm` or coroutine scope outliving its disposable, per the patterns in `docs/009-platform-compatibility-and-leaks.md`.
- [ ] 1.3 Reproduce the predicted symptom end to end: with the bundled Markdown plugin enabled, confirm our plugin takes `md` (and that `options/filetypes.xml` gains `<removed_mapping ext="md" approved="true" type="Markdown"/>`), then remove our plugin by whichever path 1.1 established, restart, and open a `.md` file. Record whether it opens as Markdown or as plain text.
- [ ] 1.4 Repeat 1.3 at both ends of the supported range, 2025.1 and 2026.2, since this is platform-internal behaviour.
- [ ] 1.5 Record all of the above in a new numbered doc, including the resolver ladder, the silent-approval rule, and the cleanup predicate as read from the platform, so the next person does not have to disassemble it again.

## 2. Fix unloadability, if that is what 1.1 found

- [ ] 2.1 Fix each holder identified in 1.2, following the disposable discipline `docs/009` already establishes for this codebase.
- [ ] 2.2 Confirm the plugin now unloads without a restart, and that the IDE log is clean across disable, re-enable, and update.
- [ ] 2.3 Verify nothing regressed in the lifecycles touched: open and close many preview editors, switch themes, toggle settings, and confirm no leaked browsers, timers, or listeners.
- [ ] 2.4 Re-run 1.3 and confirm `.md` now opens as Markdown after removal.
- [ ] 2.5 If the cause turns out to be large enough to be its own piece of work, stop and split it into a separate change, and take the fallback in section 3 for this one.

## 3. Fallback teardown, only if section 2 cannot guarantee unloading

- [ ] 3.1 Implement teardown on the plugin's disposal, removing removed-mapping entries by exactly the platform's predicate: file type name equal to ours, or matcher among our file type's current associations. Nothing broader.
- [ ] 3.2 Make it best-effort: wrap in a failure guard, log the reason, and never let it block disable or uninstall.
- [ ] 3.3 Confirm it does not double-act or conflict when the platform's own cleanup also runs.

## 4. Regression test

- [ ] 4.1 Add a test that registers and then unregisters our file type through `FileTypeManager` and asserts no removed-mapping record survives that would suppress another plugin's claim on the extensions we had taken.
- [ ] 4.2 Derive the extension list in the test from our own file-type registration rather than hard-coding it, so adding an extension without accounting for it on teardown fails.
- [ ] 4.3 Add a test asserting the narrowness required by the spec: an unrelated user reassignment and another plugin's recorded conflict both survive teardown untouched.

## 5. Verify the user-visible outcome

- [ ] 5.1 Walk every scenario in `specs/lifecycle/file-type-association-teardown/spec.md`.
- [ ] 5.2 Confirm removal leaves `.md` working with the bundled Markdown plugin enabled, and that removal with no other Markdown plugin installed raises no error.
- [ ] 5.3 Confirm an update in place preserves our association, across a restart if one is required.
- [ ] 5.4 Confirm remove-then-reinstall restores our editor without manual file-type configuration.
- [ ] 5.5 Confirm re-enabling the bundled Markdown plugin — including as a side effect of enabling the bundled Mermaid plugin — leaves `.md` with our editor.
- [ ] 5.6 Confirm a user's deliberate reassignment of `md` away from our file type is not overridden on removal.
- [ ] 5.7 Confirm no prompt or notification is shown by the plugin during disable, uninstall, or update, and no error attributable to it appears in the log.

## 6. Ship

- [ ] 6.1 Run `./dev test` and fix any regressions.
- [ ] 6.2 Add a `KNOWN-ISSUES.md` entry for users already in the broken state: the symptom, and the manual fix via **Settings → Editor → File Types** re-associating `*.md`. Write it so someone searching "md files opening as plain text" finds it.
- [ ] 6.3 Cross-reference the new doc from `docs/009-platform-compatibility-and-leaks.md`, since this is the same class of lifecycle problem.
- [ ] 6.4 Bump the version with `./scripts/bump-version.sh` — patch if this landed as verification and documentation, minor if unloadability work shipped — and add the `CHANGELOG.md` entry.
- [ ] 6.5 Update `ROADMAP.md` per the project's phase conventions.
- [ ] 6.6 Commit on a `fix/file-type-teardown` branch using Conventional Commits, and open a PR whose test plan is section 5.
