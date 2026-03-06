package com.tribus.markdown.lang

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

class MarkdownFileType private constructor() : LanguageFileType(MarkdownLanguage) {

    override fun getName(): String = "MarkdownAIO"

    override fun getDescription(): String = "Markdown file"

    override fun getDefaultExtension(): String = "md"

    override fun getIcon(): Icon? = MarkdownIcons.FILE

    companion object {
        @JvmField
        val INSTANCE = MarkdownFileType()
    }
}
