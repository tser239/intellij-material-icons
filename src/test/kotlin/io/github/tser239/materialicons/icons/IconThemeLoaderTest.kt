package io.github.tser239.materialicons.icons

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Loads the theme shipped in `src/main/resources`, which also proves that the manifest format and
 * the resource layout stay in sync. These assertions deliberately reference ids from the full
 * upstream theme instead of the ids the plugin itself uses, so a broken or partial import fails
 * here rather than silently degrading the icons in the IDE.
 */
class IconThemeLoaderTest {

    private val theme = IconThemeLoader.load(
        themeId = "material",
        manifestPath = "/themes/material/material-icons.json",
        classLoader = javaClass.classLoader,
    )

    @Test
    fun `loads the complete upstream theme`() {
        assertTrue(theme.iconPaths.size > 1000, "expected the full theme, got ${theme.iconPaths.size} icons")
    }

    @Test
    fun `loads exact file name associations`() {
        assertEquals("docker", theme.iconForFile("Dockerfile"))
        assertEquals("git", theme.iconForFile(".gitignore"))
        assertEquals("readme", theme.iconForFile("README.md"))
        assertEquals("nodejs", theme.iconForFile("package.json"))
    }

    @Test
    fun `loads extension associations`() {
        assertEquals("kotlin", theme.iconForFile("Main.kt"))
        assertEquals("typescript", theme.iconForFile("app.ts"))
        assertEquals("react_ts", theme.iconForFile("App.tsx"))
        assertEquals("markdown", theme.iconForFile("notes.md"))
    }

    @Test
    fun `prefers multi-part extensions from the theme`() {
        assertEquals("test-ts", theme.iconForFile("Button.spec.ts"))
    }

    @Test
    fun `loads folder associations`() {
        assertEquals("folder-src", theme.iconForFolder("src"))
        assertEquals("folder-test", theme.iconForFolder("tests"))
        assertEquals("folder-node", theme.iconForFolder("node_modules"))
        assertEquals("folder-github", theme.iconForFolder(".github"))
    }

    @Test
    fun `every association points at an icon that is actually bundled`() {
        val manifest = javaClass.getResource("/themes/material/material-icons.json")
        assertNotNull(manifest)

        val broken = theme.iconPaths.filterValues { javaClass.getResource(it) == null }
        assertTrue(broken.isEmpty(), "missing icon resources: $broken")
    }

    @Test
    fun `every association target is defined in the theme`() {
        val stream = javaClass.getResourceAsStream("/themes/material/material-icons.json")
        assertNotNull(stream)
        val root = stream!!.use { com.google.gson.JsonParser.parseReader(it.reader()).asJsonObject }

        val defined = root.getAsJsonObject("iconDefinitions").keySet()
        val dangling = mutableListOf<String>()
        for (section in listOf("fileNames", "fileExtensions", "folderNames")) {
            val map = root.getAsJsonObject(section) ?: continue
            for ((pattern, id) in map.entrySet()) {
                if (id.asString !in defined) dangling += "$section:$pattern -> ${id.asString}"
            }
        }
        assertTrue(dangling.isEmpty(), "associations point at undefined icons: ${dangling.take(20)}")
    }

    @Test
    fun `resolves icon paths to classpath resources`() {
        val path = theme.iconPath("kotlin")
        assertEquals("/themes/material/icons/kotlin.svg", path)
        assertNotNull(javaClass.getResource(path))
    }

    @Test
    fun `loads default icons`() {
        assertEquals("file", theme.defaultFileIconId)
        assertEquals("folder", theme.defaultFolderIconId)
    }
}
