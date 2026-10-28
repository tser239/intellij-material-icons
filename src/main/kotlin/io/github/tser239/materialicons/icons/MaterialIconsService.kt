package io.github.tser239.materialicons.icons

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.IconLoader
import io.github.tser239.materialicons.settings.MaterialIconsSettings
import java.util.concurrent.ConcurrentHashMap
import javax.swing.Icon

/**
 * Application-level entry point for icon resolution.
 *
 * The theme is parsed lazily and exactly once, and the resulting [IconTheme] is immutable. Actual
 * `Icon` instances are not cached here on purpose: `IconLoader` already caches them by path and
 * transparently swaps `_dark`/`@2x` variants when the theme changes. Duplicating that cache would
 * only risk stale icons after a look-and-feel switch.
 */
class MaterialIconsService {

    @Volatile
    private var myTheme: IconTheme? = null
    private val loadLock = Any()

    /** Paths/ids that were already reported, so a broken override cannot flood idea.log. */
    private val reportedProblems = ConcurrentHashMap.newKeySet<String>()

    fun theme(): IconTheme {
        myTheme?.let { return it }
        synchronized(loadLock) {
            myTheme?.let { return it }
            val loaded = IconThemeLoader.load(
                themeId = THEME_ID,
                manifestPath = MANIFEST_PATH,
                classLoader = javaClass.classLoader,
            )
            myTheme = loaded
            LOG.info("Loaded icon theme '${loaded.id}' (${loaded.iconPaths.size} icons)")
            return loaded
        }
    }

    /** Can be called from a background thread to move the one-time JSON parse off the UI thread. */
    fun warmUp() {
        theme()
    }

    fun iconForFile(fileName: String): Icon? {
        val state = MaterialIconsSettings.getInstance().currentState
        if (!state.enabled) return null

        val theme = theme()
        val iconId = theme.iconForFile(
            fileName = fileName,
            overrides = state.associations,
            useDefaultIcon = state.applyDefaultIcons,
        ) ?: return null
        return loadIcon(theme, iconId)
    }

    fun iconForFolder(folderName: String): Icon? {
        val state = MaterialIconsSettings.getInstance().currentState
        if (!state.enabled) return null

        val theme = theme()
        val iconId = theme.iconForFolder(
            folderName = folderName,
            overrides = state.associations,
            useDefaultIcon = state.applyDefaultIcons,
        ) ?: return null
        return loadIcon(theme, iconId)
    }

    private fun loadIcon(theme: IconTheme, iconId: String): Icon? {
        val path = theme.iconPath(iconId)
        if (path == null) {
            if (reportedProblems.add("id:$iconId")) {
                LOG.warn("Icon id '$iconId' is not defined in theme '${theme.id}'")
            }
            return null
        }

        val icon = IconLoader.findIcon(path, javaClass)
        if (icon == null && reportedProblems.add("path:$path")) {
            LOG.warn("Icon resource '$path' could not be loaded")
        }
        return icon
    }

    companion object {
        const val THEME_ID: String = "material"
        private const val MANIFEST_PATH: String = "/themes/$THEME_ID/material-icons.json"
        private val LOG: Logger = Logger.getInstance(MaterialIconsService::class.java)

        fun getInstance(): MaterialIconsService = service()
    }
}
