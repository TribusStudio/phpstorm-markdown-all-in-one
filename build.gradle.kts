import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.11.0"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        phpstorm(providers.gradleProperty("platformVersion"))

        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)
    }

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.11.4")
}

/**
 * Build the plugin description shown in Settings > Plugins from README.md, so the
 * two can't drift.
 *
 * Two parts, both derived from the README:
 *  - the block between the `<!-- Plugin description -->` markers (intro + screenshot)
 *  - a feature list built from the `###` headings under `## Features`
 *
 * Relative image paths are rewritten to absolute raw.githubusercontent.com URLs.
 * The IDE's plugin details panel installs an image-view handler for the description
 * pane (PluginDetailsPageComponent.createHtmlImageViewHandler), so <img> renders —
 * but only for absolute URLs, since there is no document base to resolve against.
 *
 * Only the small HTML subset the plugin description supports is emitted:
 * p, b, i, code, a, img, ul/li, blockquote, h3.
 */
fun extractDescription(): String {
    val readme = file("README.md")
    val repoUrl = providers.gradleProperty("pluginRepositoryUrl").get().trimEnd('/')
    val branch = providers.gradleProperty("pluginRepositoryBranch").get()
    val rawBase = repoUrl.replace("https://github.com/", "https://raw.githubusercontent.com/") + "/" + branch + "/"

    if (!readme.exists()) return "<p>See <a href=\"$repoUrl\">GitHub</a> for details.</p>"
    val lines = readme.readLines()

    fun inline(text: String): String {
        var t = text
            .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        // images before links — the syntaxes overlap
        t = Regex("""!\[([^\]]*)]\(([^)\s]+)(?:\s+"[^"]*")?\)""").replace(t) { m ->
            val alt = m.groupValues[1]
            val src = m.groupValues[2].let { if (it.startsWith("http")) it else rawBase + it.removePrefix("./") }
            """<img src="$src" alt="$alt" width="600"/>"""
        }
        t = Regex("""\[([^\]]+)]\(([^)\s]+)(?:\s+"[^"]*")?\)""").replace(t) { m ->
            """<a href="${m.groupValues[2]}">${m.groupValues[1]}</a>"""
        }
        t = Regex("""`([^`]+)`""").replace(t) { "<code>" + it.groupValues[1] + "</code>" }
        t = Regex("""\*\*([^*]+)\*\*""").replace(t) { "<b>" + it.groupValues[1] + "</b>" }
        t = Regex("""(?<![*\w])\*([^*]+)\*(?!\w)""").replace(t) { "<i>" + it.groupValues[1] + "</i>" }
        return t
    }

    val html = StringBuilder()

    // ── Part 1: the marked intro block ───────────────────────────────
    val start = lines.indexOfFirst { it.trim() == "<!-- Plugin description -->" }
    val end = lines.indexOfFirst { it.trim() == "<!-- Plugin description end -->" }
    if (start >= 0 && end > start) {
        for (raw in lines.subList(start + 1, end)) {
            val line = raw.trim()
            when {
                line.isEmpty() -> {}
                line.startsWith("> ") -> html.append("<blockquote><p>${inline(line.removePrefix("> "))}</p></blockquote>")
                else -> html.append("<p>${inline(line)}</p>")
            }
        }
    }

    // ── Part 2: feature headings under "## Features" ─────────────────
    val featIdx = lines.indexOfFirst { it.trim() == "## Features" }
    if (featIdx >= 0) {
        val features = lines.drop(featIdx + 1)
            .takeWhile { !it.startsWith("## Installation") }
            .filter { it.startsWith("### ") }
            .map { it.removePrefix("### ").trim() }
        if (features.isNotEmpty()) {
            html.append("<h3>Features</h3><ul>")
            features.forEach { html.append("<li>${inline(it)}</li>") }
            html.append("</ul>")
        }
    }

    html.append("""<p><a href="$repoUrl">Full documentation on GitHub</a></p>""")
    return html.toString()
}

/**
 * Extract the current version's section from CHANGELOG.md and convert to HTML
 * for the "What's New" tab in the plugin dialog.
 */
fun extractChangeNotes(): String {
    val changelog = file("CHANGELOG.md")
    val repoUrl = "https://github.com/TribusStudio/phpstorm-markdown-all-in-one"

    // Build number -> marketing version: "251" -> "2025.1", "262.*" -> "2026.2"
    fun releaseName(build: String): String {
        val b = build.substringBefore('.')
        return "20${b.take(2)}.${b.drop(2).take(1)}"
    }

    val since = providers.gradleProperty("pluginSinceBuild").get()
    val until = providers.gradleProperty("pluginUntilBuild").get()
    val compatibility =
        "<p><b>Compatible with PhpStorm ${releaseName(since)} \u2013 ${releaseName(until)}</b> " +
            "(builds <code>$since</code> \u2013 <code>$until</code>)</p>"

    val footer = compatibility + """<p><a href="$repoUrl">Full documentation on GitHub</a></p>"""

    if (!changelog.exists()) return "<p>See <a href=\"$repoUrl\">GitHub</a> for details.</p>"

    val pluginVersion = providers.gradleProperty("pluginVersion").get()
    val lines = changelog.readLines()
    val section = mutableListOf<String>()
    var capturing = false

    for (line in lines) {
        if (line.startsWith("## ") && line.contains("[$pluginVersion]")) {
            capturing = true
            continue
        }
        if (capturing && line.startsWith("## ")) break
        if (capturing) section.add(line)
    }

    if (section.isEmpty()) return "<p>Version $pluginVersion</p>$footer"

    // Convert the markdown section to HTML
    val html = StringBuilder()
    var inList = false

    for (line in section) {
        when {
            line.startsWith("### ") -> {
                if (inList) { html.append("</ul>"); inList = false }
                html.append("<h3>${line.removePrefix("### ")}</h3>")
            }
            line.startsWith("- ") -> {
                if (!inList) { html.append("<ul>"); inList = true }
                val content = line.removePrefix("- ")
                    .replace(Regex("\\*\\*(.+?)\\*\\*"), "<b>$1</b>")
                    .replace(Regex("`(.+?)`"), "<code>$1</code>")
                html.append("<li>$content</li>")
            }
            line.isBlank() -> {}
            else -> {
                if (inList) { html.append("</ul>"); inList = false }
                val content = line
                    .replace(Regex("\\*\\*(.+?)\\*\\*"), "<b>$1</b>")
                    .replace(Regex("`(.+?)`"), "<code>$1</code>")
                html.append("<p>$content</p>")
            }
        }
    }
    if (inList) html.append("</ul>")

    return "$html$footer"
}

intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }
        description = provider { extractDescription() }
        changeNotes = provider { extractChangeNotes() }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }

    pluginVerification {
        ides {
            // Verify against both ends of the declared compatibility range.
            // The plugin is compiled against the oldest supported platform, so
            // the newest IDE is where binary incompatibilities would show up.
            // create(type, version) supersedes the deprecated ide(type, version).
            // The Provider overload also keeps the version lazy instead of
            // resolving it eagerly at configuration time.
            create(IntelliJPlatformType.PhpStorm, providers.gradleProperty("platformVersion"))
            create(IntelliJPlatformType.PhpStorm, providers.gradleProperty("verifyAgainstVersion"))
        }
    }
}

tasks {
    test {
        useJUnitPlatform()
        // IntelliJ platform test framework needs these for headless test execution
        systemProperty("idea.force.use.core.classloader", "true")
        jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    }

    wrapper {
        gradleVersion = "9.2.1"
    }
}
