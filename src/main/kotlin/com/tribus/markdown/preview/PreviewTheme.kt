package com.tribus.markdown.preview

import com.intellij.openapi.editor.colors.EditorColorsManager
import java.io.File

/**
 * Manages CSS themes for the markdown preview.
 * Supports bundled themes and user-provided CSS overrides.
 */
object PreviewTheme {

    enum class Theme(val displayName: String, val resourcePath: String) {
        GITHUB("GitHub", "/preview/css/github.css"),
        GITHUB_DARK("GitHub Dark", "/preview/css/github-dark.css"),
        GITLAB("GitLab", "/preview/css/gitlab.css"),
        VSCODE("VSCode", "/preview/css/vscode.css"),
        AUTO("Auto (follow IDE theme)", "");

        companion object {
            fun fromName(name: String): Theme {
                return entries.find { it.name.equals(name, ignoreCase = true) }
                    ?: entries.find { it.displayName.equals(name, ignoreCase = true) }
                    ?: GITHUB
            }

            fun displayNames(): List<String> = entries.map { it.displayName }
        }
    }

    /**
     * Load the CSS content for a given theme.
     * AUTO mode selects GitHub or GitHub Dark based on the IDE theme.
     */
    fun loadThemeCss(theme: Theme): String {
        val resolvedTheme = if (theme == Theme.AUTO) {
            if (isIdeDarkTheme()) Theme.GITHUB_DARK else Theme.GITHUB
        } else {
            theme
        }

        return loadResource(resolvedTheme.resourcePath)
    }

    /**
     * Load user CSS overrides from a file path.
     * Returns empty string if the path is blank or the file doesn't exist.
     */
    fun loadCustomCss(path: String): String {
        if (path.isBlank()) return ""
        val file = File(path)
        return if (file.exists() && file.isFile) {
            try {
                file.readText()
            } catch (_: Exception) {
                ""
            }
        } else ""
    }

    private fun loadResource(path: String): String {
        if (path.isEmpty()) return ""
        return PreviewTheme::class.java.getResourceAsStream(path)
            ?.bufferedReader()
            ?.readText()
            ?: ""
    }

    fun isIdeDarkTheme(): Boolean {
        return try {
            val scheme = EditorColorsManager.getInstance().globalScheme
            scheme.defaultBackground.red < 128
        } catch (_: Exception) {
            false
        }
    }
}
