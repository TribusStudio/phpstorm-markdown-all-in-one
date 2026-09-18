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
