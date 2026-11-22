package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.AccountSyncUIState
import com.prof18.feedflow.core.model.FolderSynMessages
import com.prof18.feedflow.core.utils.FeedSyncMessageQueue
import com.prof18.feedflow.feedsync.folder.FolderNotAccessibleException
import com.prof18.feedflow.feedsync.folder.FolderSettings
import com.prof18.feedflow.feedsync.folder.FolderSyncDataSource
import com.prof18.feedflow.shared.domain.feed.FeedFetcherRepository
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FolderSyncViewModel internal constructor(
    private val logger: Logger,
    private val folderSettings: FolderSettings,
    private val folderSyncDataSource: FolderSyncDataSource,
    private val feedSyncRepository: FeedSyncRepository,
    private val dateFormatter: DateFormatter,
    private val accountsRepository: AccountsRepository,
    private val feedFetcherRepository: FeedFetcherRepository,
    feedSyncMessageQueue: FeedSyncMessageQueue,
) : ViewModel() {

    private val folderSyncUiMutableState = MutableStateFlow<AccountConnectionUiState>(
        AccountConnectionUiState.Unlinked,
    )
    val folderConnectionUiState: StateFlow<AccountConnectionUiState> = folderSyncUiMutableState.asStateFlow()

    private val folderSyncMessageMutableState = MutableSharedFlow<FolderSynMessages>()
    val folderSyncMessageState: SharedFlow<FolderSynMessages> = folderSyncMessageMutableState.asSharedFlow()

    val syncMessageQueue = feedSyncMessageQueue.userMessages

    init {
        restoreFolderLink()
    }

    fun linkFolder(location: String) {
        viewModelScope.launch {
            folderSyncUiMutableState.update { AccountConnectionUiState.Loading }
            try {
                // The data source reads the location from settings, so store it before checking access.
                folderSettings.setFolderLocation(location)
                folderSyncDataSource.checkAccess()
                accountsRepository.setFolderAccount()
                folderSyncUiMutableState.update {
                    AccountConnectionUiState.Linked(
                        syncState = getSyncState(),
                    )
                }
                emitSyncLoading()
                feedSyncRepository.firstSync()
                feedFetcherRepository.fetchFeeds()
                emitLastSyncUpdate()
            } catch (e: FolderNotAccessibleException) {
                logger.d(e) { "The folder is not accessible" }
                folderSettings.clearAll()
                folderSyncUiMutableState.update { AccountConnectionUiState.Unlinked }
                folderSyncMessageMutableState.emit(FolderSynMessages.NotAccessible)
            } catch (e: Throwable) {
                logger.e(e) { "Error while linking the folder" }
                folderSettings.clearAll()
                folderSyncUiMutableState.update { AccountConnectionUiState.Unlinked }
                folderSyncMessageMutableState.emit(FolderSynMessages.Error)
            }
        }
    }

    fun triggerBackup() {
        viewModelScope.launch {
            emitSyncLoading()
            feedSyncRepository.performBackup(forceBackup = true)
            emitLastSyncUpdate()
        }
    }

    fun unlink() {
        viewModelScope.launch {
            folderSyncUiMutableState.update { AccountConnectionUiState.Loading }
            try {
                folderSyncDataSource.releaseAccess()
            } catch (e: Throwable) {
                logger.d(e) { "Unable to release the folder access; unlinking locally" }
            }
            folderSettings.clearAll()
            feedSyncRepository.deleteAll()
            accountsRepository.clearAccount()
            folderSyncUiMutableState.update { AccountConnectionUiState.Unlinked }
        }
    }

    private fun restoreFolderLink() {
        folderSyncUiMutableState.update { AccountConnectionUiState.Loading }
        if (folderSyncDataSource.isLinked()) {
            folderSyncUiMutableState.update {
                AccountConnectionUiState.Linked(
                    syncState = getSyncState(),
                )
            }
        } else {
            folderSyncUiMutableState.update { AccountConnectionUiState.Unlinked }
        }
    }

    private fun getSyncState(): AccountSyncUIState {
        return when {
            folderSettings.getLastDownloadTimestamp() != null || folderSettings.getLastUploadTimestamp() != null -> {
                AccountSyncUIState.Synced(
                    lastDownloadDate = getLastDownloadDate(),
                    lastUploadDate = getLastUploadDate(),
                )
            }

            else -> AccountSyncUIState.None
        }
    }

    private fun getLastUploadDate(): String? =
        folderSettings.getLastUploadTimestamp()?.let { timestamp ->
            dateFormatter.formatDateForLastRefresh(timestamp)
        }

    private fun getLastDownloadDate(): String? =
        folderSettings.getLastDownloadTimestamp()?.let { timestamp ->
            dateFormatter.formatDateForLastRefresh(timestamp)
        }

    private fun emitSyncLoading() {
        folderSyncUiMutableState.update { oldState ->
            if (oldState is AccountConnectionUiState.Linked) {
                AccountConnectionUiState.Linked(
                    syncState = AccountSyncUIState.Loading,
                )
            } else {
                oldState
            }
        }
    }

    private fun emitLastSyncUpdate() {
        folderSyncUiMutableState.update { oldState ->
            if (oldState is AccountConnectionUiState.Linked) {
                AccountConnectionUiState.Linked(
                    syncState = getSyncState(),
                )
            } else {
                oldState
            }
        }
    }
}
