package io.github.tser239.materialicons.icons

/**
 * What a user-defined association matches against.
 *
 * Kept as a separate enum (and not as a sealed class) so that IntelliJ's XML state
 * serializer can persist it without a custom converter.
 */
enum class AssociationKind {
    FILE_NAME,
    EXTENSION,
    FOLDER;

    /** Short form used in the settings text format and in the UI. */
    val prefix: String
        get() = when (this) {
            FILE_NAME -> "file"
            EXTENSION -> "extension"
            FOLDER -> "folder"
        }

    companion object {
        /**
         * Parses a kind from the settings text format. Accepts a few human-friendly aliases.
         */
        fun parse(value: String): AssociationKind? = when (value.trim().lowercase()) {
            "file", "filename", "file-name", "file_name" -> FILE_NAME
            "extension", "ext" -> EXTENSION
            "folder", "dir", "directory" -> FOLDER
            else -> null
        }
    }
}
