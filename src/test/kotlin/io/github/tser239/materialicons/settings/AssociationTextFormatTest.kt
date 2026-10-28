package io.github.tser239.materialicons.settings

import io.github.tser239.materialicons.icons.AssociationKind
import io.github.tser239.materialicons.icons.IconAssociation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AssociationTextFormatTest {

    @Test
    fun `parses all supported kinds and aliases`() {
        val associations = AssociationTextFormat.parse(
            """
            extension:kt=kotlin
            ext:.ts=typescript
            file:Makefile=make
            filename:.env=gear
            folder:.github=github
            dir:src=folder-src
            """.trimIndent()
        )

        assertEquals(
            listOf(
                IconAssociation(AssociationKind.EXTENSION, "kt", "kotlin"),
                IconAssociation(AssociationKind.EXTENSION, ".ts", "typescript"),
                IconAssociation(AssociationKind.FILE_NAME, "Makefile", "make"),
                IconAssociation(AssociationKind.FILE_NAME, ".env", "gear"),
                IconAssociation(AssociationKind.FOLDER, ".github", "github"),
                IconAssociation(AssociationKind.FOLDER, "src", "folder-src"),
            ),
            associations,
        )
    }

    @Test
    fun `ignores comments blank lines and malformed entries`() {
        val associations = AssociationTextFormat.parse(
            """
            # this is a comment
            extension:kt=kotlin

            not-an-association
            extension:=missing-pattern
            extension:ts=
            :js=javascript
            file:name
            """.trimIndent()
        )

        assertEquals(listOf(IconAssociation(AssociationKind.EXTENSION, "kt", "kotlin")), associations)
    }

    @Test
    fun `round-trips through the text format`() {
        val original = listOf(
            IconAssociation(AssociationKind.FILE_NAME, ".editorconfig", "editorconfig"),
            IconAssociation(AssociationKind.EXTENSION, "rs", "rust"),
            IconAssociation(AssociationKind.FOLDER, "assets", "folder-resource"),
        )

        assertEquals(original, AssociationTextFormat.parse(AssociationTextFormat.format(original)))
    }
}
