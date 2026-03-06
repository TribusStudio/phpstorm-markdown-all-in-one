package com.tribus.markdown.lang

import com.intellij.lang.Language

object MarkdownLanguage : Language("MarkdownAIO") {
    private fun readResolve(): Any = MarkdownLanguage

    override fun getDisplayName(): String = "Markdown"
}
