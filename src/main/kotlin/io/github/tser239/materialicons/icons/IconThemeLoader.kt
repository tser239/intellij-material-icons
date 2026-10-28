package io.github.tser239.materialicons.icons

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * Reads the subset of the Material Icon Theme manifest format that the plugin understands.
 *
 * The format is intentionally compatible with the upstream `material-icons.json`, so the full
 * theme can be dropped into `resources/themes/<id>/` without a conversion step. Sections that do
 * not apply to an IDE (VSCode `languageIds`, `light`/`highContrast` overrides, root folder names,
 * expanded folder names, file nesting) are ignored: they would only cost parse time and memory
 * without changing a single icon.
 */
object IconThemeLoader {

    /**
     * @param themeId directory name below `resources/themes/`, also used to resolve icon paths
     * @param manifestPath classpath path of the manifest, e.g. `/themes/material/material-icons.json`
     */
    fun load(themeId: String, manifestPath: String, classLoader: ClassLoader): IconTheme {
        val stream = classLoader.getResourceAsStream(manifestPath.removePrefix("/"))
            ?: throw IllegalStateException("Icon theme manifest not found on the classpath: $manifestPath")

        val root = stream.use {
            JsonParser.parseReader(InputStreamReader(it, StandardCharsets.UTF_8)).asJsonObject
        }

        val iconPaths = HashMap<String, String>()
        for ((iconId, definition) in root.objectOrEmpty("iconDefinitions").entrySet()) {
            val iconPath = definition.asJsonObjectOrNull()?.stringOrNull("iconPath") ?: continue
            iconPaths[iconId] = toClasspathIconPath(themeId, iconPath)
        }
        check(iconPaths.isNotEmpty()) { "Icon theme '$themeId' does not define any icons: $manifestPath" }

        return IconTheme(
            id = themeId,
            iconPaths = iconPaths,
            fileNames = root.stringMap("fileNames"),
            fileExtensions = root.stringMap("fileExtensions"),
            folderNames = root.stringMap("folderNames"),
            defaultFileIconId = root.stringOrNull("file"),
            defaultFolderIconId = root.stringOrNull("folder"),
        )
    }

    /**
     * The upstream manifest stores paths relative to its own `dist/` directory (`./../icons/x.svg`)
     * and keeps all icons in one flat directory. Resolving by file name keeps the plugin
     * independent of the upstream layout and supports both `./icons/x.svg` and `./../icons/x.svg`.
     */
    private fun toClasspathIconPath(themeId: String, manifestIconPath: String): String {
        val fileName = manifestIconPath.substringAfterLast('/')
        return "/themes/$themeId/icons/$fileName"
    }

    private fun JsonObject.stringOrNull(name: String): String? {
        val value = get(name) ?: return null
        return if (value.isJsonPrimitive) value.asString else null
    }

    private fun JsonObject.objectOrEmpty(name: String): JsonObject =
        get(name).asJsonObjectOrNull() ?: JsonObject()

    private fun JsonObject.stringMap(name: String): Map<String, String> {
        val objectValue = objectOrEmpty(name)
        val result = HashMap<String, String>(objectValue.size() * 2)
        for ((key, value) in objectValue.entrySet()) {
            if (value.isJsonPrimitive) {
                result[key] = value.asString
            }
        }
        return result
    }

    private fun com.google.gson.JsonElement?.asJsonObjectOrNull(): JsonObject? =
        if (this != null && isJsonObject) asJsonObject else null
}
