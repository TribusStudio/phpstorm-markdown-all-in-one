package com.tribus.markdown.settings

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.ProjectManager
import java.util.concurrent.CopyOnWriteArrayList

@Service
@State(
    name = "MarkdownAllInOneSettings",
    storages = [Storage("MarkdownAllInOne.xml")]
)
class MarkdownSettings : PersistentStateComponent<MarkdownSettings.State> {

    fun interface ChangeListener {
        fun settingsChanged(state: State)
    }

    data class State(
        // Formatting
        var boldIndicator: String = "**",
        var italicIndicator: String = "*",

        // List editing
        var autoRenumberOrderedLists: Boolean = true,
        var listIndentationSize: String = "adaptive",
        var orderedListMarkerStyle: String = "ordered",  // "ordered" (1. 2. 3.) or "one" (always 1.)
        var listToggleCandidates: String = "-, *, +, 1., 1)",

        // Table of Contents
        var tocLevels: String = "1..6",
        var tocUpdateOnSave: Boolean = true,
        var tocOrderedList: Boolean = false,
        var tocSlugifyMode: String = "github",
        var tocUnorderedListMarker: String = "-",

        // Table formatting
        var tableFormatterEnabled: Boolean = true,
        var tableFormatOnSave: Boolean = true,
        var tableMaxWidth: Int = 0,  // 0 = no limit, otherwise column limit (e.g. 80, 120)

        // Completion
        var autoPopupCompletionEnabled: Boolean = true,

        // Preview
        var previewTheme: String = "auto",
        var previewCustomCssPath: String = "",
        var scrollSyncEnabled: Boolean = true,

        // Toolbar
        var toolbarEnabled: Boolean = true,
        var contextSensitiveToolbar: Boolean = true,

        // Smart paste
        var smartPasteEnabled: Boolean = true,

        // Math
        var mathEnabled: Boolean = true,

        // Editor decorations
        var decorationCodeSpanBackground: Boolean = true,
        var decorationStrikethrough: Boolean = true,
        var decorationFormattingMarkerDimming: Boolean = true,
        var decorationTrailingSpace: Boolean = true,
        var decorationHardLineBreak: Boolean = true,
        var decorationFileSizeLimit: Int = 500000,

        // Export
        var exportEmbedImages: Boolean = false,
        var exportValidateLinks: Boolean = true,
        var exportOnSave: Boolean = false,
        var exportPureCss: Boolean = false,
        var exportConvertMdLinks: Boolean = true,

        // Preview
        var autoShowPreview: Boolean = true,
    )

    private var state = State()
    private val listeners = CopyOnWriteArrayList<ChangeListener>()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
        notifyListeners()
        restartHighlighting()
    }

    fun addChangeListener(listener: ChangeListener) {
        listeners.add(listener)
    }

    fun removeChangeListener(listener: ChangeListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        for (listener in listeners) {
            listener.settingsChanged(state)
        }
    }

    private fun restartHighlighting() {
        for (project in ProjectManager.getInstance().openProjects) {
            if (!project.isDisposed) {
                DaemonCodeAnalyzer.getInstance(project).restart()
            }
        }
    }

    companion object {
        fun getInstance(): MarkdownSettings {
            return ApplicationManager.getApplication().getService(MarkdownSettings::class.java)
        }
    }
}
