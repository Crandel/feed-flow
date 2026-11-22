package com.prof18.feedflow.desktop.accounts.folder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.FolderSynMessages
import com.prof18.feedflow.desktop.accounts.AccountConnectButton
import com.prof18.feedflow.desktop.accounts.AccountScreen
import com.prof18.feedflow.desktop.accounts.CloudAccountContent
import com.prof18.feedflow.desktop.accounts.CredentialsState
import com.prof18.feedflow.desktop.accounts.NativeDirectoryChooserDialog
import com.prof18.feedflow.desktop.accounts.ObserveAccountConnection
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.shared.presentation.FolderSyncViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun FolderSyncScreen(
    credentialsState: CredentialsState? = null,
) {
    val viewModel = koinViewModel<FolderSyncViewModel>()
    val uiState by viewModel.folderConnectionUiState.collectAsState()
    val strings = LocalFeedFlowStrings.current
    val fallbackDraft = remember { CredentialsState() }
    val draft = credentialsState ?: fallbackDraft
    var isChooserOpen by remember { mutableStateOf(false) }

    ObserveAccountConnection(
        draft = draft,
        uiState = viewModel.folderConnectionUiState,
        messages = viewModel.folderSyncMessageState,
    ) { event ->
        val message = when (event) {
            FolderSynMessages.Error -> strings.folderSyncError
            FolderSynMessages.NotAccessible -> strings.folderSyncNotAccessibleError
        }
        draft.connectionErrorRevision++
        draft.connectionError = message
        draft.syncError = message
        draft.submissionPending = false
    }

    AccountScreen {
        FolderAccountContent(
            uiState = uiState,
            credentialsState = draft,
            onConnect = { isChooserOpen = true },
            onBackup = viewModel::triggerBackup,
            onDisconnect = viewModel::unlink,
        )
    }

    if (isChooserOpen) {
        NativeDirectoryChooserDialog(dialogTitle = strings.folderConnectButton) { directory ->
            isChooserOpen = false
            if (directory != null) viewModel.linkFolder(directory.absolutePath)
        }
    }
}

@Composable
internal fun FolderAccountContent(
    uiState: AccountConnectionUiState,
    credentialsState: CredentialsState,
    onConnect: () -> Unit,
    onBackup: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    CloudAccountContent(
        description = strings.folderSyncCommonDescription,
        successMessage = strings.folderSyncSuccess,
        noSyncMessage = strings.noFolderSyncYet,
        disconnectMessage = strings.accountDisconnectBackupMessage,
        uiState = uiState,
        credentialsState = credentialsState,
        onBackupClick = onBackup,
        onDisconnectClick = onDisconnect,
        lastUploadMessage = strings.lastUpload,
        lastDownloadMessage = strings.lastDownload,
        connectContent = {
            AccountConnectButton(
                label = strings.folderConnectButton,
                isLoading = uiState == AccountConnectionUiState.Loading,
                errorRevision = credentialsState.connectionErrorRevision,
                errorMessage = credentialsState.connectionError,
                onConnect = {
                    credentialsState.connectionError = null
                    credentialsState.syncError = null
                    onConnect()
                },
                state = credentialsState,
            )
        },
    )
}
