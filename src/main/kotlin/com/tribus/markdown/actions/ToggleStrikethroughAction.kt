package com.tribus.markdown.actions

import com.tribus.markdown.util.MarkdownFormattingUtil

class ToggleStrikethroughAction : BaseToggleFormattingAction(
    wrapper = MarkdownFormattingUtil.FormattingWrapper.STRIKETHROUGH
)
