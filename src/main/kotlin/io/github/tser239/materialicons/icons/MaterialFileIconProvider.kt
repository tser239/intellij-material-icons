package io.github.tser239.materialicons.icons

import com.intellij.ide.FileIconProvider
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.Icon

/**
 * Resolves icons for files (and only files) in the Project View, editor tabs, search results and
 * anywhere else the platform asks for a `VirtualFile` icon.
 *
 * This provider performs no PSI access, no index access and no I/O: everything it needs is the
 * file name and an immutable lookup table. It is therefore safe in dumb mode and ignores the
 * `FAST_ONLY` flag introduced in 2026.1.
 */
class MaterialFileIconProvider : FileIconProvider {

    override fun getIcon(file: VirtualFile, flags: Int, project: Project?): Icon? {
        // Directories are handled by MaterialFolderIconProvider, which wins before this provider.
        if (file.isDirectory) return null

        return try {
            MaterialIconsService.getInstance().iconForFile(file.name)
        } catch (e: Exception) {
            // A provider that throws makes the platform log a PluginException for every icon.
            // Report once and then fail silently so a broken theme cannot flood the log.
            if (failureReported.compareAndSet(false, true)) {
                LOG.warn("Failed to resolve a Material icon for '${file.name}'; further errors are suppressed", e)
            }
            null
        }
    }

    companion object {
        private val LOG = Logger.getInstance(MaterialFileIconProvider::class.java)
        private val failureReported = AtomicBoolean(false)
    }
}
