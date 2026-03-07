package com.tribus.markdown.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*

class MarkdownSettingsConfigurable : Configurable {

    private var panel: DialogPanel? = null
    private var state = MarkdownSettings.State()
    private val settings get() = MarkdownSettings.getInstance()

    override fun getDisplayName(): String = "Markdown All-in-One"

    override fun createComponent(): DialogPanel {
        state = settings.state.copy()

        panel = panel {
            group("Formatting") {
                row("Bold indicator:") {
                    comboBox(listOf("**", "__"))
                        .bindItem(
                            { state.boldIndicator },
                            { state.boldIndicator = it ?: "**" }
                        )
                }
                row("Italic indicator:") {
                    comboBox(listOf("*", "_"))
                        .bindItem(
                            { state.italicIndicator },
                            { state.italicIndicator = it ?: "*" }
                        )
                }
            }

            group("List Editing") {
                row {
                    checkBox("Auto-renumber ordered lists")
                        .bindSelected(state::autoRenumberOrderedLists)
                }
                row("Indentation size:") {
                    comboBox(listOf("adaptive", "inherit"))
                        .bindItem(
                            { state.listIndentationSize },
                            { state.listIndentationSize = it ?: "adaptive" }
                        )
                }
            }

            group("Table of Contents") {
                row("Heading levels:") {
                    textField()
                        .bindText(state::tocLevels)
                        .comment("Range of heading levels to include, e.g. \"2..4\"")
                }
                row {
                    checkBox("Update TOC on save")
                        .bindSelected(state::tocUpdateOnSave)
                }
                row {
                    checkBox("Use ordered list for TOC")
                        .bindSelected(state::tocOrderedList)
                }
                row("Slug generation mode:") {
                    comboBox(listOf("github", "gitlab", "azure-devops", "bitbucket", "gitea"))
                        .bindItem(
                            { state.tocSlugifyMode },
                            { state.tocSlugifyMode = it ?: "github" }
                        )
                }
            }

            group("Table Formatting") {
                row {
                    checkBox("Enable table auto-formatting")
                        .bindSelected(state::tableFormatterEnabled)
                }
            }

            group("Smart Paste") {
                row {
                    checkBox("Smart paste (auto-create links from URLs)")
                        .bindSelected(state::smartPasteEnabled)
                }
            }
        }

        return panel!!
    }

    override fun isModified(): Boolean = panel?.isModified() == true

    override fun apply() {
        panel?.apply()
        settings.loadState(state)
    }

    override fun reset() {
        panel?.reset()
    }
}
