package com.tribus.markdown.preview

import junit.framework.TestCase
import java.io.File

/**
 * The live preview re-renders on every document change, so theme CSS loading
 * sits on a hot path. These cover the caching added to keep it off the
 * filesystem without going stale.
 */
class PreviewThemeTest : TestCase() {

    override fun setUp() {
        super.setUp()
        PreviewTheme.clearCache()
    }

    override fun tearDown() {
        PreviewTheme.clearCache()
        super.tearDown()
    }

    fun testBundledThemeCssIsLoaded() {
        val css = PreviewTheme.loadThemeCss(PreviewTheme.Theme.GITHUB)
        assertTrue("expected non-empty bundled CSS", css.isNotEmpty())
    }

    fun testRepeatedLoadsReturnTheSameCachedContent() {
        val first = PreviewTheme.loadThemeCss(PreviewTheme.Theme.GITHUB)
        val second = PreviewTheme.loadThemeCss(PreviewTheme.Theme.GITHUB)
        assertSame("repeated loads should hit the cache", first, second)
    }

    fun testBlankCustomCssPathReturnsEmpty() {
        assertEquals("", PreviewTheme.loadCustomCss(""))
    }

    fun testMissingCustomCssFileReturnsEmpty() {
        assertEquals("", PreviewTheme.loadCustomCss("/no/such/file-xyz.css"))
    }

    fun testCustomCssIsCachedButPicksUpEdits() {
        val file = File.createTempFile("markdown-aio-", ".css")
        try {
            file.writeText("body { color: red; }")
            assertEquals("body { color: red; }", PreviewTheme.loadCustomCss(file.path))

            // Same content, cached — and a later edit must still be observed.
            assertEquals("body { color: red; }", PreviewTheme.loadCustomCss(file.path))

            file.writeText("body { color: blue; }")
            file.setLastModified(file.lastModified() + 2000)
            assertEquals("body { color: blue; }", PreviewTheme.loadCustomCss(file.path))
        } finally {
            file.delete()
        }
    }
}
