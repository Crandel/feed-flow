package com.prof18.feedflow.core.model

sealed class FolderSynMessages {
    data object Error : FolderSynMessages()
}
