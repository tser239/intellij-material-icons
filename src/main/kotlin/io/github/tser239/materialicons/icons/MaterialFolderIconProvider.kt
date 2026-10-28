package io.github.tser239.materialicons.icons

import com.intellij.ide.IconProvider
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.Icon

/**
 * Resolves icons for directories in the Project View.
 *
 * Folders are PSI elements in the Project View, which is why this is an [IconProvider] rather than
 * a [com.intellij.ide.FileIconProvider]. Only [PsiDirectory.getName] is used, so this stays cheap.
 *
 * The platform has no "expanded folder" icon API (the tree only draws a chevron), so open-folder
 * variants are intentionally not emulated here: doing so requires a ProjectViewNodeDecorator plus
 * forced tree refreshes, which is exactly the kind of overhead this plugin avoids.
 */
class MaterialFolderIconProvider : IconProvider(), DumbAware {

    override fun getIcon(element: PsiElement, flags: Int): Icon? {
        val directory = element as? PsiDirectory ?: return null

        return try {
            MaterialIconsService.getInstance().iconForFolder(directory.name)
        } catch (e: Exception) {
            if (failureReported.compareAndSet(false, true)) {
                LOG.warn("Failed to resolve a Material folder icon for '${directory.name}'; further errors are suppressed", e)
            }
            null
        }
    }

    companion object {
        private val LOG = Logger.getInstance(MaterialFolderIconProvider::class.java)
        private val failureReported = AtomicBoolean(false)
    }
}
