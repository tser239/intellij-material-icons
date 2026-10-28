package io.github.tser239.materialicons.icons

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class IconThemeTest {

    private val theme = IconTheme(
        id = "test",
        iconPaths = mapOf(
            "kotlin" to "/themes/test/icons/kotlin.svg",
            "typescript" to "/themes/test/icons/typescript.svg",
            "spec" to "/themes/test/icons/spec.svg",
            "archive" to "/themes/test/icons/archive.svg",
            "docker" to "/themes/test/icons/docker.svg",
            "json" to "/themes/test/icons/json.svg",
            "file" to "/themes/test/icons/file.svg",
            "folder-src" to "/themes/test/icons/folder-src.svg",
            "folder" to "/themes/test/icons/folder.svg",
        ),
        // Deliberately mixed case to prove normalisation happens at construction time.
        fileNames = mapOf(
            "Dockerfile" to "docker",
            "tsconfig.json" to "json",
        ),
        fileExtensions = mapOf(
            "kt" to "kotlin",
            "ts" to "typescript",
            "spec.ts" to "spec",
            "tar.gz" to "archive",
        ),
        folderNames = mapOf("SRC" to "folder-src", "lib" to "folder"),
        defaultFileIconId = "file",
        defaultFolderIconId = "folder",
    )

    @Test
    fun `matches exact file names case-insensitively`() {
        assertEquals("docker", theme.iconForFile("Dockerfile"))
        assertEquals("docker", theme.iconForFile("dockerfile"))
        assertEquals("docker", theme.iconForFile("DOCKERFILE"))
    }

    @Test
    fun `matches simple extensions`() {
        assertEquals("kotlin", theme.iconForFile("Main.kt"))
        assertEquals("kotlin", theme.iconForFile("Main.KT"))
    }

    @Test
    fun `prefers the longest extension`() {
        assertEquals("spec", theme.iconForFile("Button.spec.ts"))
        assertEquals("archive", theme.iconForFile("backup.tar.gz"))
        assertEquals("typescript", theme.iconForFile("Button.ts"))
    }

    @Test
    fun `does not treat dotfiles as extensions`() {
        assertNull(theme.iconForFile(".gitignore"))
        assertNull(theme.iconForFile(".env"))
    }

    @Test
    fun `dotfiles can still have a real extension`() {
        assertEquals("kotlin", theme.iconForFile(".env.kt"))
    }

    @Test
    fun `falls back to the default file icon only when enabled`() {
        assertNull(theme.iconForFile("notes.xyz"))
        assertEquals("file", theme.iconForFile("notes.xyz", useDefaultIcon = true))
    }

    @Test
    fun `exact file name wins over an extension override`() {
        val overrides = listOf(IconAssociation(AssociationKind.EXTENSION, "json", "kotlin"))
        assertEquals("json", theme.iconForFile("tsconfig.json", overrides))
    }

    @Test
    fun `user extension override wins over the theme extension`() {
        val overrides = listOf(IconAssociation(AssociationKind.EXTENSION, "kt", "typescript"))
        assertEquals("typescript", theme.iconForFile("Main.kt", overrides))
    }

    @Test
    fun `accepts dotted and glob extension patterns`() {
        assertEquals("typescript", theme.iconForFile("Main.kt", listOf(IconAssociation(AssociationKind.EXTENSION, ".kt", "typescript"))))
        assertEquals("typescript", theme.iconForFile("Main.kt", listOf(IconAssociation(AssociationKind.EXTENSION, "*.kt", "typescript"))))
    }

    @Test
    fun `user file name override wins over the theme file name`() {
        val overrides = listOf(IconAssociation(AssociationKind.FILE_NAME, "Dockerfile", "kotlin"))
        assertEquals("kotlin", theme.iconForFile("Dockerfile", overrides))
    }

    @Test
    fun `resolves folder names case-insensitively`() {
        assertEquals("folder-src", theme.iconForFolder("src"))
        assertEquals("folder-src", theme.iconForFolder("SRC"))
    }

    @Test
    fun `folder override wins over the theme`() {
        val overrides = listOf(IconAssociation(AssociationKind.FOLDER, "src", "folder"))
        assertEquals("folder", theme.iconForFolder("src", overrides))
    }

    @Test
    fun `default folder icon is opt-in`() {
        assertNull(theme.iconForFolder("unknown"))
        assertEquals("folder", theme.iconForFolder("unknown", useDefaultIcon = true))
    }

    @Test
    fun `exposes icon paths by id`() {
        assertEquals("/themes/test/icons/kotlin.svg", theme.iconPath("kotlin"))
        assertNull(theme.iconPath("missing"))
    }
}
