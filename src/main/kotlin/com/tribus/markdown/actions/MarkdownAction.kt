package com.tribus.markdown.actions

/**
 * Marker interface for all Markdown All-in-One actions.
 * Used by [MarkdownActionPromoter] to identify our actions during shortcut
 * conflict resolution so they take priority over built-in IDE actions
 * when editing markdown files.
 */
interface MarkdownAction
