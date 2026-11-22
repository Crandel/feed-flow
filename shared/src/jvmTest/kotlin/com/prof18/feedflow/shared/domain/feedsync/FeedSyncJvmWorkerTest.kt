package com.prof18.feedflow.shared.domain.feedsync

import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.core.model.SyncDownloadError
import com.prof18.feedflow.core.model.SyncUploadError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FeedSyncJvmWorkerTest {

    @Test
    fun `syncDownloadErrorForAccount returns Google Drive error for Google Drive account`() {
        val error = syncDownloadErrorForAccount(SyncAccounts.GOOGLE_DRIVE)

        assertEquals(SyncDownloadError.GoogleDriveDownloadFailed, error)
    }

    @Test
    fun `syncDownloadErrorForAccount returns iCloud error for iCloud account`() {
        val error = syncDownloadErrorForAccount(SyncAccounts.ICLOUD)

        assertEquals(SyncDownloadError.ICloudDownloadFailed, error)
    }

    @Test
    fun `syncDownloadErrorForAccount returns Dropbox error for Dropbox account`() {
        val error = syncDownloadErrorForAccount(SyncAccounts.DROPBOX)

        assertEquals(SyncDownloadError.DropboxDownloadFailed, error)
    }

    @Test
    fun `syncDownloadErrorForAccount returns Folder error for Folder account`() {
        val error = syncDownloadErrorForAccount(SyncAccounts.FOLDER)

        assertEquals(SyncDownloadError.FolderDownloadFailed, error)
    }

    @Test
    fun `folder error codes are unique among upload and download errors`() {
        val uploadCodes = listOf(SyncUploadError.FolderUploadFailed.code, SyncUploadError.FolderNotAccessible.code)
        val downloadCodes =
            listOf(SyncDownloadError.FolderDownloadFailed.code, SyncDownloadError.FolderNotAccessible.code)

        assertEquals(listOf("SU6", "SU7"), uploadCodes)
        assertEquals(listOf("SD5", "SD6"), downloadCodes)
        assertTrue(uploadCodes.toSet().intersect(downloadCodes.toSet()).isEmpty())
    }
}
