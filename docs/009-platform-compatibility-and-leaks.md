# Platform Compatibility and Lifecycle Leaks

Written for v0.23.0. Covers why the plugin broke on recent PhpStorm builds, why it degraded over long IDE sessions, and the rules that keep both from regressing.

## 1. Compatibility strategy

> **Correction (v0.23.2).** The process described below was not sufficient, and 2026.2 support has been withdrawn. See §5.

The plugin is **compiled against the oldest supported platform** (currently PhpStorm 2025.1 / build 251) and **verified against the newest**. This is deliberate:

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

To support a newly released IDE, raise `pluginUntilBuild` and `verifyAgainstVersion` together, run `./dev verify`, **and then actually launch that IDE** (`./dev gradle runIde`) and open a markdown file. The verifier alone is not enough — see §5. Leave `platformVersion` alone unless you are intentionally dropping support for old IDEs.

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

## 5. What the Plugin Verifier does not check

v0.23.0 raised `untilBuild` to `262.*` on the strength of a clean Plugin Verifier run against PhpStorm 2026.2. Users on 2026.2 then hit an IDE hang when opening a markdown file: the EDT blocks in `FileEditorManagerImpl.blockingWaitForCompositeFileOpen` waiting for the editor composite to finish building, and never returns. 2026.2 support was withdrawn in v0.23.2.

**The Plugin Verifier is a static binary-compatibility check.** It resolves the plugin's bytecode against an IDE's classes and reports missing or changed API. It never launches the IDE and never runs a line of plugin code. It therefore cannot detect:

- threading-model violations (work on the wrong thread, missing read/write actions)
- deadlocks and lifecycle problems, such as blocking during editor creation
- anything that depends on runtime ordering or platform behaviour rather than API shape

A "Compatible" verdict means *the plugin will link against this IDE*. It does not mean *the plugin works on this IDE*. Those are different claims, and only the first one is being tested.

**Rule:** a new platform version is not supported until someone has launched that exact IDE with the plugin installed and exercised the editor. `./dev gradle runIde` with `platformVersion` temporarily pointed at the new build is the minimum bar. A green verifier run is necessary, never sufficient.

## 6. Never bind shortcuts to actions from ActionManager

`AnAction.registerCustomShortcutSet` mutates the shortcut set of **the instance you call it on**. `ActionManager.getAction(id)` returns an application-wide singleton. Calling one on the other therefore rebinds the shortcut for the entire IDE, not for your component — and the platform notices, logging a stack trace per call:

```
This is likely not what you wanted to do. Consider setting shortcut in keymap
defaults, inheriting from other action using `use-shortcut-of` or wrapping with
ActionUtil.wrap(). Action: Move Line Up [Plugin: com.tribus.markdown-all-in-one]
```

`MarkdownFileEditorListener` did exactly this for 21 actions on every markdown editor created — 21 global mutations and 21 logged stack traces per `.md` file opened, all inside `MarkdownSplitEditorProvider.createEditor`.

**Rule:** bind shortcuts to a per-editor wrapper, never to the instance `ActionManager` hands you. `EditorScopedAction` in `MarkdownFileEditorListener` is that wrapper. It must keep implementing `MarkdownAction`: `MarkdownActionPromoter` identifies our actions by that marker when resolving conflicts against IDE builtins, so a wrapper without it would let Cmd+B fall through to Go To Declaration.

`MarkdownFileEditorListenerTest` pins this — it asserts the shared instances' shortcut sets are unchanged after opening markdown files, and fails if the registration goes back to the singleton.

## 7. JCEF moved out of the platform core in 2026.2

Through 2026.1, `com.intellij.ui.jcef.JBCefBrowser` shipped in `lib/app-client.jar` — platform core, always on every plugin's classpath. In 2026.2 it moved to `plugins/jcef-plugin/lib/modules/intellij.platform.ui.jcef.jar`, a separate bundled plugin with id `com.intellij.modules.jcef`. Its version string is architecture-suffixed (`262.10968.76-linux-arm64`), which is the likely reason for the split: the native Chromium bundle is large and per-OS/per-arch, so shipping it as its own module lets it be updated independently of the platform.

The consequence for us: a plugin that does not declare the dependency does not get the module on its classloader, and `JBCefBrowser` fails to resolve at runtime. That is what caused the 2026.2 lockup — `NoClassDefFoundError` out of `getComponent()`, a cancelled `EditorComposite model flow` coroutine, and an EDT stuck forever in `blockingWaitForCompositeFileOpen`.

The declaration is **optional**, not required:

```xml
<depends optional="true" config-file="jcef-support.xml">com.intellij.modules.jcef</depends>
```

`com.intellij.modules.jcef` does not exist before 2026.2. A required dependency would therefore stop the plugin loading on 2025.1 and 2026.1, where JCEF is in core and needs no declaration at all. Optional resolves where it exists and is skipped where it does not, which is the only form that spans 251–262.

### Catch Throwable around optional platform backends

The fallback for "JCEF isn't available" already existed and still didn't fire, because it caught `Exception`. A class that cannot be resolved raises `NoClassDefFoundError`, which extends `Error`. Anything guarding against a *missing* platform class must catch `Throwable`, or the guard is decorative.

This matters far more than it sounds: the exception escaped into `EditorComposite`, so instead of a missing preview the user got an IDE that could not open markdown files at all. **A failure in an optional feature must never be able to take out editor creation.**

**Rule:** any platform class that might not be present — anything outside `com.intellij.modules.platform` — gets both a declared (optional) dependency *and* a `Throwable` guard at the point of first use.
