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
    val footer = """<p><a href="$repoUrl">Full documentation on GitHub</a></p>"""

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
            sinceBuild = "251"
            untilBuild = "261.*"
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
            recommended()
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
