package com.prof18.feedflow.feedsync.folder.di

import org.koin.core.module.Module
import org.koin.dsl.module

// iOS never shows the Folder provider; the module only keeps common code compiling.
actual fun folderModule(): Module = module { }
