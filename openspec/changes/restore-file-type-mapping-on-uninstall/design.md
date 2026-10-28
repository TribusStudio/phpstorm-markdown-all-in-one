# Design — File-type association teardown

## Context

See `proposal.md` — Why. The platform mechanics, read from the 2026.2 build (`PS-262.10315.130`) with `javap`:

- **Conflict resolution.** `ConflictingFileTypeMappingTracker.resolveConflict` normalises the two candidates with a swap and returns the second as the winner. The ladder: non-bundled beats bundled (offsets 47–96); among bundled, non-core beats core, where `isCorePlugin` is `pluginId == PluginManagerCore.CORE_ID`; a `NativeFileType` loser takes a simpler message and is auto-approved; otherwise a vendor tiebreak where non-JetBrains beats JetBrains (`PluginManagerCore.isVendorJetBrains`).
- **Silent approval.** `approved = oldPlugin.isBundled()` (187–193), and `warnAndResolveConflict` skips the notification block when `approved` is true (90–95). A third-party plugin taking a pattern from a bundled one therefore produces no balloon and no user decision.
- **Persistence.** The loss is recorded by `RemovedMappingTracker` in `options/filetypes.xml` as `<removed_mapping ext="…" approved="true" type="…"/>`, keyed by **file type name**, with no reference to the plugin that won.
- **The platform's own cleanup.** `FileTypeManagerImpl.unregisterFileTypeWithoutNotification` calls `RemovedMappingTracker.removeIf` with this predicate (`lambda$unregisterFileTypeWithoutNotification$0`):

  ```
  mapping.getFileTypeName().equals(unregisteredType.getName())
    || currentAssociationsOfUnregisteredType.contains(mapping.getFileNameMatcher())
  ```

  The second clause is the one that saves us: `md` and `markdown` are associations of `MarkdownAIO`, so the `type="Markdown"` entries are dropped when `MarkdownAIO` is unregistered.
- **The suppression path.** `registerFileTypeWithoutNotification` consults `RemovedMappingTracker.getMappingsForFileType(name)` and withholds those patterns. So a surviving `removed_mapping` is what suppresses the bundled plugin's claim at the next startup.
- `fileType` is declared `dynamic="true"`, so the extension point itself supports unregistration.

What is unknown — and what this change exists to settle — is whether *this plugin* unloads dynamically. That is a whole-plugin property. This one registers a `fileEditorProvider` holding JCEF browsers, an `editorFactoryListener`, two `editorActionHandler`s, and 52 actions, and `docs/009-platform-compatibility-and-leaks.md` documents previously-fixed leaks of precisely the blocking kind (a `FloatingToolbar` never disposed, a cached SAM lambda misused as a per-editor disposable).

## Goals / Non-Goals

**Goals**

- Make the platform's existing cleanup path the one that actually runs.
- Keep any cleanup of our own strictly narrower than "tidy up file types".
- Leave the outcome under test, so the removal path cannot silently rot.

**Non-Goals (design level)**

- Interfering with conflict resolution while both plugins are installed.
- Any teardown that runs on a path other than our own unregistration.
- Repairing machines already in the broken state from code.

## Decisions

### 1. Establish unloadability first; it determines whether there is any code to write

The first task is a measurement, not an implementation: disable the plugin in a running IDE and observe whether the platform unloads it or demands a restart. Everything downstream branches on the answer, so writing a teardown hook before knowing it risks building a mechanism for a path that already works — and a teardown hook that duplicates a cleanup the platform already performs is worse than nothing, because it is a second thing to keep correct.

If the plugin already unloads, this change is a verification, a regression test, and a documentation entry. That is a legitimate and cheap outcome.

### 2. Prefer fixing unloadability over adding a teardown hook

If something keeps the plugin resident, fixing it is strictly better than working around it: it makes the platform's cleanup run, and it independently fixes disable-and-update-without-restart, which is the behaviour users expect of a modern plugin. A teardown hook would address only the file-type symptom of a broader lifecycle defect.

