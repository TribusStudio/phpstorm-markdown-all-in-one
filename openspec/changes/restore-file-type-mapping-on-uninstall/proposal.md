# Leave `.md` working when the plugin is removed

## Why

**Symptom (predicted, not yet observed):** a user disables or uninstalls Markdown All-in-One and their `.md` files stop being Markdown — opening as plain text even though PhpStorm's bundled Markdown plugin is installed and enabled. Nothing in the IDE connects this to our plugin, because the cause was written to disk silently, possibly months earlier. It looks like the IDE broke, or like our plugin broke it on the way out.

**Root cause.** Installing our plugin alongside the bundled Markdown plugin triggers a file-type conflict, because both claim `md` and `markdown`:

- bundled: `<fileType name="Markdown" extensions="markdown;md" language="Markdown">`
- ours: `<fileType name="MarkdownAIO" extensions="md;markdown;mdown;mkd;mkdn" language="MarkdownAIO">`

`ConflictingFileTypeMappingTracker.resolveConflict` resolves this by a fixed ladder: a non-bundled plugin's file type takes the pattern from a bundled one; among bundled plugins a non-core one beats the core plugin; and a non-JetBrains vendor beats a JetBrains one. We are non-bundled, so we win at the first tier. It then sets `approved = oldPlugin.isBundled()`, and `warnAndResolveConflict` skips its notification entirely when `approved` is true — so **the reassignment happens silently**, with no balloon and no user decision. The loss is recorded in `options/filetypes.xml` against the *file type name*, not against our plugin:

```xml
<removed_mapping ext="markdown" approved="true" type="Markdown" />
<removed_mapping ext="md" approved="true" type="Markdown" />
```

The platform does have a cleanup for this. `FileTypeManagerImpl.unregisterFileTypeWithoutNotification` calls `RemovedMappingTracker.removeIf` with a predicate that drops any removed mapping whose file type name matches the type being unregistered **or whose matcher is currently associated with it**. That second clause is what matters: when `MarkdownAIO` is unregistered, `md` and `markdown` are among its associations, so the `type="Markdown"` entries are dropped and the bundled mapping is free to come back.

**The gap:** that cleanup only runs when the file type is actually *unregistered* — the dynamic-unload path. If our plugin cannot be unloaded dynamically, disabling or uninstalling it requires an IDE restart; on the next boot our extensions are simply never loaded, `unregisterFileTypeWithoutNotification` is never called, and the `removed_mapping` entries survive. `registerFileTypeWithoutNotification` then consults `getMappingsForFileType("Markdown")`, finds `md` and `markdown` recorded as approved removals, and suppresses them. `.md` ends up associated with nothing.

The `fileType` extension point is declared `dynamic="true"`, so nothing structural prevents the good path. But dynamic unloadability is a property of the *whole plugin*, and this one holds JCEF browsers, an `editorFactoryListener`, `editorActionHandler`s, and 52 actions — and `docs/009-platform-compatibility-and-leaks.md` records lifecycle leaks of exactly the kind that block unloading, found and fixed in this plugin before.

**Fix:** make the good path the guaranteed one. Establish whether the plugin unloads dynamically today, fix what prevents it if anything does, and verify the user-visible outcome — that removing the plugin leaves `.md` opening as Markdown. If dynamic unload cannot be guaranteed, clean up our own conflict residue explicitly rather than relying on a path that does not run.

## What Changes

- Determine whether the plugin is dynamically unloadable, and record the answer. This is the hinge: everything else follows from it.
- If something blocks unloading, fix it, which also brings the usual benefit of disable and update working without a restart.
- Verify the user-visible outcome directly, by walking the install → conflict → remove → reopen path and checking that `.md` is Markdown at the end, with the bundled plugin both enabled and disabled.
- If dynamic unload cannot be guaranteed, add an explicit teardown that removes **only** the removed-mapping entries our own file type caused, matching the platform's own predicate rather than inventing a broader rule.
- Add a regression test over the conflict residue, so the mapping state after teardown is asserted rather than assumed.
- Document the interaction in `KNOWN-ISSUES.md` for anyone already in the broken state, with the manual recovery: **Settings → Editor → File Types**, re-associate `*.md` with Markdown. That costs one short section and is the only thing that helps a user who already hit this.

### Non-goals

- **Changing which plugin wins the conflict while both are installed.** Winning `.md` is correct and intended — it is how our editor opens Markdown files at all. This change is only about what happens on the way out.
- **Suppressing or intercepting the platform's conflict resolution**, or trying to make it notify the user. The silent auto-approval is platform behaviour for a bundled loser, and fighting it would be fragile and intrusive.
- **Touching removed mappings we did not cause.** A user's own file-type reassignments, and conflicts between other plugins, are none of our business — the teardown must be narrow enough that it cannot touch them.
- **Narrowing our registered extensions** to avoid the conflict. Dropping `md` would mean not opening Markdown files.
- **Fixing this for users already in the broken state, automatically.** We cannot distinguish "our leftover residue" from "the user deliberately unmapped `.md`" once our file type is gone and our code is not running. Documentation is the honest answer.

## Capabilities

### New Capabilities

- `lifecycle/file-type-association-teardown`: what the IDE's file-type associations must look like after the plugin is disabled, uninstalled, or updated, and what the plugin may and may not touch when cleaning up after itself.

### Modified Capabilities

None — `openspec/specs/` holds no existing requirement for plugin teardown.

## Impact

**Code**

- Possibly none, if the plugin already unloads dynamically — in which case this change is a verification, a regression test, and a `KNOWN-ISSUES.md` entry, and that is a legitimate outcome worth the effort of establishing.
- Otherwise: whatever holds the plugin resident. Likely candidates by construction are the JCEF browsers held by preview editors, the `editorFactoryListener`, and any `Alarm` or coroutine scope outliving its disposable — the same class of defect `docs/009` already documents.
- A teardown hook only in the fallback case.

**Platform APIs**

`com.intellij.openapi.fileTypes.FileTypeManager` and its `FileTypeManagerImpl`/`RemovedMappingTracker` internals for the fallback and for assertions, `com.intellij.ide.plugins.DynamicPlugins` for the unloadability check, and `Disposer` discipline across the preview, listener, and toolbar lifecycles. No new extension points unless the fallback is needed.

**Versions in scope**

PhpStorm 2025.1 through 2026.2. The conflict resolver's ladder and the cleanup predicate were read from the 2026.2 build (`PS-262.10315.130`); both ends of the range need checking, since this is platform internals and the fallback would depend on them.

**Users**

Affects only the uninstall and disable paths. No change while the plugin is installed. Users already in the broken state are served by documentation, not by code.

**Relationship to other changes**

Independent of the other five. It does interact with `add-optional-mermaid-language-support`: enabling the bundled Mermaid plugin also enables the bundled Markdown plugin, which re-registers the competing `Markdown` file type. Our mapping holds deterministically by the first tier of the ladder, so that is safe — but it means the conflict state is reachable again after an initial install, which this change's verification should cover.
