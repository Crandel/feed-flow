package com.prof18.feedflow.feedsync.folder.di

import com.prof18.feedflow.feedsync.folder.FolderSettings
import com.prof18.feedflow.feedsync.folder.FolderSyncDataSource
import com.prof18.feedflow.feedsync.folder.FolderSyncDataSourceJvm
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun folderModule(): Module =
    module {
        factory {
            FolderSettings(
                settings = get(),
            )
        }

        single<FolderSyncDataSource> {
            FolderSyncDataSourceJvm(
                folderSettings = get(),
            )
        }
    }