*Why this is likely tractable:* the plugin's registrations are all dynamic-capable extension points, and the known blocking pattern here is resource lifetime rather than static registration — the kind of defect `docs/009` already shows this codebase finding and fixing.

### 3. If a fallback is needed, mirror the platform's predicate exactly

Should unloadability prove unachievable, the teardown removes removed-mapping entries using the *same* rule the platform uses — entries whose file type name is ours, or whose matcher is among our file type's current associations — and nothing else.

*Why mirror rather than invent:* the predicate is the platform's definition of "residue our registration caused". Any broader rule risks deleting a user's own reassignment, which the spec forbids; any narrower rule misses the case that matters. Copying it also means that if the platform's own path does run, the two agree instead of fighting.

*Where it runs:* on our plugin's disposal, which is the only point we control that implies "we are going away". This is explicitly best-effort: it cannot run when the IDE is killed, and the spec requires that a failure never blocks removal.

### 4. The regression test asserts residue, not editor behaviour

End-to-end removal cannot be driven from a unit test — it needs a real IDE lifecycle. What *is* testable is the invariant underneath: after our file type is unregistered, no record remains that would suppress another plugin's claim on the extensions we had taken. The test drives registration and unregistration through `FileTypeManager` and asserts the tracker's state.

The test derives the extension list from our own file-type registration rather than hard-coding it, so adding an extension without accounting for it on teardown fails the test. That is the specific rot this change is trying to prevent.

### 5. Users already broken get documentation, not detection

Once our file type is gone and our code is not running, a `removed_mapping` of `md` from `Markdown` is indistinguishable from a user who deliberately unmapped it. Guessing would override a deliberate choice, which the spec forbids. So `KNOWN-ISSUES.md` carries the symptom and the one-line manual fix — **Settings → Editor → File Types**, re-associate `*.md` — which is what actually helps someone searching for why their Markdown stopped working.

## Risks / Trade-offs

- **The whole premise may not reproduce** — the plugin may already unload cleanly, in which case the predicted symptom never occurs → Decision 1 makes that the first thing established rather than the last, and a negative result still leaves a regression test guarding the invariant.
- **Depends on platform internals** (`RemovedMappingTracker`, the resolver's ladder) read from one build → Only the fallback depends on them at runtime; the verification and the test depend on observable behaviour. Both ends of the supported range are checked.
- **A teardown hook could delete a user's deliberate reassignment** → Mirror the platform's predicate exactly (Decision 3), and the spec has an explicit scenario for the deliberate-unmapping case.
- **Chasing dynamic unloadability can sprawl**, since any resource held anywhere blocks it → Scope it by outcome: the goal is that removal leaves `.md` working, not a general unloadability project. If the cause turns out to be large, the fallback in Decision 3 is the bounded answer and the unloadability work becomes its own change.
- **Fixing unloadability changes disable/update behaviour** from restart-required to live, which touches more than file types → That is a genuine behaviour change and wants its own verification pass over preview editors, listeners, and toolbars; it is also the point of doing it.
- **The conflict state is reachable again after install** — enabling the bundled Mermaid plugin re-enables the bundled Markdown plugin → Our mapping holds by the ladder's first tier, and the spec covers re-enabling as a scenario.

## Migration Plan

No migration. A SemVer **patch** if it turns out to be verification and documentation only; **minor** if unloadability work lands, since disable-without-restart is a user-visible improvement worth a CHANGELOG line.

Rollback is a revert. The fallback teardown, if it exists, is additive and only narrows what is left behind.

## Open Questions

- Whether to report, in the IDE log at startup, that our file type took a pattern from another plugin — a breadcrumb for diagnosing exactly this class of report later. Cheap and additive, but it is diagnostics rather than behaviour, so it does not change the specs or the approach and can be decided during implementation.
