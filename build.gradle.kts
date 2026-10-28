plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

// Optional: build against an already installed IDE instead of downloading one.
//   ./gradlew test -PlocalIdePath="$HOME/.local/share/JetBrains/Toolbox/apps/webstorm"
val localIdePath = providers.gradleProperty("localIdePath")

dependencies {
    // Small, self-contained JSON parser. Bundled into the plugin's lib/ directory.
    implementation("com.google.code.gson:gson:2.13.2")

    intellijPlatform {
        if (localIdePath.isPresent) {
            local(localIdePath.get())
        } else {
            // IntelliJ IDEA 2026.2.x (Ultimate). Only platform APIs are used, so compiling against
            // IDEA is enough for the plugin to work in every IntelliJ-based IDE.
            intellijIdea(providers.gradleProperty("platformVersion").get())
        }
        pluginVerifier()
    }

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    // The oldest supported IDE (2026.2) ships JBR 25, so the plugin can target Java 25 directly.
    jvmToolchain(25)
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

// Ship the plugin's own license inside the JAR, next to the third-party texts in resources/licenses.
tasks.processResources {
    from("LICENSE") {
        into("licenses")
        rename { "MIT.txt" }
    }
}

intellijPlatform {
    pluginConfiguration {
        id = providers.gradleProperty("pluginId").get()
        name = providers.gradleProperty("pluginName").get()
        version = providers.gradleProperty("pluginVersion").get()

        // The description lives in plugin.xml only, to keep one authoritative copy of the HTML.
        vendor {
            name = providers.gradleProperty("pluginVendor").get()
            url = providers.gradleProperty("pluginVendorUrl").get()
        }

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild").get()
            untilBuild = provider { null }
        }

        changeNotes = """
            <ul>
                <li>Initial release.</li>
                <li>Full Material Icon Theme: 1256 icons, 2156 file names, 1391 extensions and 4674 folder names.</li>
                <li>Folder icons redrawn on IntelliJ's folder silhouette, with light and dark variants.</li>
                <li>File and folder icon providers, custom associations, settings page.</li>
                <li>No PSI traversal, no indexing and no forced Project View refreshes.</li>
            </ul>
        """.trimIndent()
    }

    // Starting an IDE during the build to index the settings page is unnecessary for local builds.
    buildSearchableOptions = false

    pluginVerification {
        ides {
            // Verify against an IDE that is already installed instead of downloading multi-GB builds:
            //   ./gradlew verifyPlugin -PverifyIdePath="$HOME/.local/share/JetBrains/Toolbox/apps/webstorm"
            val verifyIdePath = providers.gradleProperty("verifyIdePath").orNull
            if (verifyIdePath != null) local(verifyIdePath) else recommended()
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
