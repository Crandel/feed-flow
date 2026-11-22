package com.prof18.feedflow.shared.test

import com.prof18.feedflow.core.model.CloudBackupNotFoundException
import com.prof18.feedflow.feedsync.folder.FolderNotAccessibleException
import com.prof18.feedflow.feedsync.folder.FolderSyncDataSource

class FolderSyncDataSourceFake : FolderSyncDataSource {

    var isLinkedValue: Boolean = false
    var shouldThrowOnCheckAccess: Boolean = false
    var shouldThrowOnReleaseAccess: Boolean = false
    var storedBackup: ByteArray? = null

    var checkAccessCallCount = 0
        private set
    var releaseAccessCallCount = 0
        private set
    var uploadCallCount = 0
        private set
    var downloadCallCount = 0
        private set
    var lastDownloadFileName: String? = null
        private set

    override fun isLinked(): Boolean = isLinkedValue

    override suspend fun checkAccess() {
        checkAccessCallCount++
        if (shouldThrowOnCheckAccess) {
            throw FolderNotAccessibleException()
        }
    }

    override suspend fun upload(
        source: String,
        fileName: String,
    ) {
        uploadCallCount++
        storedBackup = byteArrayOf(1)
    }

    override suspend fun download(
        fileName: String,
        destination: String,
    ) {
        downloadCallCount++
        lastDownloadFileName = fileName
        if (storedBackup == null) {
            throw CloudBackupNotFoundException()
        }
    }

    override fun releaseAccess() {
        releaseAccessCallCount++
        if (shouldThrowOnReleaseAccess) {
            throw FolderNotAccessibleException()
        }
    }
}
