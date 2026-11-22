package com.prof18.feedflow.shared.domain.feedsync

import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.core.model.SyncDownloadError
import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.core.model.SyncUploadError
import com.prof18.feedflow.core.utils.FeedSyncMessageQueue
import com.prof18.feedflow.feedsync.folder.FolderSettings
import com.prof18.feedflow.feedsync.folder.FolderSyncDataSource
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.cloudsync.CloudDevice
import com.prof18.feedflow.shared.test.cloudsync.CloudProvider
import com.prof18.feedflow.shared.test.cloudsync.CloudStore
import com.prof18.feedflow.shared.test.cloudsync.JvmFolderTransport
import com.prof18.feedflow.shared.test.cloudsync.createCloudDevice
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FeedSyncJvmWorkerTest : KoinTestBase() {
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

    @Test
    fun `folder upload stores the backup and records the sync timestamps`() =
        runTest(testDispatcher) {
            val store = CloudStore()
            val device = createCloudDevice(CloudProvider.FOLDER, store, "worker-folder")
            val outcomes = mutableListOf<SyncResult>()
            val collector =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    device.application.koin
                        .get<FeedSyncMessageQueue>()
                        .messageQueue
                        .collect { outcomes.add(it) }
                }
            try {
                device.seed()
                device.backup()

                assertEquals(1, store.fileCount(CloudProvider.FOLDER))
                assertTrue(outcomes.isNotEmpty())
                assertTrue(outcomes.all { !it.isError() }, "Unexpected sync outcomes: $outcomes")
                assertNotNull(
                    device.application.koin
                        .get<FolderSettings>()
                        .getLastUploadTimestamp(),
                )

                val downloadResult =
                    device.application.koin
                        .get<FeedSyncWorker>()
                        .download(isFirstSync = false)

                assertTrue(downloadResult.isSuccess())
                assertNotNull(
                    device.application.koin
                        .get<FolderSettings>()
                        .getLastDownloadTimestamp(),
                )
            } finally {
                collector.cancel()
                device.close()
            }
        }

    @Test
    fun `folder download without a backup reports backup not found and keeps local data`() =
        runTest(testDispatcher) {
            val store = CloudStore()
            val device = createCloudDevice(CloudProvider.FOLDER, store, "worker-empty")
            try {
                device.seed()

                val result =
                    device.application.koin
                        .get<FeedSyncWorker>()
                        .download(isFirstSync = false)

                assertIs<SyncResult.BackupNotFound>(result)
                assertEquals(SyncDownloadError.FolderDownloadFailed, result.errorCode)
                assertEquals(listOf(CloudDevice.source.id), device.database.getFeedSources().map { it.id })
            } finally {
                device.close()
            }
        }

    @Test
    fun `folder download reports not accessible when access was revoked`() =
        runTest(testDispatcher) {
            val store = CloudStore()
            val device = createCloudDevice(CloudProvider.FOLDER, store, "worker-revoked")
            try {
                device.seed()
                (device.application.koin.get<FolderSyncDataSource>() as JvmFolderTransport).accessible = false

                val result =
                    device.application.koin
                        .get<FeedSyncWorker>()
                        .download(isFirstSync = false)

                assertIs<SyncResult.General>(result)
                assertEquals(SyncDownloadError.FolderNotAccessible, result.errorCode)
                assertEquals(listOf(CloudDevice.source.id), device.database.getFeedSources().map { it.id })
            } finally {
                device.close()
            }
        }

    @Test
    fun `folder upload reports not accessible when the folder is not writable`() =
        runTest(testDispatcher) {
            val store = CloudStore()
            val device = createCloudDevice(CloudProvider.FOLDER, store, "worker-unwritable")
            val outcomes = mutableListOf<SyncResult>()
            val collector =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    device.application.koin
                        .get<FeedSyncMessageQueue>()
                        .messageQueue
                        .collect { outcomes.add(it) }
                }
            try {
                device.seed()
                device.backup()
                (device.application.koin.get<FolderSyncDataSource>() as JvmFolderTransport).writable = false

                device.application.koin
                    .get<FeedSyncWorker>()
                    .uploadImmediate()

                assertTrue(
                    outcomes.any { it is SyncResult.General && it.errorCode == SyncUploadError.FolderNotAccessible },
                    "Unexpected sync outcomes: $outcomes",
                )
                assertEquals(listOf(CloudDevice.source.id), device.database.getFeedSources().map { it.id })
            } finally {
                collector.cancel()
                device.close()
            }
        }
}
