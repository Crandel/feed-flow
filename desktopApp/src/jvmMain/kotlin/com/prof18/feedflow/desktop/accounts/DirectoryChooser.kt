package com.prof18.feedflow.desktop.accounts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.prof18.feedflow.desktop.testing.DesktopE2eHooks
import com.prof18.feedflow.desktop.testing.FileDialogScript
import java.awt.Component
import java.awt.Window
import java.io.File
import javax.swing.JFileChooser

/**
 * The Folder provider's directory picker. The AWT [java.awt.FileDialog] cannot choose folders on
 * Linux, so this uses the Swing directories-only chooser.
 *
 * A scripted desktop E2E response is returned directly, so flows never wait on a real dialog:
 * a path answers as the chosen folder and an empty response answers as Cancel.
 */
internal fun chooseDirectory(
    dialogTitle: String,
    parent: Component?,
): File? {
    val script = DesktopE2eHooks.fileDialogScript()
    if (script != FileDialogScript.NotScripted) {
        return (script as? FileDialogScript.Choose)?.file
    }
    val chooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        isAcceptAllFileFilterUsed = false
        this.dialogTitle = dialogTitle
    }
    val result = chooser.showOpenDialog(parent)
    return chooser.selectedFile.takeIf { result == JFileChooser.APPROVE_OPTION }
}

/**
 * Shows [chooseDirectory] once while this composable is in composition, on a background thread
 * like the import and export dialogs, so the modal chooser never blocks the Swing event thread.
 * The result is delivered exactly once; null means the chooser was cancelled.
 */
@Composable
internal fun NativeDirectoryChooserDialog(
    dialogTitle: String,
    onCloseRequest: (directory: File?) -> Unit,
) {
    val latestOnCloseRequest by rememberUpdatedState(onCloseRequest)
    DisposableEffect(dialogTitle) {
        var composing = true
        val thread = Thread {
            val parent = Window.getWindows().firstOrNull(Window::isActive)
            val directory = chooseDirectory(dialogTitle, parent)
            if (composing) latestOnCloseRequest(directory)
        }
        thread.name = "folder-directory-chooser"
        thread.isDaemon = true
        thread.start()
        onDispose { composing = false }
    }
}
