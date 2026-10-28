package io.github.tser239.materialicons

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The plugin redistributes three licensed works, so the license texts must ship with it.
 *
 * The upstream MIT text lives next to the artwork it covers and is restored by
 * `tools/import-material-theme.sh`. The Apache-2.0 text covers the IntelliJ folder silhouette and
 * the bundled Gson library, so it sits outside the imported theme directory, which the importer
 * deletes and recreates. The plugin's own MIT text is copied from the repository root by the
 * build.
 */
class LicenseResourcesTest {

    private fun read(path: String): String {
        val stream = javaClass.getResourceAsStream(path)
        assertNotNull(stream, "missing license resource $path")
        return stream!!.use { it.reader().readText() }
    }

    @Test
    fun `upstream MIT license is packaged with the theme`() {
        val license = read("/themes/material/LICENSE")
        assertTrue("The MIT License" in license, "theme license is not MIT")
        assertTrue("Material Extensions" in license, "theme license lost its copyright holder")
    }

    @Test
    fun `plugin license is packaged with the plugin`() {
        val license = read("/licenses/MIT.txt")
        assertTrue("MIT License" in license, "plugin license is not MIT")
        assertTrue("tser239" in license, "plugin license lost its copyright holder")
    }

    @Test
    fun `Apache license text is packaged with the plugin`() {
        val license = read("/licenses/APACHE-2.0.txt")
        assertTrue("Apache License" in license, "not the Apache license")
        assertTrue("Version 2.0, January 2004" in license, "unexpected Apache license version")
        assertTrue(license.length > 10_000, "Apache license text looks truncated")
    }
}
