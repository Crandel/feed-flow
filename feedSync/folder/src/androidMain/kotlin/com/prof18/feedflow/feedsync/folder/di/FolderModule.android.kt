package com.prof18.feedflow.feedsync.folder.di

import com.prof18.feedflow.feedsync.folder.FolderSettings
import com.prof18.feedflow.feedsync.folder.FolderSyncDataSource
import com.prof18.feedflow.feedsync.folder.FolderSyncDataSourceAndroid
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

actual fun folderModule(): Module =
    module {
        factory {
            FolderSettings(
                settings = get(),
            )
        }

        single<FolderSyncDataSource> {
            FolderSyncDataSourceAndroid(
                context = get(),
                folderSettings = get(),
                logger = get(parameters = { parametersOf("FolderSyncDataSourceAndroid") }),
            )
        }
    }
