package com.tribus.markdown.preview

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.jcef.JBCefBrowser
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

    private val mainComponent: JComponent by lazy {
        try {
            val b = JBCefBrowser()
            browser = b
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
        var bodyHtml = MarkdownHtmlConverter.convert(document.text)

        // Resolve relative image paths to absolute file:// URLs so they render
        // in the JCEF preview (loadHTML's baseUrl doesn't reliably resolve locals).
        val parentPath = file.parent?.path
        if (parentPath != null) {
            val baseDir = File(parentPath)
            val warnings = mutableListOf<String>()
            bodyHtml = HtmlExporter.resolveImagePaths(bodyHtml, baseDir, false, warnings)
        }

        val fullHtml = MarkdownHtmlConverter.wrapInDocument(bodyHtml, css, customCss, isDark)
        browser?.loadHTML(fullHtml)
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
        browser?.dispose()
        browser = null
    }
}
