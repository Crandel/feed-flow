package com.prof18.feedflow.desktop.accounts

import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DesktopE2eProperties
import com.prof18.feedflow.desktop.testing.DesktopE2eHooks
import org.junit.After
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DirectoryChooserTest {

    @After
    fun tearDown() {
        DesktopE2eHooks.reset()
    }

    @Test
    fun `scripted chooser answers with the scripted directory`() {
        val folder = Files.createTempDirectory("folder-chooser").toFile()
        install(temporaryResponse(folder.absolutePath))

        assertEquals(folder, chooseDirectory("Choose a folder", null))
    }

    @Test
    fun `empty scripted response answers as cancel`() {
        install(temporaryResponse(""))

        assertNull(chooseDirectory("Choose a folder", null))
    }

    private fun install(response: File) {
        val values = mapOf(DesktopE2eProperties.FILE_DIALOG_RESPONSE to response.absolutePath)
        DesktopE2eHooks.install(AppEnvironment.Debug) { values[it] }
    }

    private fun temporaryResponse(content: String): File =
        Files.createTempFile("dialog-response", ".txt").toFile().apply { writeText(content) }
}
