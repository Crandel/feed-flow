package com.prof18.feedflow.android.ui.accounts.folder

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.prof18.feedflow.android.ui.accounts.AccountE2eIds
import com.prof18.feedflow.android.ui.accounts.DisconnectAccountItem
import com.prof18.feedflow.android.ui.accounts.DisconnectAccountKind
import com.prof18.feedflow.android.ui.preview.PreviewPhone
import com.prof18.feedflow.android.ui.settings.SettingItem
import com.prof18.feedflow.android.ui.style.Spacing
import com.prof18.feedflow.android.ui.theme.FeedFlowTheme
import com.prof18.feedflow.android.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.AccountSyncUIState

@Composable
fun FolderSyncContent(
    uiState: AccountConnectionUiState,
    onBackClick: () -> Unit,
    onBackupClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onChooseFolderClick: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                title = {
                    Text(text = LocalFeedFlowStrings.current.desktopAccountsFolderTitle)
                },
            )
        },
        snackbarHost = snackbarHost,
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .padding(bottom = Spacing.regular),
        ) {
            when (uiState) {
                is AccountConnectionUiState.Linked -> ConnectedView(
                    uiState = uiState,
                    onBackupClick = onBackupClick,
                    onDisconnectClick = onDisconnectClick,
                )

                AccountConnectionUiState.Loading -> LoadingView()
                AccountConnectionUiState.Unlinked -> DisconnectedView(
                    onChooseFolderClick = onChooseFolderClick,
                )
            }
        }
    }
}

@Composable
private fun LoadingView() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize(),
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ConnectedView(
    uiState: AccountConnectionUiState.Linked,
    onBackupClick: () -> Unit,
    onDisconnectClick: () -> Unit,
) {
    Column {
        Text(
            text = LocalFeedFlowStrings.current.folderSyncSuccess,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .padding(horizontal = Spacing.regular)
                .testTag(AccountE2eIds.CONNECTED_MESSAGE),
        )

        when (val syncState = uiState.syncState) {
            AccountSyncUIState.Loading -> {
                Row(
                    modifier = Modifier
                        .padding(
                            top = Spacing.regular,
                            start = Spacing.regular,
                            end = Spacing.regular,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = LocalFeedFlowStrings.current.accountRefreshProgress,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = Spacing.small),
                    )
                }
            }

            AccountSyncUIState.None -> {
                Text(
                    text = LocalFeedFlowStrings.current.noFolderSyncYet,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .padding(horizontal = Spacing.regular)
                        .padding(top = Spacing.small),
                )
            }

            is AccountSyncUIState.Synced -> {
                val lastUpload = syncState.lastUploadDate
                if (lastUpload != null) {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = Spacing.regular)
                            .padding(top = Spacing.small)
                            .testTag(AccountE2eIds.LAST_SYNC_LABEL),
                        text = LocalFeedFlowStrings.current.lastUpload(lastUpload),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                val lastDownload = syncState.lastDownloadDate
                if (lastDownload != null) {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = Spacing.regular)
                            .padding(top = Spacing.small)
                            .testTag(AccountE2eIds.LAST_SYNC_LABEL),
                        text = LocalFeedFlowStrings.current.lastDownload(lastDownload),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        SettingItem(
            modifier = Modifier
                .padding(top = Spacing.regular),
            isEnabled = uiState.syncState !is AccountSyncUIState.Loading,
            title = LocalFeedFlowStrings.current.backupButton,
            icon = Icons.Default.FileUpload,
            onClick = onBackupClick,
        )

        DisconnectAccountItem(
            kind = DisconnectAccountKind.BackupService,
            isEnabled = uiState.syncState !is AccountSyncUIState.Loading,
            onConfirm = onDisconnectClick,
        )
    }
}

@Composable
private fun DisconnectedView(
    onChooseFolderClick: () -> Unit,
) {
    Column {
        Text(
            text = LocalFeedFlowStrings.current.folderSyncCommonDescription,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = Spacing.regular),
        )

        SettingItem(
            modifier = Modifier
                .padding(top = Spacing.regular)
                .testTag(AccountE2eIds.CONNECT_BUTTON),
            title = LocalFeedFlowStrings.current.folderConnectButton,
            icon = Icons.Default.Folder,
            onClick = onChooseFolderClick,
        )
    }
}

@PreviewPhone
@Composable
private fun FolderSyncContentUnlinkedPreview() {
    FeedFlowTheme {
        FolderSyncContent(
            uiState = AccountConnectionUiState.Unlinked,
            onBackClick = {},
            onBackupClick = {},
            onDisconnectClick = {},
            onChooseFolderClick = {},
        )
    }
}

@PreviewPhone
@Composable
private fun FolderSyncContentLinkedPreview() {
    FeedFlowTheme {
        FolderSyncContent(
            uiState = AccountConnectionUiState.Linked(
                syncState = AccountSyncUIState.Synced(
                    lastUploadDate = "Today, 10:30",
                    lastDownloadDate = "Today, 10:29",
                ),
            ),
            onBackClick = {},
            onBackupClick = {},
            onDisconnectClick = {},
            onChooseFolderClick = {},
        )
    }
}
