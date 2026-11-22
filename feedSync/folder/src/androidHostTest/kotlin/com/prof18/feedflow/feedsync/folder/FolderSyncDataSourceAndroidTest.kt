package com.prof18.feedflow.feedsync.folder

import com.prof18.feedflow.core.model.CloudBackupNotFoundException
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FolderSyncDataSourceAndroidTest {
    private val folderSettings = FolderSettings(MapSettings())
    private val store = FakeFolderDocumentStore()
    private lateinit var dataSource: FolderSyncDataSourceAndroid
    private lateinit var stagingFile: File

    @BeforeTest
    fun setUp() {
        folderSettings.setFolderLocation(TREE_URI)
        dataSource = FolderSyncDataSourceAndroid(store, folderSettings)
        stagingFile = File.createTempFile("cloud-upload-", ".db")
    }

    @AfterTest
    fun tearDown() {
        stagingFile.delete()
    }

    @Test
    fun `upload writes the tmp child then deletes the old backup and renames the tmp into place`() =
        runTest {
            store.documents[BACKUP_NAME] = "old-remote-state"
            stagingFile.writeText("new-state")

            dataSource.upload(stagingFile.absolutePath, BACKUP_NAME)

            assertEquals(
                listOf(
                    "find:$TMP_NAME",
                    "create:$TMP_NAME",
                    "write:$TMP_NAME:wt",
                    "find:$BACKUP_NAME",
                    "delete:$BACKUP_NAME",
                    "rename:$TMP_NAME->$BACKUP_NAME",
                ),
                store.calls,
            )
            assertEquals("new-state", store.documents[BACKUP_NAME])
            assertFalse(store.documents.containsKey(TMP_NAME))
        }

    @Test
    fun `upload reuses a leftover tmp child instead of creating one`() =
        runTest {
            store.documents[TMP_NAME] = "half-written-junk"
            stagingFile.writeText("new-state")

            dataSource.upload(stagingFile.absolutePath, BACKUP_NAME)

            assertFalse(store.calls.contains("create:$TMP_NAME"))
            assertEquals("new-state", store.documents[BACKUP_NAME])
            assertFalse(store.documents.containsKey(TMP_NAME))
        }

    @Test
    fun `upload falls back to a direct write when renameDocument is unsupported`() =
        runTest {
            store.supportsRename = false
            stagingFile.writeText("new-state")

            dataSource.upload(stagingFile.absolutePath, BACKUP_NAME)

            assertEquals(
                listOf(
                    "find:$TMP_NAME",
                    "create:$TMP_NAME",
                    "write:$TMP_NAME:wt",
                    "find:$BACKUP_NAME",
                    "rename:$TMP_NAME->$BACKUP_NAME",
                    "find:$TMP_NAME",
                    "delete:$TMP_NAME",
                    "find:$BACKUP_NAME",
                    "create:$BACKUP_NAME",
                    "write:$BACKUP_NAME:wt",
                ),
                store.calls,
            )
            assertEquals("new-state", store.documents[BACKUP_NAME])
        }

    @Test
    fun `upload refuses to write when the persisted access was revoked`() =
        runTest {
            store.accessGranted = false
            stagingFile.writeText("new-state")

            assertFailsWith<FolderNotAccessibleException> {
                dataSource.upload(stagingFile.absolutePath, BACKUP_NAME)
            }
            assertTrue(store.calls.isEmpty())
        }

    @Test
    fun `upload fails when the tree is no longer queryable`() =
        runTest {
            store.treeQueryable = false

            assertFailsWith<FolderNotAccessibleException> {
                dataSource.upload(stagingFile.absolutePath, BACKUP_NAME)
            }
        }

    @Test
    fun `download copies the document into the staging file`() =
        runTest {
            store.documents[BACKUP_NAME] = "remote-state"

            dataSource.download(BACKUP_NAME, stagingFile.absolutePath)

            assertEquals("remote-state", stagingFile.readText())
            assertEquals(listOf("find:$BACKUP_NAME", "read:$BACKUP_NAME"), store.calls)
        }

    @Test
    fun `download of a missing backup throws CloudBackupNotFound`() =
        runTest {
            assertFailsWith<CloudBackupNotFoundException> {
                dataSource.download(BACKUP_NAME, stagingFile.absolutePath)
            }
        }

    @Test
    fun `download reports not accessible when the persisted access was revoked`() =
        runTest {
            store.documents[BACKUP_NAME] = "remote-state"
            store.accessGranted = false

            assertFailsWith<FolderNotAccessibleException> {
                dataSource.download(BACKUP_NAME, stagingFile.absolutePath)
            }
        }

    @Test
    fun `checkAccess passes with a granted and queryable tree`() =
        runTest {
            dataSource.checkAccess()
        }

    @Test
    fun `checkAccess fails when no folder is linked`() {
        folderSettings.clearAll()

        assertFalse(dataSource.isLinked())
        assertFailsWith<FolderNotAccessibleException> { runTest { dataSource.checkAccess() } }
    }

    @Test
    fun `releaseAccess releases the persistable permission`() {
        dataSource.releaseAccess()

        assertEquals(listOf("release"), store.calls)
        assertFalse(store.accessGranted)
    }

    private companion object {
        const val TREE_URI = "content://com.example.provider/tree/primary"
        const val BACKUP_NAME = "FeedFlowFeedSyncDB.db"
        const val TMP_NAME = ".$BACKUP_NAME.tmp"
    }
}
