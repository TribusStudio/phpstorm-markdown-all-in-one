package com.tribus.markdown.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.EditorNotifications
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
                        .comment("\"adaptive\" uses 2 or 4 spaces based on list marker width")
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
                row("Unordered list marker:") {
                    comboBox(listOf("-", "*", "+"))
                        .bindItem(
                            { state.tocUnorderedListMarker },
                            { state.tocUnorderedListMarker = it ?: "-" }
                        )
                }
                row("Slug generation mode:") {
                    comboBox(listOf("github", "gitlab", "azure-devops", "bitbucket", "gitea"))
                        .bindItem(
                            { state.tocSlugifyMode },
                            { state.tocSlugifyMode = it ?: "github" }
                        )
                        .comment("How heading anchor IDs are generated")
                }
            }

            group("Table Formatting") {
                row {
                    checkBox("Enable table auto-formatting")
                        .bindSelected(state::tableFormatterEnabled)
                }
                row {
                    checkBox("Format tables on save")
                        .bindSelected(state::tableFormatOnSave)
                }
                row("Max table width (columns):") {
                    intTextField(0..300)
                        .bindIntText(state::tableMaxWidth)
                        .comment("0 = no limit. Tables wider than this use compact spacing (e.g. 80, 120)")
                }
            }

            group("Preview") {
                row("Render theme:") {
                    comboBox(listOf("Auto (follow IDE theme)", "GitHub", "GitHub Dark", "GitLab", "VSCode"))
                        .bindItem(
                            { state.previewTheme },
                            { state.previewTheme = it ?: "auto" }
                        )
                        .comment("CSS theme for the markdown preview panel")
                }
                row("Custom CSS path:") {
                    textField()
                        .bindText(state::previewCustomCssPath)
                        .comment("Optional path to a .css file for additional style overrides")
                }
            }

            group("Toolbar") {
                row {
                    checkBox("Show editor toolbar")
                        .bindSelected(state::toolbarEnabled)
                        .comment("Display the formatting toolbar at the top of markdown editors")
                }
                row("Button display:") {
                    comboBox(listOf("icons", "labels", "icons and labels"))
                        .bindItem(
                            { state.toolbarDisplayMode },
                            { state.toolbarDisplayMode = it ?: "icons" }
                        )
                }
            }

            group("Completion") {
                row {
                    checkBox("Auto-popup completion in link/image paths")
                        .bindSelected(state::autoPopupCompletionEnabled)
                        .comment("Show completion popup automatically when typing ( after ] or # in links")
                }
            }

            group("Smart Paste") {
                row {
                    checkBox("Smart paste (auto-create links from URLs)")
                        .bindSelected(state::smartPasteEnabled)
                }
            }

            group("Export") {
                row {
                    checkBox("Embed images as base64 in exported HTML")
                        .bindSelected(state::exportEmbedImages)
                        .comment("Creates self-contained HTML files (larger file size)")
                }
                row {
                    checkBox("Validate links on export")
                        .bindSelected(state::exportValidateLinks)
                        .comment("Warn about broken anchors, file links, and reference definitions")
                }
            }
        }

        return panel!!
    }

    override fun isModified(): Boolean = panel?.isModified() == true

    override fun apply() {
        panel?.apply()
        settings.loadState(state)

        // Rebuild editor notification toolbars so display mode changes take effect
        for (project in ProjectManager.getInstance().openProjects) {
            EditorNotifications.getInstance(project).updateAllNotifications()
        }
    }

    override fun reset() {
        panel?.reset()
    }
}
