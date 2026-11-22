package com.prof18.feedflow.feedsync.folder

import java.io.File

/** In-memory stand-in for the SAF document tree used to verify the write order of the transport. */
internal class FakeFolderDocumentStore : FolderDocumentStore {
    var accessGranted = true
    var treeQueryable = true
    var supportsRename = true
    val calls = mutableListOf<String>()
    val documents = linkedMapOf<String, String>()

    override fun hasPersistedAccess(treeUri: String): Boolean = accessGranted

    override fun releasePersistedAccess(treeUri: String) {
        calls += "release"
        accessGranted = false
    }

    override fun isTreeQueryable(treeUri: String): Boolean = treeQueryable

    override fun findDocument(
        treeUri: String,
        displayName: String,
    ): String? {
        calls += "find:$displayName"
        return if (documents.containsKey(displayName)) documentUri(displayName) else null
    }

    override fun createDocument(
        treeUri: String,
        displayName: String,
    ): String? {
        calls += "create:$displayName"
        documents[displayName] = ""
        return documentUri(displayName)
    }

    override fun writeDocument(
        documentUri: String,
        mode: String,
        source: File,
    ) {
        val name = nameOf(documentUri)
        calls += "write:$name:$mode"
        documents[name] = source.readText()
    }

    override fun readDocument(
        documentUri: String,
        destination: File,
    ) {
        val name = nameOf(documentUri)
        calls += "read:$name"
        destination.writeText(documents.getValue(name))
    }

    override fun deleteDocument(documentUri: String): Boolean {
        val name = nameOf(documentUri)
        calls += "delete:$name"
        return documents.remove(name) != null
    }

    override fun renameDocument(
        documentUri: String,
        displayName: String,
    ): String? {
        val name = nameOf(documentUri)
        calls += "rename:$name->$displayName"
        if (!supportsRename) return null
        val content = documents.remove(name) ?: return null
        documents[displayName] = content
        return documentUri(displayName)
    }

    private fun documentUri(displayName: String) = "content://com.example.provider/document/$displayName"

    private fun nameOf(documentUri: String) = documentUri.substringAfterLast('/')
}
