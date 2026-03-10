# 006 — Phase 9 Technical Spec: Scroll Sync & Toolbar Rewrite

## 9A — Editor-Preview Scroll Synchronization

### Architecture

Source-map based synchronization — the same technique used by VSCode's markdown preview.

```
┌──────────────────┐          ┌──────────────────┐
│  Editor (Swing)  │  ──→──→  │  Preview (JCEF)  │
│                  │          │                  │
│  VisibleArea     │  line N  │  JS: scrollTo    │
│  Listener        │  ──→──→  │  [data-source-   │
│                  │          │   line="N"]       │
│                  │  ←──←──  │                  │
│  scrollToLine(N) │  line N  │  Intersection    │
│                  │  ←──←──  │  Observer         │
└──────────────────┘          └──────────────────┘
```

### Step 1: Source-Line Annotations

Modify `MarkdownHtmlConverter.convert()` to emit `data-source-line` attributes on block-level elements.

**Annotated elements:**
- Headings (`<h1>`–`<h6>`)
- Paragraphs (`<p>`)
- Code blocks (`<pre>`)
- List openers (`<ul>`, `<ol>`)
- List items (`<li>`)
- Blockquotes (`<blockquote>`)
- Tables (`<table>`)
- Horizontal rules (`<hr>`)
- HTML comments (pass-through)

**Example output:**
```html
<h1 data-source-line="1" id="title">Title</h1>
<p data-source-line="3">Some text here.</p>
<pre data-source-line="5"><code class="language-kotlin">val x = 1</code></pre>
```

**Implementation:** The main `while` loop in `convert()` already tracks `i` (line index). Add `data-source-line="$i"` to each element's opening tag. This is a minimal change — just string interpolation in existing `html.append()` calls.

### Step 2: Editor → Preview Sync

Add a `VisibleAreaListener` to the editor (in `MarkdownSplitEditor`).

```kotlin
editor.scrollingModel.addVisibleAreaListener { e ->
    if (scrollingFromPreview) return@addVisibleAreaListener
    val topLine = editor.xyToLogicalPosition(Point(0, e.newRectangle.y)).line
    scrollEditorToPreview(topLine)
}
```

`scrollEditorToPreview(line)` executes JavaScript in the JCEF browser:

```javascript
(function() {
    const elements = document.querySelectorAll('[data-source-line]');
    let target = null;
    for (const el of elements) {
        const line = parseInt(el.getAttribute('data-source-line'));
        if (line >= TARGET_LINE) { target = el; break; }
        target = el; // keep last element before target
    }
    if (target) {
        target.scrollIntoView({ behavior: 'auto', block: 'start' });
    }
})();
```

### Step 3: Preview → Editor Sync

Use `JBCefJSQuery` to create a callback bridge from JavaScript to Kotlin.

**JavaScript side (injected into preview HTML):**
```javascript
const observer = new IntersectionObserver((entries) => {
    for (const entry of entries) {
        if (entry.isIntersecting) {
            const line = entry.target.getAttribute('data-source-line');
            if (line) {
                // Calls Kotlin callback via JBCefJSQuery
                window.__scrollCallback(line);
            }
        }
    }
}, { threshold: 0.5 });

document.querySelectorAll('[data-source-line]').forEach(el => observer.observe(el));
```

**Kotlin side:**
```kotlin
val jsQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)
jsQuery.addHandler { lineStr ->
    val line = lineStr.toIntOrNull() ?: return@addHandler null
    if (!scrollingFromEditor) {
        scrollingFromEditor = false // not needed, just for clarity
        scrollPreviewToEditor(line)
    }
    null
}
```

### Step 4: Feedback Loop Prevention

Two flags prevent recursive scroll events:

```kotlin
private var scrollingFromEditor = false
private var scrollingFromPreview = false

fun scrollEditorToPreview(line: Int) {
    scrollingFromEditor = true
    // ... execute JS scroll ...
    // Reset after a short delay
    EdtScheduler.schedule(150) { scrollingFromEditor = false }
}

fun scrollPreviewToEditor(line: Int) {
    scrollingFromPreview = true
    editor.scrollingModel.scrollTo(LogicalPosition(line, 0), ScrollType.MAKE_VISIBLE)
    EdtScheduler.schedule(150) { scrollingFromPreview = false }
}
```

### Step 5: Throttling

Scroll events fire rapidly. Throttle to ~50ms using a timer:

```kotlin
private var scrollTimer: Timer? = null

fun onEditorScroll(topLine: Int) {
    scrollTimer?.cancel()
    scrollTimer = Timer().apply {
        schedule(timerTask { scrollEditorToPreview(topLine) }, 50)
    }
}
```

### Settings

