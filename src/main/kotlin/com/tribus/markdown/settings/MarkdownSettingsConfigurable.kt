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
                        .comment("\"adaptive\" uses 2 or 4 spaces based on list marker width")
                }
                row("Ordered list marker style:") {
                    comboBox(listOf("ordered", "one"))
                        .bindItem(
                            { state.orderedListMarkerStyle },
                            { state.orderedListMarkerStyle = it ?: "ordered" }
                        )
                        .comment("\"ordered\" increments (1. 2. 3.), \"one\" always uses 1.")
                }
                row("Toggle list candidates:") {
                    textField()
                        .bindText(state::listToggleCandidates)
                        .comment("Comma-separated markers to cycle through, e.g. \"-, *, +, 1., 1)\"")
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
                    comboBox(listOf("github", "gitlab", "azure-devops", "bitbucket", "gitea", "zola"))
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
                row {
                    checkBox("Synchronize editor and preview scroll position")
                        .bindSelected(state::scrollSyncEnabled)
                        .comment("Scrolling the editor or preview keeps both panels aligned")
                }
                row {
                    checkBox("Auto-show preview when opening markdown files")
                        .bindSelected(state::autoShowPreview)
                        .comment("Default to split preview mode for files opened for the first time. PHPStorm remembers your per-file layout choice after that.")
                }
            }

            group("Math") {
                row {
                    checkBox("Enable math rendering in preview")
                        .bindSelected(state::mathEnabled)
                        .comment("Render \$...\$ (inline) and \$\$...\$\$ (display) math using KaTeX in the preview panel")
                }
            }

            group("Toolbar") {
                row {
                    checkBox("Show editor toolbar")
                        .bindSelected(state::toolbarEnabled)
                        .comment("Display the formatting toolbar at the top of markdown editors")
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

            group("Editor Decorations") {
                row {
                    checkBox("Code span background")
                        .bindSelected(state::decorationCodeSpanBackground)
                        .comment("Show a subtle background tint on inline code spans")
                }
                row {
                    checkBox("Strikethrough rendering")
                        .bindSelected(state::decorationStrikethrough)
                        .comment("Show line-through effect on ~~strikethrough~~ text")
                }
                row {
                    checkBox("Formatting marker dimming")
                        .bindSelected(state::decorationFormattingMarkerDimming)
                        .comment("Render **, ~~, *, _ markers in a muted color")
                }
                row {
                    checkBox("Trailing space indicator")
                        .bindSelected(state::decorationTrailingSpace)
                        .comment("Highlight trailing whitespace with a background color")
                }
                row {
                    checkBox("Hard line break indicator")
                        .bindSelected(state::decorationHardLineBreak)
                        .comment("Highlight trailing double-space (hard <br>) with a distinct background")
                }
                row("File size limit (chars):") {
                    intTextField(0..10_000_000)
                        .bindIntText(state::decorationFileSizeLimit)
                        .comment("Skip decorations on files larger than this (0 = no limit)")
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
                row {
                    checkBox("Auto-export HTML on save")
                        .bindSelected(state::exportOnSave)
                        .comment("Automatically generate an .html file alongside the .md file when saving")
                }
                row {
                    checkBox("Convert .md links to .html in export")
                        .bindSelected(state::exportConvertMdLinks)
                        .comment("Rewrite internal [text](file.md) links to [text](file.html) in exported HTML")
                }
                row {
                    checkBox("Pure HTML export (no CSS)")
                        .bindSelected(state::exportPureCss)
                        .comment("Export without any theme CSS stylesheets — just the raw HTML body")
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
