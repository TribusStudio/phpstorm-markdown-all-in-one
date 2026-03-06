package com.tribus.markdown.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service
@State(
    name = "MarkdownAllInOneSettings",
    storages = [Storage("MarkdownAllInOne.xml")]
)
class MarkdownSettings : PersistentStateComponent<MarkdownSettings.State> {

    data class State(
        // Formatting
        var boldIndicator: String = "**",
        var italicIndicator: String = "*",

        // List editing
        var autoRenumberOrderedLists: Boolean = true,
        var listIndentationSize: String = "adaptive",

        // Table of Contents
        var tocLevels: String = "1..6",
        var tocUpdateOnSave: Boolean = true,
        var tocOrderedList: Boolean = false,
        var tocSlugifyMode: String = "github",
        var tocUnorderedListMarker: String = "-",

        // Table formatting
        var tableFormatterEnabled: Boolean = true,

        // Smart paste
        var smartPasteEnabled: Boolean = true,
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        fun getInstance(): MarkdownSettings {
            return ApplicationManager.getApplication().getService(MarkdownSettings::class.java)
        }
    }
}
