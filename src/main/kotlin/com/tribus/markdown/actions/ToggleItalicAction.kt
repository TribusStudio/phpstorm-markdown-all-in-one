package com.tribus.markdown.actions

import com.tribus.markdown.util.MarkdownFormattingUtil

class ToggleItalicAction : BaseToggleFormattingAction(
    wrapper = MarkdownFormattingUtil.FormattingWrapper.ITALIC
)