Add to `MarkdownSettings.State`:
```kotlin
var scrollSyncEnabled: Boolean = true
```

Add to settings UI (in the Preview group):
```kotlin
row {
    checkBox("Synchronize editor and preview scroll position")
        .bindSelected(state::scrollSyncEnabled)
}
```

---

## 9B — Toolbar Architecture Rewrite

### Problem

`EditorNotificationProvider` is designed for transient notification banners ("Outdated SDK", "File encoding mismatch"). It has:
- Lazy evaluation — the platform decides when to call `collectNotificationData`
- Recreation lifecycle — panels can be discarded and recreated at any time
- No guaranteed timing — may fire before or after the editor is fully visible

This causes:
- Visible delay before toolbar appears
- Toolbar disappearing during tab drag, window split, group operations
- Need for `ToolbarInitListener` and hierarchy listener workarounds

### Solution

Embed the toolbar directly in `MarkdownSplitEditor`.

```
Before:                              After:
┌─────────────────────────┐          ┌─────────────────────────┐
│ EditorNotificationPanel │          │ MarkdownSplitEditor     │
│ ┌─────────────────────┐ │          │ ┌─────────────────────┐ │
│ │ Toolbar (injected)  │ │          │ │ Toolbar (embedded)  │ │
│ └─────────────────────┘ │          │ ├─────────────────────┤ │
│ ┌─────────────────────┐ │          │ │ TextEditorWithPreview│ │
│ │ SplitEditor         │ │          │ │ ┌────────┬─────────┐│ │
│ │ ┌────────┬─────────┐│ │          │ │ │ Editor │ Preview ││ │
│ │ │ Editor │ Preview ││ │          │ │ └────────┴─────────┘│ │
│ │ └────────┴─────────┘│ │          │ └─────────────────────┘ │
│ └─────────────────────┘ │          └─────────────────────────┘
└─────────────────────────┘
```

### Implementation

**`MarkdownSplitEditor`** (rewrite):

```kotlin
class MarkdownSplitEditor(
    private val textEditorInstance: TextEditor,
    private val preview: MarkdownPreviewFileEditor
) : TextEditorWithPreview(
    textEditorInstance, preview,
    "Markdown Editor",
    Layout.SHOW_EDITOR_AND_PREVIEW
) {
    private val toolbarPanel: JPanel = createToolbar()
    private val wrapperPanel: JPanel = JPanel(BorderLayout()).apply {
        add(toolbarPanel, BorderLayout.NORTH)
        add(super.getComponent(), BorderLayout.CENTER)
    }

    override fun getComponent(): JComponent = wrapperPanel

    private fun createToolbar(): JPanel {
        // Move all toolbar creation logic from EditorToolbarProvider here
        // No lazy evaluation — constructed synchronously
        // Reads toolbarEnabled/displayMode from settings
    }
}
```

### Files to Delete
- `src/main/kotlin/com/tribus/markdown/toolbar/EditorToolbarProvider.kt`
- `src/main/kotlin/com/tribus/markdown/toolbar/ToolbarInitListener.kt`

### plugin.xml Changes
- Remove `<editorNotificationProvider>` for `EditorToolbarProvider`
- Remove `<projectListeners>` entry for `ToolbarInitListener`

### What Stays
- `FloatingToolbar.kt` — architecturally sound (selection listener, popup)
- `MarkdownContextMenuGroup.kt` — right-click menu (action group)
- Toolbar button styling (`createHoverButton`) — move to a shared utility or into `MarkdownSplitEditor`

### Settings Integration
The toolbar should listen for settings changes to hot-swap display mode and enabled state:

```kotlin
init {
    val listener = MarkdownSettings.ChangeListener { state ->
        toolbarPanel.isVisible = state.toolbarEnabled
        // Optionally rebuild buttons if displayMode changed
    }
    MarkdownSettings.getInstance().addChangeListener(listener)
}
```

---

## 9C — Marketplace Assets

See [005-marketplace-assets.md](005-marketplace-assets.md) for the full screenshot list, animated demo specs, and capture instructions.

### Summary

| Asset Type | Count | Location |
|------------|-------|----------|
| Static screenshots | 10 | `docs/screenshots/*.png` |
| Animated GIFs | 2 | `docs/screenshots/*.gif` |
| Marketplace uploads | 6 (top picks from above) | Uploaded to JetBrains Marketplace |

### Implementation Order

1. Implement scroll sync (9A) and toolbar rewrite (9B) first
2. Capture screenshots after both features are working (so scroll sync and instant toolbar are visible)
3. Create animated GIFs last (they showcase the final UX)

### README Updates
Add a "Screenshots" section near the top of README.md with 3-4 key images inline. Link to the full gallery in `docs/screenshots/`.
