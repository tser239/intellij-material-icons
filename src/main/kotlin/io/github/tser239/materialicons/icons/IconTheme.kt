package io.github.tser239.materialicons.icons

/**
 * An immutable, fully indexed icon theme.
 *
 * This class deliberately has **no IntelliJ platform dependencies**: it is pure lookup logic and
 * can be unit-tested without starting an IDE. All maps are normalised once at construction time so
 * that resolving an icon is a couple of hash lookups with no file I/O, PSI access or index access.
 *
 * Precedence for files:
 * 1. user association matching the exact file name
 * 2. theme association matching the exact file name
 * 3. user association matching the longest file extension (`spec.ts` before `ts`)
 * 4. theme association matching the longest file extension
 * 5. the theme's default file icon (only when explicitly enabled)
 */
class IconTheme(
    val id: String,
    /** Icon id -> classpath resource path, e.g. `kotlin` -> `/themes/material/icons/kotlin.svg`. */
    val iconPaths: Map<String, String>,
    fileNames: Map<String, String> = emptyMap(),
    fileExtensions: Map<String, String> = emptyMap(),
    folderNames: Map<String, String> = emptyMap(),
    val defaultFileIconId: String? = null,
    val defaultFolderIconId: String? = null,
) {
    private val fileNamesByLowerCaseName = fileNames.lowercaseKeys()
    private val extensionsByLowerCaseExtension = fileExtensions.lowercaseKeys()
    private val folderNamesByLowerCaseName = folderNames.lowercaseKeys()

    fun iconPath(iconId: String): String? = iconPaths[iconId]

    fun iconForFile(
        fileName: String,
        overrides: List<IconAssociation> = emptyList(),
        useDefaultIcon: Boolean = false,
    ): String? {
        val lowerCaseName = fileName.lowercase()

        // Exact file names win over extensions, both for the theme and for user overrides.
        for (override in overrides) {
            if (override.kind == AssociationKind.FILE_NAME &&
                override.iconId.isNotEmpty() &&
                override.pattern.equals(fileName, ignoreCase = true)
            ) {
                return override.iconId
            }
        }
        fileNamesByLowerCaseName[lowerCaseName]?.let { return it }

        // Walk the dots from left to right so that multi-part extensions ("spec.ts") are tried
        // before their suffix ("ts"). Leading dots are ignored, so ".gitignore" has no extension.
        var searchFrom = if (lowerCaseName.startsWith('.')) 1 else 0
        while (searchFrom < lowerCaseName.length) {
            val dot = lowerCaseName.indexOf('.', searchFrom)
            if (dot < 0) break
            val extension = lowerCaseName.substring(dot + 1)
            if (extension.isNotEmpty()) {
                for (override in overrides) {
                    if (override.kind == AssociationKind.EXTENSION &&
                        override.iconId.isNotEmpty() &&
                        override.extensionPattern() == extension
                    ) {
                        return override.iconId
                    }
                }
                extensionsByLowerCaseExtension[extension]?.let { return it }
            }
            searchFrom = dot + 1
        }

        return if (useDefaultIcon) defaultFileIconId else null
    }

    fun iconForFolder(
        folderName: String,
        overrides: List<IconAssociation> = emptyList(),
        useDefaultIcon: Boolean = false,
    ): String? {
        for (override in overrides) {
            if (override.kind == AssociationKind.FOLDER &&
                override.iconId.isNotEmpty() &&
                override.pattern.equals(folderName, ignoreCase = true)
            ) {
                return override.iconId
            }
        }

        folderNamesByLowerCaseName[folderName.lowercase()]?.let { return it }

        return if (useDefaultIcon) defaultFolderIconId else null
    }
}

/**
 * Normalises `.kt`, `*.kt` and `kt` to `kt` so users do not have to remember the exact notation.
 */
private fun IconAssociation.extensionPattern(): String =
    pattern.trim().removePrefix("*.").removePrefix(".").lowercase()

private fun Map<String, String>.lowercaseKeys(): Map<String, String> {
    if (isEmpty()) return emptyMap()
    val result = HashMap<String, String>(size * 2)
    for ((key, value) in this) {
        result[key.lowercase()] = value
    }
    return result
}
