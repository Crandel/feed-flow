package com.prof18.feedflow.shared.presentation

import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.prof18.feedflow.core.model.AccountConnectionUiState.Linked
import com.prof18.feedflow.core.model.AccountConnectionUiState.Unlinked
import com.prof18.feedflow.core.model.AccountSyncUIState
import com.prof18.feedflow.core.model.FolderSynMessages
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.feedsync.folder.FolderSettings
import com.prof18.feedflow.feedsync.folder.FolderSyncDataSource
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncWorker
import com.prof18.feedflow.shared.test.FolderSyncDataSourceFake
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.get
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FolderSyncViewModelTest : KoinTestBase() {

    private val fakeFolderDataSource = FolderSyncDataSourceFake()
    private val recordingWorker = RecordingFolderSyncWorker()
    private val viewModel: FolderSyncViewModel by inject()
    private val folderSettings: FolderSettings by inject()
    private val accountsRepository: AccountsRepository by inject()

    override fun getTestModules(): List<Module> = super.getTestModules() + module {
        single<FolderSyncDataSource> { fakeFolderDataSource }
        single<FeedSyncWorker> { recordingWorker }
    }

    @Test
    fun `initial state is Unlinked when no folder is linked`() = runTest(testDispatcher) {
        viewModel.folderConnectionUiState.test {
            assertIs<Unlinked>(awaitItem())
        }
    }

    @Test
    fun `initial state is Linked with last sync dates when a folder was linked before`() = runTest(testDispatcher) {
        fakeFolderDataSource.isLinkedValue = true
        folderSettings.setFolderLocation("/home/user/syncthing")
        folderSettings.setLastUploadTimestamp(1234567890L)
        folderSettings.setLastDownloadTimestamp(1234567800L)

        val linkedViewModel: FolderSyncViewModel = getKoin().get()

        linkedViewModel.folderConnectionUiState.test {
            val state = assertIs<Linked>(expectMostRecentItem())
            val syncState = assertIs<AccountSyncUIState.Synced>(state.syncState)
            assertNotNull(syncState.lastUploadDate)
            assertNotNull(syncState.lastDownloadDate)
        }
    }

    @Test
    fun `linkFolder with an accessible folder links the account and starts a first sync`() = runTest(testDispatcher) {
        viewModel.folderConnectionUiState.test {
            assertIs<Unlinked>(awaitItem())

            viewModel.linkFolder("/home/user/syncthing")

            assertIs<Linked>(awaitItemMatching { it is Linked })
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals("/home/user/syncthing", folderSettings.getFolderLocation())
        assertEquals(SyncAccounts.FOLDER, accountsRepository.currentAccountState.value)
        assertEquals(1, fakeFolderDataSource.checkAccessCallCount)
        assertEquals(true, recordingWorker.firstIsFirstSync)
    }

    @Test
    fun `linkFolder emits not accessible and stays Unlinked when the folder is not accessible`() =
        runTest(testDispatcher) {
            fakeFolderDataSource.shouldThrowOnCheckAccess = true

            viewModel.folderSyncMessageState.test {
                viewModel.linkFolder("/home/user/deleted-folder")

                assertIs<FolderSynMessages.NotAccessible>(awaitItem())
            }

            assertIs<Unlinked>(viewModel.folderConnectionUiState.value)
            assertNull(folderSettings.getFolderLocation())
        }

    @Test
    fun `linkFolder emits a generic error and stays Unlinked when linking fails for another reason`() =
        runTest(testDispatcher) {
            fakeFolderDataSource.checkAccessFailure = IllegalStateException("boom")

            viewModel.folderSyncMessageState.test {
                viewModel.linkFolder("/home/user/weird-folder")

                assertIs<FolderSynMessages.Error>(awaitItem())
            }

            assertIs<Unlinked>(viewModel.folderConnectionUiState.value)
            assertNull(folderSettings.getFolderLocation())
        }

    @Test
    fun `linkFolder restores the existing backup during the first sync`() = runTest(testDispatcher) {
        fakeFolderDataSource.storedBackup = byteArrayOf(1, 2, 3)
        recordingWorker.onDownload = { isFirstSync ->
            if (isFirstSync) fakeFolderDataSource.download("feedflow-sync.db", "staging")
        }

        viewModel.linkFolder("/home/user/syncthing")
        advanceUntilIdle()

        // First sync plus the feed source sync that fetchFeeds performs.
        assertEquals(2, recordingWorker.downloadCallCount)
        assertEquals(1, fakeFolderDataSource.downloadCallCount)
        assertEquals("feedflow-sync.db", fakeFolderDataSource.lastDownloadFileName)
        assertIs<Linked>(viewModel.folderConnectionUiState.value)
    }

    @Test
    fun `unlink releases access and clears the link but keeps the backup file`() = runTest(testDispatcher) {
        fakeFolderDataSource.isLinkedValue = true
        fakeFolderDataSource.storedBackup = byteArrayOf(1, 2, 3)
        folderSettings.setFolderLocation("/home/user/syncthing")
        folderSettings.setLastUploadTimestamp(1234567890L)

        val linkedViewModel: FolderSyncViewModel = getKoin().get()

        linkedViewModel.folderConnectionUiState.test {
            assertIs<Linked>(expectMostRecentItem())

            linkedViewModel.unlink()

            assertIs<Unlinked>(awaitItemMatching { it is Unlinked })
        }

        assertEquals(1, fakeFolderDataSource.releaseAccessCallCount)
        assertNull(folderSettings.getFolderLocation())
        assertNull(folderSettings.getLastUploadTimestamp())
        assertEquals(SyncAccounts.LOCAL, accountsRepository.currentAccountState.value)
        assertNotNull(fakeFolderDataSource.storedBackup)
    }

    @Test
    fun `triggerBackup keeps the Linked state and uploads immediately`() = runTest(testDispatcher) {
        fakeFolderDataSource.isLinkedValue = true
        folderSettings.setFolderLocation("/home/user/syncthing")

        val linkedViewModel: FolderSyncViewModel = getKoin().get()

        linkedViewModel.triggerBackup()
        advanceUntilIdle()

        assertEquals(1, recordingWorker.uploadImmediateCallCount)
        assertIs<Linked>(linkedViewModel.folderConnectionUiState.value)
    }

    private class RecordingFolderSyncWorker : FeedSyncWorker {
        var onDownload: (suspend (Boolean) -> Unit)? = null
        var downloadCallCount = 0
            private set
        var uploadImmediateCallCount = 0
            private set
        var firstIsFirstSync: Boolean? = null
            private set
        var lastIsFirstSync: Boolean? = null
            private set

        override fun upload() = Unit

        override suspend fun uploadImmediate() {
            uploadImmediateCallCount++
        }

        override suspend fun download(isFirstSync: Boolean): SyncResult {
            downloadCallCount++
            if (firstIsFirstSync == null) firstIsFirstSync = isFirstSync
            lastIsFirstSync = isFirstSync
            onDownload?.invoke(isFirstSync)
            return SyncResult.Success
        }

        override suspend fun syncFeedSources(): SyncResult = SyncResult.Success

        override suspend fun syncFeedItems(): SyncResult = SyncResult.Success
    }

    private suspend fun <T> TurbineTestContext<T>.awaitItemMatching(
        predicate: (T) -> Boolean,
    ): T {
        while (true) {
            val item = awaitItem()
            if (predicate(item)) {
                return item
            }
        }
    }
}
