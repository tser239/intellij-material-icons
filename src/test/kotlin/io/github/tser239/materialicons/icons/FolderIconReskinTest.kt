package io.github.tser239.materialicons.icons

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Guards the folder re-skin performed by `tools/reskin_folders.py`.
 *
 * The icons in `themes/material/icons/` are upstream artwork with IntelliJ's folder body swapped in,
 * so these tests check the three properties the generator must uphold: every selectable folder uses
 * the IntelliJ silhouette, every one of them has a dark twin, and the artwork is re-fitted into the
 * shared box instead of keeping its upstream position.
 */
class FolderIconReskinTest {

    private val manifest = javaClass.getResourceAsStream("/themes/material/material-icons.json")!!
        .use { JsonParser.parseReader(it.reader()).asJsonObject }

    private val definitions = manifest.getAsJsonObject("iconDefinitions")

    private val folderIds: List<String> = (
        manifest.getAsJsonObject("folderNames").entrySet().map { it.value.asString } +
            manifest.get("folder").asString
        ).distinct().sorted()

    private fun iconPath(iconId: String): String {
        val upstreamPath = definitions.getAsJsonObject(iconId).get("iconPath").asString
        return "/themes/material/icons/" + upstreamPath.substringAfterLast('/')
    }

    private fun read(path: String): String {
        val stream = javaClass.getResourceAsStream(path)
        assertNotNull(stream, "missing icon resource $path")
        return stream!!.use { it.reader().readText() }
    }

    @Test
    fun `every selectable folder uses the IntelliJ folder silhouette`() {
        val upstreamBody = folderIds.filter { "m6.922 3.768" in read(iconPath(it)) }
        assertTrue(upstreamBody.isEmpty(), "folders still using the upstream body: $upstreamBody")

        val missingBody = folderIds.filter { "M8.10584 4.34613" !in read(iconPath(it)) }
        assertTrue(missingBody.isEmpty(), "folders without the IntelliJ body: $missingBody")
    }

    @Test
    fun `every folder icon has a dark variant`() {
        val missing = folderIds.filter {
            val dark = iconPath(it).replace(".svg", "_dark.svg")
            javaClass.getResource(dark) == null
        }
        assertTrue(missing.isEmpty(), "folders without a dark variant: $missing")

        val identical = folderIds.filter {
            val dark = read(iconPath(it).replace(".svg", "_dark.svg"))
            dark == read(iconPath(it))
        }
        assertTrue(identical.isEmpty(), "folders whose dark variant is identical: $identical")
    }

    @Test
    fun `folder artwork is fitted into the shared box`() {
        // the generic folder has no artwork, only the body
        val withArtwork = folderIds.filter { it != "folder" }
        val fitted = Regex("""<g transform="translate\([^)]*\) scale\([0-9.]+\)">""")
        val unfitted = withArtwork.filter { !fitted.containsMatchIn(read(iconPath(it))) }
        assertTrue(unfitted.isEmpty(), "folders with unfitted artwork: $unfitted")
    }
}
