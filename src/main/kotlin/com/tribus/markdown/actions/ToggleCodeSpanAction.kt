package com.tribus.markdown.actions

import com.tribus.markdown.util.MarkdownFormattingUtil

class ToggleCodeSpanAction : BaseToggleFormattingAction(
    wrapper = MarkdownFormattingUtil.FormattingWrapper.CODE_SPAN
)
