# Platform Compatibility and Lifecycle Leaks

Written for v0.23.0. Covers why the plugin broke on recent PhpStorm builds, why it degraded over long IDE sessions, and the rules that keep both from regressing.

## 1. Compatibility strategy

The plugin is **compiled against the oldest supported platform** (currently PhpStorm 2025.1 / build 251) and **verified against the newest** (2026.2 / build 262). This is deliberate:

- Compiling against the oldest target makes binary compatibility across the range a property of the build rather than something to remember. It is impossible to accidentally call an API that 2025.1 lacks, because the compiler would reject it.
- Compiling against the newest target and hoping would give the opposite guarantee: the code would build fine and fail at runtime for anyone on an older IDE.
- The Plugin Verifier closes the other direction, catching APIs that were removed between 251 and 262.

The knobs live in `gradle.properties`:

| Property | Meaning |
| --- | --- |
| `platformVersion` | What we compile against. The floor of the supported range. |
| `pluginSinceBuild` | Declared minimum IDE build. Must match `platformVersion`. |
| `pluginUntilBuild` | Declared maximum IDE build. |
| `verifyAgainstVersion` | The newest IDE the Plugin Verifier checks. Should track `pluginUntilBuild`. |

To support a newly released IDE, raise `pluginUntilBuild` and `verifyAgainstVersion` together, then run `./dev verify`. Leave `platformVersion` alone unless you are intentionally dropping support for old IDEs.

`untilBuild` is kept explicit rather than open-ended. Open-ended would spare a release per IDE bump, but nothing would catch an incompatible platform change before users did.

### Known deprecation

`DaemonCodeAnalyzer.restart()` is deprecated in favour of `restart(String reason)`, which does not exist in 2025.1. The call site in `MarkdownSettings.restartHighlighting()` carries a `@Suppress("DEPRECATION")` and a pointer back here. Switch to the overload when `sinceBuild` moves past 251.

## 2. Write access in editor action handlers

Since the 2024.2 threading model, the platform no longer grants editor action handlers implicit write access on the EDT. Any document mutation from an `EditorActionHandler` must be wrapped explicitly, or it throws:

```
Write access is allowed inside write-action only (see Application.runWriteAction());
If you access or modify model on EDT consider wrapping your code in WriteIntentReadAction
```

`AnAction.actionPerformed` implementations are unaffected — those already run inside a write command via `WriteCommandAction`. The gap was specifically in the Tab and Shift+Tab handlers, which are registered through the `editorActionHandler` extension point.

`util/EditorWriteUtil.kt` carries the two helpers this needs:

- `isWritable(editor)` — rejects viewers, consoles and read-only documents. Editor action handlers are global, so they run in every editor in the IDE, not just ours.
- `runWriteCommand(editor, dataContext, name, block)` — wraps the mutation so it satisfies the assertion *and* becomes undoable.

**Rule:** any document mutation outside an `AnAction` goes through `EditorWriteUtil.runWriteCommand`.

A second, quieter case: `SwingUtilities.invokeLater` runs with no read lock at all, so platform model access inside it (for example `scrollingModel.scrollTo`) can assert. Use `ApplicationManager.getApplication().invokeLater`, which runs under a write-intent read action.

## 3. Lifecycle and leaks

The long-session slowdown had one root cause and several amplifiers.

**Root cause.** `MarkdownPreviewFileEditor` registered its document listener as `document.addDocumentListener(listener)` — no disposable. Documents outlive editors (`FileDocumentManager` caches them), so the listener, the editor and the JCEF browser behind it stayed reachable for the rest of the IDE session. Every markdown file ever opened kept re-rendering its preview on every keystroke, forever. Open twenty files over a working day and each keystroke drove twenty full markdown→HTML→`loadHTML` cycles.

**Amplifiers.**

- No debounce. Each render did a full conversion, image path resolution with filesystem `exists()` checks, a CSS resource read, and a complete JCEF page reload — per character.
- Hidden previews rendered anyway, including in editor-only layout and background tabs.
- Scroll sync allocated a fresh Swing `Timer` per scroll event. A started `Timer` is held by the shared `TimerQueue`, so each one kept the editor reachable until it fired.
- `FloatingToolbar` was constructed per editor and never disposed. Its popup and debounce timer survived the editor's close.

**The rules that came out of it:**

1. Every listener registered on an object that outlives the registrant (`Document`, `EditorFactory`, application services) takes a `Disposable` overload. `addDocumentListener(listener, disposable)`, not `addDocumentListener(listener)`.
2. Per-editor state is scoped to a disposable stored in the editor's user data and released in `EditorFactoryListener.editorReleased`. See `MarkdownFileEditorListener`.
3. Use `com.intellij.util.Alarm` parented to a disposable rather than `javax.swing.Timer`. Alarms are cancellable, reusable and die with their parent.
4. Expensive work driven by document changes is debounced and skipped when its output is not visible.

### The shared-lambda trap

This bit us during the fix and is worth stating plainly:

```kotlin
val scope = Disposable { }   // WRONG — one shared instance for the whole JVM
val scope = Disposer.newDisposable("MarkdownEditorScope")  // right
```

A non-capturing Kotlin SAM lambda compiles to a single cached instance. Using one as a per-editor disposable meant the first editor released marked that instance disposed, and every editor opened afterwards failed to register anything under it. The symptom was 31 test failures reading `Sorry but parent ... has already been disposed`.

## 4. TextAttributesKey namespacing

`TextAttributesKey` names are a single flat namespace shared by every plugin in the IDE. PhpStorm bundles JetBrains' own Markdown plugin, which registers `MARKDOWN_*` keys. This plugin used the same names.

The platform keeps the *first* registration and logs a `SEVERE` for each subsequent one with a different fallback. Which plugin won depended on load order, and the loser's fallback colors were silently discarded. Five keys collided: `MARKDOWN_CODE_BLOCK`, `MARKDOWN_CODE_FENCE`, `MARKDOWN_CODE_SPAN`, `MARKDOWN_IMAGE`, `MARKDOWN_LINK_TEXT`.

There was also an invisible dependency. The bundled plugin ships scheme entries for `MARKDOWN_BOLD` and `MARKDOWN_ITALIC` that supply the bold and italic font types. Because the key names matched, this plugin was picking those up for free — and would have lost them the moment a user disabled the bundled plugin, which is exactly what a standalone Markdown plugin's users are likely to do.

Both are fixed by namespacing the keys `MDAIO_MARKDOWN_*` and shipping our own `colorSchemes/MarkdownAllInOneDefault.xml` and `MarkdownAllInOneDarcula.xml`, registered via `<additionalTextAttributes>` in `plugin.xml`.

**Rule:** external `TextAttributesKey` names are prefixed `MDAIO_`. Anything that needs a concrete color or font type (as opposed to inheriting from a fallback key) needs an entry in both scheme files.

## Verification

```
./dev test      # 377 tests
./dev verify    # Plugin Verifier against 2025.1 and 2026.2
```

Both must be clean. `./dev verify` output should contain no `was already registered with the other fallback` lines.
