package com.prof18.feedflow.feedsync.folder

import com.prof18.feedflow.core.model.CloudBackupNotFoundException
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Plain file-system transport for the desktop Folder provider.
 *
 * Uploads land as `.<name>.tmp` and are moved atomically over the final name, so an interrupted
 * upload never leaves a partly written file under the backup name.
 */
class FolderSyncDataSourceJvm(
    private val folderSettings: FolderSettings,
) : FolderSyncDataSource {
    override fun isLinked(): Boolean = folderSettings.getFolderLocation() != null

    override suspend fun checkAccess() {
        requireAccessibleFolder()
    }

    override suspend fun upload(
        source: String,
        fileName: String,
    ) {
        val folder = requireAccessibleFolder()
        val tmpFile = File(folder, tmpFileName(fileName))
        val finalFile = File(folder, fileName)
        try {
            File(source).inputStream().use { input ->
                tmpFile.outputStream().use(input::copyTo)
            }
            moveOver(tmpFile, finalFile)
        } catch (_: IOException) {
            tmpFile.delete()
            throw FolderNotAccessibleException()
        }
    }

    override suspend fun download(
        fileName: String,
        destination: String,
    ) {
        val folder = requireAccessibleFolder()
        val backupFile = File(folder, fileName)
        if (!backupFile.exists() || backupFile.isDirectory) {
            throw CloudBackupNotFoundException()
        }
        try {
            backupFile.inputStream().use { input ->
                File(destination).outputStream().use(input::copyTo)
            }
        } catch (_: IOException) {
            throw FolderNotAccessibleException()
        }
    }

    override fun releaseAccess() = Unit

    private fun moveOver(
        tmpFile: File,
        finalFile: File,
    ) {
        val tmpPath = tmpFile.toPath()
        val finalPath = finalFile.toPath()
        try {
            Files.move(tmpPath, finalPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            // Network shares and FAT file systems have no atomic rename. A plain replace is the
            // best available option and still keeps the final file either old or new.
            Files.move(tmpPath, finalPath, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun requireAccessibleFolder(): File {
        val location = folderSettings.getFolderLocation() ?: throw FolderNotAccessibleException()
        val folder = File(location)
        if (!folder.isDirectory) {
            throw FolderNotAccessibleException()
        }
        val probe = File(folder, PROBE_FILE_NAME)
        try {
            probe.outputStream().use { it.write(0) }
        } catch (_: IOException) {
            throw FolderNotAccessibleException()
        } finally {
            probe.delete()
        }
        return folder
    }

    private fun tmpFileName(fileName: String) = "$TMP_PREFIX$fileName$TMP_SUFFIX"

    companion object {
        const val TMP_PREFIX = "."
        const val TMP_SUFFIX = ".tmp"
        const val PROBE_FILE_NAME = ".feedflow-access-check"
    }
}
