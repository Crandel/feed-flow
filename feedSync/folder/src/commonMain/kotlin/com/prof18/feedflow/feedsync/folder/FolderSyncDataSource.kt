package com.prof18.feedflow.feedsync.folder

import com.prof18.feedflow.core.model.CloudBackupNotFoundException

/**
 * Copies the sync database between local staging files and the user-chosen folder.
 *
 * Paths are plain file-system paths on desktop and are read through content streams on Android;
 * SQLite never opens a file inside the folder.
 */
interface FolderSyncDataSource {
    /** True while a folder location is linked. */
    fun isLinked(): Boolean

    /**
     * Verifies the folder still exists, is writable and reachable.
     * Throws [FolderNotAccessibleException] otherwise.
     */
    suspend fun checkAccess()

    /**
     * Writes [source] (a local staging file path) into the folder as [fileName], never leaving a
     * partly written file under the final name.
     * Throws [FolderNotAccessibleException] when the folder cannot be written.
     */
    suspend fun upload(
        source: String,
        fileName: String,
    )

    /**
     * Copies the backup [fileName] from the folder into the local staging file at [destination].
     * Throws [CloudBackupNotFoundException] when the folder holds no backup file, and
     * [FolderNotAccessibleException] when the folder cannot be read.
     */
    suspend fun download(
        fileName: String,
        destination: String,
    )

    /** Drops any platform-held claim on the folder (persistable URI permission on Android). */
    fun releaseAccess()
}
