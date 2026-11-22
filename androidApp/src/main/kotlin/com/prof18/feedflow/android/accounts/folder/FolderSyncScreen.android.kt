package com.prof18.feedflow.android.accounts.folder

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.android.ui.accounts.folder.FolderSyncContent
import com.prof18.feedflow.android.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.core.model.FolderSynMessages
import com.prof18.feedflow.shared.presentation.FolderSyncViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun FolderSyncScreen(
    navigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<FolderSyncViewModel>()
    val context = LocalContext.current

    val uiState by viewModel.folderConnectionUiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val strings = LocalFeedFlowStrings.current
    LaunchedEffect(Unit) {
        viewModel.folderSyncMessageState.collect { message ->
            val errorMessage = when (message) {
                FolderSynMessages.Error -> strings.folderSyncError
                FolderSynMessages.NotAccessible -> strings.folderSyncNotAccessibleError
            }
            scope.launch {
                snackbarHostState.showSnackbar(message = errorMessage)
            }
        }
    }

    // SAF only allows persisting the permission while the result is delivered, so the screen takes
    // it here before the ViewModel stores the location. Cancelling leaves the previous state intact.
    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            takePersistableFolderAccess(context.contentResolver, uri)
            viewModel.linkFolder(uri.toString())
        }
    }

    FolderSyncContent(
        uiState = uiState,
        onBackClick = navigateBack,
        onBackupClick = viewModel::triggerBackup,
        onDisconnectClick = viewModel::unlink,
        onChooseFolderClick = { folderLauncher.launch(null) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    )
}

private fun takePersistableFolderAccess(contentResolver: ContentResolver, uri: Uri) {
    runCatching {
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
    }
}
