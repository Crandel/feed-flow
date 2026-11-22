package com.prof18.feedflow.feedsync.folder

/**
 * Thrown when the linked folder no longer exists, is not writable, or access to it was revoked.
 */
class FolderNotAccessibleException : RuntimeException("Folder not accessible")
