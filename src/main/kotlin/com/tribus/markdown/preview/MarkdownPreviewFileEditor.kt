package com.tribus.markdown.preview

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import com.tribus.markdown.export.HtmlExporter
import com.tribus.markdown.settings.MarkdownSettings
import java.io.File
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.SwingConstants

/**
 * A file editor that renders a live HTML preview of a markdown document
 * using JCEF (Chromium Embedded Framework).
 * Read-only — no editing, no keyboard shortcuts, just rendered output.
 */
class MarkdownPreviewFileEditor(
    private val file: VirtualFile,
    private val document: Document
) : UserDataHolderBase(), FileEditor {

    private var browser: JBCefBrowser? = null
    private var fallbackComponent: JLabel? = null
    private var zoomLevel: Double = 1.0
    private var currentTheme: PreviewTheme.Theme = PreviewTheme.Theme.AUTO
    private var settingsListener: MarkdownSettings.ChangeListener? = null

    // Scroll sync
    private var jsQuery: JBCefJSQuery? = null
    private var onScrollCallback: ((Int) -> Unit)? = null

    private val mainComponent: JComponent by lazy {
        try {
            val b = JBCefBrowser()
            browser = b

            // Set up scroll sync JS query bridge
            try {
                val query = JBCefJSQuery.create(b as JBCefBrowserBase)
                query.addHandler { lineStr ->
                    val line = lineStr.toIntOrNull()
                    if (line != null) onScrollCallback?.invoke(line)
                    null
                }
                jsQuery = query
            } catch (_: Exception) {
                // JBCefJSQuery not available
            }

            updatePreview()

            // Listen for document changes to live-refresh
            document.addDocumentListener(object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    updatePreview()
                }
            })

            // Listen for settings changes to hot-swap CSS theme
            settingsListener = MarkdownSettings.ChangeListener { updatePreview() }
            try {
                MarkdownSettings.getInstance().addChangeListener(settingsListener!!)
            } catch (_: Exception) {
                // No application context (tests)
            }

            b.component
        } catch (_: Exception) {
            // JCEF not available (headless, older IDE, etc.)
            val label = JLabel("Preview not available (JCEF not supported)", SwingConstants.CENTER)
            fallbackComponent = label
            label
        }
    }

    /**
     * Track the last known source line for scroll position restoration after preview updates.
     * Set by the scroll sync callback or by the split editor before triggering an update.
     */
    @Volatile
    var lastVisibleSourceLine: Int = -1

    fun updatePreview() {
        val settings = try { MarkdownSettings.getInstance() } catch (_: Exception) { null }
        val themeName = settings?.state?.previewTheme ?: "auto"
        currentTheme = PreviewTheme.Theme.fromName(themeName)
        val customCssPath = settings?.state?.previewCustomCssPath ?: ""

        val css = PreviewTheme.loadThemeCss(currentTheme)
        val customCss = PreviewTheme.loadCustomCss(customCssPath)
        val isDark = currentTheme == PreviewTheme.Theme.GITHUB_DARK ||
            currentTheme == PreviewTheme.Theme.VSCODE ||
            (currentTheme == PreviewTheme.Theme.AUTO && PreviewTheme.isIdeDarkTheme())
        var bodyHtml = MarkdownHtmlConverter.convert(document.text, annotateSourceLines = true)

        // Resolve relative image paths to absolute file:// URLs so they render
        // in the JCEF preview (loadHTML's baseUrl doesn't reliably resolve locals).
        val parentPath = file.parent?.path
        if (parentPath != null) {
            val baseDir = File(parentPath)
            val warnings = mutableListOf<String>()
            bodyHtml = HtmlExporter.resolveImagePaths(bodyHtml, baseDir, false, warnings)
        }

        // Capture the line to restore after the full-page reload
        val restoreLine = lastVisibleSourceLine

        val scrollJs = buildScrollSyncJs()
        val fullHtml = MarkdownHtmlConverter.wrapInDocument(bodyHtml, css, customCss, isDark, scrollJs)
        browser?.loadHTML(fullHtml)

        // After loadHTML, the page reloads asynchronously. Schedule a scroll restore
        // once the DOM is ready. We use a short delay to allow the JCEF page to load.
        if (restoreLine >= 0) {
            javax.swing.Timer(150) {
                scrollToSourceLine(restoreLine)
            }.apply {
                isRepeats = false
                start()
            }
        }
    }

    /**
     * Execute JavaScript to scroll the preview to the element matching [line].
     */
    fun scrollToSourceLine(line: Int) {
        browser?.cefBrowser?.executeJavaScript(
            "if(window.__scrollToSourceLine)window.__scrollToSourceLine($line);",
            "", 0
        )
    }

    /**
     * Register a callback for preview→editor scroll sync.
     * Called with the source line number when the user scrolls the preview.
     */
    fun setOnScrollCallback(callback: (Int) -> Unit) {
        onScrollCallback = callback
    }

    fun setZoom(level: Double) {
        zoomLevel = level.coerceIn(0.5, 3.0)
        browser?.cefBrowser?.zoomLevel = zoomLevel - 1.0 // JCEF zoom is relative: 0.0 = 100%
    }

    fun getZoom(): Double = zoomLevel

    fun zoomIn() = setZoom(zoomLevel + 0.1)
    fun zoomOut() = setZoom(zoomLevel - 0.1)
    fun zoomReset() = setZoom(1.0)

    fun setTheme(theme: PreviewTheme.Theme) {
        currentTheme = theme
        updatePreview()
    }

    override fun getComponent(): JComponent = mainComponent
    override fun getPreferredFocusedComponent(): JComponent? = mainComponent
    override fun getName(): String = "Markdown Preview"
    override fun setState(state: FileEditorState) {}
    override fun isModified(): Boolean = false
    override fun isValid(): Boolean = file.isValid
    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}
    override fun getFile(): VirtualFile = file

    override fun dispose() {
        // Unsubscribe from settings changes
        settingsListener?.let { listener ->
            try {
                MarkdownSettings.getInstance().removeChangeListener(listener)
            } catch (_: Exception) {
                // No application context
            }
        }
        jsQuery = null
        browser?.dispose()
        browser = null
    }

    /**
     * Build JavaScript for bidirectional scroll synchronization.
     * Included in the HTML page via wrapInDocument's extraJs parameter.
     */
    private fun buildScrollSyncJs(): String {
        val query = jsQuery ?: return ""
        val scrollEnabled = try {
            MarkdownSettings.getInstance().state.scrollSyncEnabled
        } catch (_: Exception) { true }
        if (!scrollEnabled) return ""

        val injection = query.inject("'' + line")

        return """
(function() {
    // Editor -> Preview: scroll to the element closest to targetLine
    window.__scrollToSourceLine = function(targetLine) {
        var elements = document.querySelectorAll('[data-source-line]');
        var best = null;
        for (var i = 0; i < elements.length; i++) {
            var line = parseInt(elements[i].getAttribute('data-source-line'));
            if (line <= targetLine) best = elements[i];
            if (line > targetLine) break;
        }
        if (best) {
            window.__scrollFromEditor = true;
            best.scrollIntoView({behavior:'auto', block:'start'});
            setTimeout(function() { window.__scrollFromEditor = false; }, 100);
        }
    };

    // Preview -> Editor: report visible source line on user scroll
    var _lastLine = -1;
    var _scrollTimer = null;
    window.__scrollFromEditor = false;
    window.addEventListener('scroll', function() {
        if (window.__scrollFromEditor) return;
        if (_scrollTimer) clearTimeout(_scrollTimer);
        _scrollTimer = setTimeout(function() {
            var elements = document.querySelectorAll('[data-source-line]');
            var viewTop = window.scrollY + 10;
            var best = null;
            for (var i = 0; i < elements.length; i++) {
                if (elements[i].offsetTop <= viewTop) best = elements[i];
                else break;
            }
            if (best) {
                var line = parseInt(best.getAttribute('data-source-line'));
                if (!isNaN(line) && line !== _lastLine) {
                    _lastLine = line;
                    $injection
                }
            }
        }, 50);
    }, {passive: true});
})();
""".trimIndent()
    }
}
