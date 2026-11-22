package com.prof18.feedflow.feedsync.folder

import java.io.File

/**
 * Seam over the SAF document tree behind [FolderSyncDataSourceAndroid].
 *
 * Document locations are plain URI strings so the copy, delete and rename order stays testable
 * without a content provider.
 */
internal interface FolderDocumentStore {
    /** True while the tree URI still holds a persisted read and write permission. */
    fun hasPersistedAccess(treeUri: String): Boolean

    /** Releases the persistable permission; a permission that is already gone is ignored. */
    fun releasePersistedAccess(treeUri: String)

    /** False once the document provider behind the tree no longer answers queries. */
    fun isTreeQueryable(treeUri: String): Boolean

    fun findDocument(
        treeUri: String,
        displayName: String,
    ): String?

    fun createDocument(
        treeUri: String,
        displayName: String,
    ): String?

    fun writeDocument(
        documentUri: String,
        mode: String,
        source: File,
    )

    fun readDocument(
        documentUri: String,
        destination: File,
    )

    fun deleteDocument(documentUri: String): Boolean

    fun renameDocument(
        documentUri: String,
        displayName: String,
    ): String?
}
