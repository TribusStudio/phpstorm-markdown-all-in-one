package com.tribus.markdown.lang.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider
import com.tribus.markdown.lang.MarkdownFileType
import com.tribus.markdown.lang.MarkdownLanguage

class MarkdownFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, MarkdownLanguage) {
    override fun getFileType() = MarkdownFileType.INSTANCE
}
