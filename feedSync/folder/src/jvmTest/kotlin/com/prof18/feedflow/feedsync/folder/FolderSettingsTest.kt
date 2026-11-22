package com.prof18.feedflow.feedsync.folder

import com.prof18.feedflow.feedsync.dropbox.DropboxSettings
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveSettings
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FolderSettingsTest {
    private val settings: Settings = MapSettings()
    private val folderSettings = FolderSettings(settings)

    @Test
    fun `location is null until set and is cleared afterwards`() {
        assertNull(folderSettings.getFolderLocation())

        folderSettings.setFolderLocation("/home/user/Syncthing")
        assertEquals("/home/user/Syncthing", folderSettings.getFolderLocation())

        folderSettings.setFolderLocation("content://provider/tree/primary")
        assertEquals("content://provider/tree/primary", folderSettings.getFolderLocation())

        folderSettings.clearAll()
        assertNull(folderSettings.getFolderLocation())
    }

    @Test
    fun `timestamps are null until set and are cleared afterwards`() {
        assertNull(folderSettings.getLastUploadTimestamp())
        assertNull(folderSettings.getLastDownloadTimestamp())

        folderSettings.setLastUploadTimestamp(111L)
        folderSettings.setLastDownloadTimestamp(222L)
        assertEquals(111L, folderSettings.getLastUploadTimestamp())
        assertEquals(222L, folderSettings.getLastDownloadTimestamp())

        folderSettings.clearAll()
        assertNull(folderSettings.getLastUploadTimestamp())
        assertNull(folderSettings.getLastDownloadTimestamp())
    }

    @Test
    fun `clearAll removes only the three folder keys`() {
        folderSettings.setFolderLocation("/tmp/folder")
        folderSettings.setLastUploadTimestamp(1L)
        folderSettings.setLastDownloadTimestamp(2L)
        assertEquals(
            setOf(
                "FOLDER_SYNC_LOCATION",
                "FOLDER_SYNC_LAST_UPLOAD_TIMESTAMP",
                "FOLDER_SYNC_LAST_DOWNLOAD_TIMESTAMP",
            ),
            settings.keys,
        )

        folderSettings.clearAll()
        assertTrue(settings.keys.isEmpty())
    }

    @Test
    fun `keys do not collide with Dropbox or Google Drive keys`() {
        val dropboxSettings = DropboxSettings(settings)
        val googleDriveSettings = GoogleDriveSettings(settings)

        folderSettings.setFolderLocation("/tmp/folder")
        folderSettings.setLastUploadTimestamp(1L)
        folderSettings.setLastDownloadTimestamp(2L)

        assertNull(dropboxSettings.getDropboxData())
        assertNull(dropboxSettings.getLastUploadTimestamp())
        assertNull(dropboxSettings.getLastDownloadTimestamp())
        assertFalse(googleDriveSettings.isGoogleDriveLinked())
        assertNull(googleDriveSettings.getBackupFileId())
        assertNull(googleDriveSettings.getLastUploadTimestamp())

        dropboxSettings.setDropboxData("some-data")
        dropboxSettings.setLastUploadTimestamp(9L)
        googleDriveSettings.setGoogleDriveLinked(true)
        googleDriveSettings.setBackupFileId("file-id")

        folderSettings.clearAll()

        assertEquals("some-data", dropboxSettings.getDropboxData())
        assertEquals(9L, dropboxSettings.getLastUploadTimestamp())
        assertTrue(googleDriveSettings.isGoogleDriveLinked())
        assertEquals("file-id", googleDriveSettings.getBackupFileId())
    }
}
