package com.tribus.markdown.actions

import com.tribus.markdown.util.MarkdownFormattingUtil

class ToggleBoldAction : BaseToggleFormattingAction(
    wrapper = MarkdownFormattingUtil.FormattingWrapper.BOLD
)
