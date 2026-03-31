package com.tribus.markdown.preview

import com.intellij.icons.AllIcons
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
import com.intellij.util.ui.JBUI
import com.tribus.markdown.export.HtmlExporter
import com.tribus.markdown.settings.MarkdownSettings
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.network.CefRequest
import java.awt.BorderLayout
import java.awt.Desktop
import java.awt.FlowLayout
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File
import java.beans.PropertyChangeListener
import java.net.URI
import java.net.URLDecoder
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
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
    private var linkClickQuery: JBCefJSQuery? = null
    private var onScrollCallback: ((Int) -> Unit)? = null

    // Navigation bar for when the user navigates away from preview
    private var navBar: JPanel? = null
    @Volatile
    private var isShowingPreview = true

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

            // Set up link click interception bridge
            try {
                val linkQuery = JBCefJSQuery.create(b as JBCefBrowserBase)
                linkQuery.addHandler { url ->
                    handleLinkClick(url)
                    null
                }
                linkClickQuery = linkQuery
            } catch (_: Exception) {
                // JBCefJSQuery not available
            }

            // Intercept navigation requests — block external URLs, open in system browser
            try {
                b.jbCefClient.addRequestHandler(object : CefRequestHandlerAdapter() {
                    override fun onBeforeBrowse(
                        browser: CefBrowser?,
                        frame: CefFrame?,
                        request: CefRequest?,
                        userGesture: Boolean,
                        isRedirect: Boolean
                    ): Boolean {
                        val url = request?.url ?: return false
                        // Allow data: URLs and about:blank (used by loadHTML)
                        if (url.startsWith("data:") || url.startsWith("about:")) return false
                        // Allow file: URLs (local resources)
                        if (url.startsWith("file:")) return false
                        // Block external URLs — they're handled via JS interception + system browser
                        if (url.startsWith("http://") || url.startsWith("https://")) {
                            if (userGesture) {
                                openInSystemBrowser(url)
                            }
                            return true // block navigation
                        }
                        return false
                    }
                }, b.cefBrowser)
            } catch (_: Exception) {
                // CefRequestHandler not available
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

            // Wrap browser with navigation bar
            val wrapper = JPanel(BorderLayout())
            val nav = createNavBar()
            navBar = nav
            wrapper.add(nav, BorderLayout.NORTH)
            wrapper.add(b.component, BorderLayout.CENTER)
            wrapper
        } catch (_: Exception) {
            // JCEF not available (headless, older IDE, etc.)
            val label = JLabel("Preview not available (JCEF not supported)", SwingConstants.CENTER)
            fallbackComponent = label
            label
        }
    }

    private fun createNavBar(): JPanel {
        val bar = JPanel(FlowLayout(FlowLayout.LEFT, 4, 2))
        bar.border = JBUI.Borders.customLineBottom(JBUI.CurrentTheme.Editor.BORDER_COLOR)

        val backBtn = JButton(AllIcons.Actions.Back)
        backBtn.toolTipText = "Back"
        backBtn.isFocusable = false
        backBtn.addActionListener { browser?.cefBrowser?.goBack() }

        val forwardBtn = JButton(AllIcons.Actions.Forward)
        forwardBtn.toolTipText = "Forward"
        forwardBtn.isFocusable = false
        forwardBtn.addActionListener { browser?.cefBrowser?.goForward() }

        val returnBtn = JButton("Return to Preview")
        returnBtn.toolTipText = "Return to the markdown preview"
        returnBtn.isFocusable = false
        returnBtn.addActionListener {
            isShowingPreview = true
            bar.isVisible = false
            updatePreview()
        }

        val openExternalBtn = JButton(AllIcons.Ide.External_link_arrow)
        openExternalBtn.toolTipText = "Open in browser"
        openExternalBtn.isFocusable = false
        openExternalBtn.addActionListener {
            val url = browser?.cefBrowser?.url
            if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                openInSystemBrowser(url)
            }
        }

        bar.add(backBtn)
        bar.add(forwardBtn)
        bar.add(returnBtn)
        bar.add(openExternalBtn)
        bar.isVisible = false // hidden by default
        return bar
    }

    private fun handleLinkClick(url: String) {
        when {
            url.startsWith("#") -> {
                // Anchor link — scroll within preview
                val anchor = url.removePrefix("#")
                browser?.cefBrowser?.executeJavaScript(
                    "var el = document.getElementById('$anchor'); if(el) el.scrollIntoView({behavior:'smooth', block:'start'});",
                    "", 0
                )
            }
            url.startsWith("http://") || url.startsWith("https://") -> {
                openInSystemBrowser(url)
            }
            else -> {
                // Relative file link — resolve against current file's directory and open
                openRelativeFile(url)
            }
        }
    }

    private fun openRelativeFile(relativePath: String) {
        val parentDir = file.parent?.path ?: return
        // Strip anchor fragment if present (e.g., "file.md#heading")
        val pathPart = relativePath.split("#").first()
        val decoded = try { URLDecoder.decode(pathPart, "UTF-8") } catch (_: Exception) { pathPart }
        val targetFile = File(parentDir, decoded).canonicalFile

        ApplicationManager.getApplication().invokeLater {
            val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(targetFile)
            if (virtualFile != null && virtualFile.exists()) {
                // Find the project that contains this file
                val project = ProjectManager.getInstance().openProjects.firstOrNull { p ->
                    !p.isDisposed
                } ?: return@invokeLater

                // Open the file in the editor — our split editor provider will handle .md files
                val descriptor = OpenFileDescriptor(project, virtualFile)
                FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
            }
        }
    }

    private fun openInSystemBrowser(url: String) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI(url))
            }
        } catch (_: Exception) {
            // Failed to open browser
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

        val mathEnabled = settings?.state?.mathEnabled ?: true

        val scrollJs = buildScrollSyncJs()
        val linkJs = buildLinkInterceptJs()
        val combinedJs = listOf(scrollJs, linkJs).filter { it.isNotEmpty() }.joinToString("\n")
        val fullHtml = MarkdownHtmlConverter.wrapInDocument(bodyHtml, css, customCss, isDark, combinedJs, mathEnabled)
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
        linkClickQuery = null
        browser?.dispose()
        browser = null
    }

    /**
     * Build JavaScript that intercepts link clicks.
     * External links are routed through JBCefJSQuery to open in the system browser.
     * Anchor links are handled with smooth scrolling within the page.
     */
    private fun buildLinkInterceptJs(): String {
        val query = linkClickQuery ?: return ""
        val injection = query.inject("href")

        return """
(function() {
    document.addEventListener('click', function(e) {
        var link = e.target.closest('a');
        if (!link) return;
        var href = link.getAttribute('href');
        if (!href) return;

        // Anchor links — scroll within page
        if (href.startsWith('#')) {
            e.preventDefault();
            var anchor = href.substring(1);
            var el = document.getElementById(anchor);
            if (el) el.scrollIntoView({behavior:'smooth', block:'start'});
            return;
        }

        // External links — send to Kotlin side to open in system browser
        if (href.startsWith('http://') || href.startsWith('https://')) {
            e.preventDefault();
            $injection
            return;
        }

        // Relative file links — send to Kotlin side to open in the IDE
        if (!href.startsWith('data:') && !href.startsWith('javascript:')) {
            e.preventDefault();
            $injection
            return;
        }
    }, true);
})();
""".trimIndent()
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
    // Uses linear interpolation between annotated elements for accuracy.
    var _lastLine = -1;
    var _scrollTimer = null;
    window.__scrollFromEditor = false;
    window.addEventListener('scroll', function() {
        if (window.__scrollFromEditor) return;
        if (_scrollTimer) clearTimeout(_scrollTimer);
        _scrollTimer = setTimeout(function() {
            var elements = document.querySelectorAll('[data-source-line]');
            if (elements.length === 0) return;
            var viewTop = window.scrollY + 10;

            // Find the two elements that bracket the current scroll position
            var before = null;
            var after = null;
            for (var i = 0; i < elements.length; i++) {
                if (elements[i].offsetTop <= viewTop) {
                    before = elements[i];
                } else {
                    after = elements[i];
                    break;
                }
            }

            var line;
            if (before && after) {
                // Interpolate between the two elements
                var beforeLine = parseInt(before.getAttribute('data-source-line'));
                var afterLine = parseInt(after.getAttribute('data-source-line'));
                var beforeTop = before.offsetTop;
                var afterTop = after.offsetTop;
                var range = afterTop - beforeTop;
                if (range > 0) {
                    var ratio = (viewTop - beforeTop) / range;
                    line = Math.round(beforeLine + ratio * (afterLine - beforeLine));
                } else {
                    line = beforeLine;
                }
            } else if (before) {
                // Past the last element — use its line
                line = parseInt(before.getAttribute('data-source-line'));
            } else if (after) {
                // Before the first element
                line = parseInt(after.getAttribute('data-source-line'));
            } else {
                return;
            }

            if (!isNaN(line) && line !== _lastLine) {
                _lastLine = line;
                $injection
            }
        }, 50);
    }, {passive: true});
})();
""".trimIndent()
    }
}
