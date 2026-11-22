package com.prof18.feedflow.feedsync.folder

import com.prof18.feedflow.core.model.CloudBackupNotFoundException
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class FolderSyncDataSourceJvmTest {
    private val folderSettings = FolderSettings(MapSettings())
    private val dataSource = FolderSyncDataSourceJvm(folderSettings)
    private val backupName = "FeedFlowFeedSyncDB.db"
    private lateinit var folder: File
    private lateinit var stagingFile: File

    @BeforeTest
    fun setUp() {
        folder = Files.createTempDirectory("folder-sync-test-").toFile()
        stagingFile = File(Files.createTempFile("cloud-upload-", ".db").toFile().absolutePath)
        folderSettings.setFolderLocation(folder.absolutePath)
    }

    @AfterTest
    fun tearDown() {
        folder.setWritable(true)
        folder.deleteRecursively()
        stagingFile.delete()
    }

    @Test
    fun `upload creates the backup file and leaves no temporary file`() =
        runTest {
            writeStaging("backup-v1")

            dataSource.upload(stagingFile.absolutePath, backupName)

            assertEquals("backup-v1", File(folder, backupName).readText())
            assertEquals(listOf(backupName), folder.list()?.sorted())
        }

    @Test
    fun `upload replaces an existing backup file`() =
        runTest {
            writeStaging("backup-v1")
            dataSource.upload(stagingFile.absolutePath, backupName)

            writeStaging("backup-v2")
            dataSource.upload(stagingFile.absolutePath, backupName)

            assertEquals("backup-v2", File(folder, backupName).readText())
            assertEquals(listOf(backupName), folder.list()?.sorted())
        }

    @Test
    fun `upload replaces a leftover temporary file`() =
        runTest {
            val leftoverTmp = File(folder, tmpName(backupName))
            leftoverTmp.writeText("half-written-junk")
            writeStaging("backup-v2")

            dataSource.upload(stagingFile.absolutePath, backupName)

            assertFalse(leftoverTmp.exists())
            assertEquals("backup-v2", File(folder, backupName).readText())
        }

    @Test
    fun `download copies the backup into the staging file`() =
        runTest {
            writeStaging("backup-v1")
            dataSource.upload(stagingFile.absolutePath, backupName)
            stagingFile.writeText("previous staging content")

            dataSource.download(backupName, stagingFile.absolutePath)

            assertEquals("backup-v1", stagingFile.readText())
        }

    @Test
    fun `download of a missing backup throws CloudBackupNotFound`() =
        runTest {
            try {
                dataSource.download(backupName, stagingFile.absolutePath)
                fail("Expected CloudBackupNotFoundException")
            } catch (_: CloudBackupNotFoundException) {
            }
        }

    @Test
    fun `a folder without a location is not linked and not accessible`() =
        runTest {
            folderSettings.clearAll()

            assertFalse(dataSource.isLinked())
            assertFolderNotAccessible { dataSource.checkAccess() }
        }

    @Test
    fun `a deleted folder is not accessible`() =
        runTest {
            folder.delete()

            assertFolderNotAccessible { dataSource.checkAccess() }
            assertFolderNotAccessible { dataSource.upload(stagingFile.absolutePath, backupName) }
        }

    @Test
    fun `a folder that is not writable throws FolderNotAccessible`() =
        runTest {
            assertTrue(folder.setWritable(false))
            try {
                assertFolderNotAccessible { dataSource.checkAccess() }
                assertFolderNotAccessible { dataSource.upload(stagingFile.absolutePath, backupName) }
                assertFolderNotAccessible { dataSource.download(backupName, stagingFile.absolutePath) }
            } finally {
                folder.setWritable(true)
            }
        }

    private suspend fun assertFolderNotAccessible(block: suspend () -> Unit) {
        try {
            block()
            fail("Expected FolderNotAccessibleException")
        } catch (_: FolderNotAccessibleException) {
        }
    }

    private fun writeStaging(content: String) {
        stagingFile.writeText(content)
    }

    private fun tmpName(fileName: String): String = ".$fileName.tmp"
}
