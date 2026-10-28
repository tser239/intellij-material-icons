package io.github.tser239.materialicons.settings

import io.github.tser239.materialicons.icons.AssociationKind
import io.github.tser239.materialicons.icons.IconAssociation

/**
 * Line-based text format used by the settings page:
 *
 * ```
 * # kind:pattern=icon-id
 * extension:kt=kotlin
 * file:Makefile=make
 * folder:.github=github
 * ```
 *
 * Pure functions, unit-tested without the platform.
 */
object AssociationTextFormat {

    fun parse(text: String): List<IconAssociation> {
        val result = ArrayList<IconAssociation>()
        for (rawLine in text.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) continue

            val equalsIndex = line.lastIndexOf('=')
            if (equalsIndex <= 0 || equalsIndex == line.lastIndex) continue

            val iconId = line.substring(equalsIndex + 1).trim()
            if (iconId.isEmpty()) continue

            val left = line.substring(0, equalsIndex)
            val colonIndex = left.indexOf(':')
            if (colonIndex <= 0) continue

            val kind = AssociationKind.parse(left.substring(0, colonIndex)) ?: continue
            val pattern = left.substring(colonIndex + 1).trim()
            if (pattern.isEmpty()) continue

            result.add(IconAssociation(kind, pattern, iconId))
        }
        return result
    }

    fun format(associations: List<IconAssociation>): String =
        associations.joinToString(separator = "\n") { "${it.kind.prefix}:${it.pattern}=${it.iconId}" }
}
