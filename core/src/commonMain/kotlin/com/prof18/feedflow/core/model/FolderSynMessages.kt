package com.prof18.feedflow.core.model

sealed class FolderSynMessages {
    data object Error : FolderSynMessages()

    /** The chosen folder disappeared or is not writable; the UI asks to choose it again. */
    data object NotAccessible : FolderSynMessages()
}
