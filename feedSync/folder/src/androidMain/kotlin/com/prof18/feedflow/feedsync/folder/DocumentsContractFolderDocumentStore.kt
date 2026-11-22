package com.prof18.feedflow.feedsync.folder

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File
import java.io.FileNotFoundException

/** Real [FolderDocumentStore] backed by [DocumentsContract] and the application [ContentResolver]. */
internal class DocumentsContractFolderDocumentStore(
    context: Context,
) : FolderDocumentStore {
    private val contentResolver: ContentResolver = context.contentResolver

    override fun hasPersistedAccess(treeUri: String): Boolean =
        contentResolver.persistedUriPermissions.any {
            it.uri.toString() == treeUri && it.isReadPermission && it.isWritePermission
        }

    override fun releasePersistedAccess(treeUri: String) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            contentResolver.releasePersistableUriPermission(Uri.parse(treeUri), flags)
        } catch (_: SecurityException) {
            // The permission was already revoked. Unlinking must not fail because of it.
        }
    }

    override fun isTreeQueryable(treeUri: String): Boolean =
        try {
            contentResolver.query(
                childrenUri(treeUri),
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                null,
                null,
                null,
            ) != null
        } catch (_: Exception) {
            // A dead provider surfaces as IllegalArgumentException, FileNotFoundException or
            // IllegalStateException depending on how the tree went away.
            false
        }

    override fun findDocument(
        treeUri: String,
        displayName: String,
    ): String? {
        val cursor =
            contentResolver.query(
                childrenUri(treeUri),
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                ),
                null,
                null,
                null,
            ) ?: return null
        cursor.use {
            val idIndex = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (it.moveToNext()) {
                if (it.getString(nameIndex) == displayName) {
                    return DocumentsContract
                        .buildDocumentUriUsingTree(Uri.parse(treeUri), it.getString(idIndex))
                        .toString()
                }
            }
        }
        return null
    }

    override fun createDocument(
        treeUri: String,
        displayName: String,
    ): String? =
        DocumentsContract
            .createDocument(
                contentResolver,
                treeDocumentUri(treeUri),
                DOCUMENT_MIME_TYPE,
                displayName,
            )?.toString()

    override fun writeDocument(
        documentUri: String,
        mode: String,
        source: File,
    ) {
        val output =
            contentResolver.openOutputStream(Uri.parse(documentUri), mode)
                ?: throw FileNotFoundException("Cannot open $documentUri for writing")
        output.use { source.inputStream().use { input -> input.copyTo(output) } }
    }

    override fun readDocument(
        documentUri: String,
        destination: File,
    ) {
        val input =
            contentResolver.openInputStream(Uri.parse(documentUri))
                ?: throw FileNotFoundException("Cannot open $documentUri for reading")
        input.use { destination.outputStream().use { output -> input.copyTo(output) } }
    }

    override fun deleteDocument(documentUri: String): Boolean {
        val uri = Uri.parse(documentUri)
        return DocumentsContract.deleteDocument(contentResolver, uri)
    }

    override fun renameDocument(
        documentUri: String,
        displayName: String,
    ): String? {
        val renamed = DocumentsContract.renameDocument(contentResolver, Uri.parse(documentUri), displayName)
        return renamed?.toString()
    }

    private fun childrenUri(treeUri: String): Uri {
        val uri = Uri.parse(treeUri)
        return DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
    }

    private fun treeDocumentUri(treeUri: String): Uri {
        val uri = Uri.parse(treeUri)
        return DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
    }

    private companion object {
        const val DOCUMENT_MIME_TYPE = "application/octet-stream"
    }
}
