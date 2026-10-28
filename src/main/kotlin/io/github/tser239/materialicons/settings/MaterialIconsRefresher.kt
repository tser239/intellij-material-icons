package io.github.tser239.materialicons.settings

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.IconLoader

/**
 * Re-applies icons after the user explicitly changes settings.
 *
 * `IconLoader.clearCache()` is intentionally only called here and never from an icon provider.
 * Clearing the platform icon cache is a relatively expensive, global operation; doing it on every
 * tree update is what makes icon plugins slow.
 */
object MaterialIconsRefresher {

    fun refresh() {
        IconLoader.clearCache()

        for (project in ProjectManager.getInstance().openProjects) {
            if (project.isDisposed) continue
            try {
                ProjectView.getInstance(project).refresh()
            } catch (e: Exception) {
                LOG.warn("Could not refresh the Project View of '${project.name}'", e)
            }
        }
    }

    private val LOG: Logger = Logger.getInstance(MaterialIconsRefresher::class.java)
}
