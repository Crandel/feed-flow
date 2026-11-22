package com.prof18.feedflow.feedsync.folder

import android.content.Context
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.CloudBackupNotFoundException
import java.io.File
import java.io.IOException

/**
 * SAF tree transport for the Android Folder provider.
 *
 * SAF has no atomic replace: the upload writes the tmp child first, deletes the old final
 * document and renames the tmp child into place. Between delete and rename the final name is
 * briefly missing, but it never holds a partly written file. Providers without rename support
 * fall back to a direct write.
 */
class FolderSyncDataSourceAndroid private constructor(
    private val store: FolderDocumentStore,
    private val folderSettings: FolderSettings,
    private val logger: Logger,
) : FolderSyncDataSource {
    internal constructor(
        store: FolderDocumentStore,
        folderSettings: FolderSettings,
    ) : this(store, folderSettings, Logger.withTag(TAG))

    constructor(
        context: Context,
        folderSettings: FolderSettings,
        logger: Logger,
    ) : this(DocumentsContractFolderDocumentStore(context), folderSettings, logger)

    override fun isLinked(): Boolean = folderSettings.getFolderLocation() != null

    override suspend fun checkAccess() {
        requireAccess()
    }

    override suspend fun upload(
        source: String,
        fileName: String,
    ) {
        val treeUri = requireAccess()
        val tmpName = tmpFileName(fileName)
        val sourceFile = File(source)
        try {
            val tmpUri =
                store.findDocument(treeUri, tmpName)
                    ?: store.createDocument(treeUri, tmpName)
                    ?: throw FolderNotAccessibleException()
            store.writeDocument(tmpUri, WRITE_MODE_TRUNCATE, sourceFile)
            store.findDocument(treeUri, fileName)?.let(store::deleteDocument)
            val renamed = rename(tmpUri, fileName)
            if (renamed == null) {
                writeDirectFallback(treeUri, sourceFile, fileName)
            }
        } catch (_: IOException) {
            throw FolderNotAccessibleException()
        }
    }

    override suspend fun download(
        fileName: String,
        destination: String,
    ) {
        val treeUri = requireAccess()
        val documentUri =
            store.findDocument(treeUri, fileName)
                ?: throw CloudBackupNotFoundException()
        try {
            store.readDocument(documentUri, File(destination))
        } catch (_: IOException) {
            throw FolderNotAccessibleException()
        }
    }

    /** Drops the persistable URI permission. The backup file in the folder is left untouched. */
    override fun releaseAccess() {
        folderSettings.getFolderLocation()?.let(store::releasePersistedAccess)
    }

    private fun requireAccess(): String {
        val treeUri = folderSettings.getFolderLocation() ?: throw FolderNotAccessibleException()
        if (!store.hasPersistedAccess(treeUri) || !store.isTreeQueryable(treeUri)) {
            throw FolderNotAccessibleException()
        }
        return treeUri
    }

    private fun rename(
        documentUri: String,
        displayName: String,
    ): String? =
        try {
            store.renameDocument(documentUri, displayName)
        } catch (_: IOException) {
            null
        }

    private fun writeDirectFallback(
        treeUri: String,
        sourceFile: File,
        fileName: String,
    ) {
        logger.d { "$TAG: the document provider does not support renameDocument. Writing $fileName directly." }
        val tmpUri = store.findDocument(treeUri, tmpFileName(fileName))
        tmpUri?.let(store::deleteDocument)
        val finalUri =
            store.findDocument(treeUri, fileName)
                ?: store.createDocument(treeUri, fileName)
                ?: throw FolderNotAccessibleException()
        store.writeDocument(finalUri, WRITE_MODE_TRUNCATE, sourceFile)
    }

    private fun tmpFileName(fileName: String) = "$TMP_PREFIX$fileName$TMP_SUFFIX"

    private companion object {
        const val TAG = "FolderSyncDataSourceAndroid"
        const val TMP_PREFIX = "."
        const val TMP_SUFFIX = ".tmp"
        const val WRITE_MODE_TRUNCATE = "wt"
    }
}
