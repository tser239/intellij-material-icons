package io.github.tser239.materialicons.icons

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Parses the icon theme once on a background thread after a project opens, so the first icon
 * request does not pay for the JSON parse on the UI thread.
 *
 * This is deliberately a one-shot, idempotent warm-up: [MaterialIconsService.theme] is synchronized
 * and caches the result, so opening more projects (or having no project at all) costs nothing.
 */
class MaterialIconsPreloader : ProjectActivity {

    override suspend fun execute(project: Project) {
        withContext(Dispatchers.IO) {
            try {
                MaterialIconsService.getInstance().warmUp()
            } catch (e: Exception) {
                // A broken theme is reported here once; the icon providers then fail silently.
                LOG.warn("Could not preload the Material icon theme", e)
            }
        }
    }

    private companion object {
        private val LOG: Logger = Logger.getInstance(MaterialIconsPreloader::class.java)
    }
}
