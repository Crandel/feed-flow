package com.prof18.feedflow.feedsync.folder.di

import com.prof18.feedflow.feedsync.folder.FolderSettings
import org.koin.core.module.Module
import org.koin.dsl.module

// iOS never shows the Folder provider, but the common AccountsRepository still resolves the settings.
actual fun folderModule(): Module =
    module {
        factory {
            FolderSettings(
                settings = get(),
            )
        }
    }
