package com.prof18.feedflow.feedsync.folder

import com.russhwolf.settings.Settings

class FolderSettings(
    private val settings: Settings,
) {
    /** Stores either an absolute file-system path (desktop) or a SAF tree URI string (Android). */
    fun setFolderLocation(location: String) =
        settings.putString(
            FolderSettingsFields.FOLDER_SYNC_LOCATION.name,
            location,
        )

    fun getFolderLocation(): String? = settings.getStringOrNull(FolderSettingsFields.FOLDER_SYNC_LOCATION.name)

    fun setLastUploadTimestamp(timestamp: Long) =
        settings.putLong(
            FolderSettingsFields.FOLDER_SYNC_LAST_UPLOAD_TIMESTAMP.name,
            timestamp,
        )

    fun getLastUploadTimestamp(): Long? =
        settings.getLongOrNull(
            FolderSettingsFields.FOLDER_SYNC_LAST_UPLOAD_TIMESTAMP.name,
        )

    fun setLastDownloadTimestamp(timestamp: Long) =
        settings.putLong(
            FolderSettingsFields.FOLDER_SYNC_LAST_DOWNLOAD_TIMESTAMP.name,
            timestamp,
        )

    fun getLastDownloadTimestamp(): Long? =
        settings.getLongOrNull(
            FolderSettingsFields.FOLDER_SYNC_LAST_DOWNLOAD_TIMESTAMP.name,
        )

    fun clearAll() {
        FolderSettingsFields.entries.forEach {
            settings.remove(it.name)
        }
    }

    // The FOLDER_SYNC_ prefix keeps the keys unique in the Settings instance shared by all providers.
    private enum class FolderSettingsFields {
        FOLDER_SYNC_LOCATION,
        FOLDER_SYNC_LAST_UPLOAD_TIMESTAMP,
        FOLDER_SYNC_LAST_DOWNLOAD_TIMESTAMP,
    }
}
