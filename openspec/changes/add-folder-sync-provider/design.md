# Design

## Context

See proposal.md for the motivation and specs/folder-sync/spec.md for the required behavior.

Cloud-backup sync today:

```
 FeedSyncRepository (gated by AccountsRepository.isSyncEnabled())
        |
        v
 FeedSyncWorker  (FeedSyncAndroidWorker | FeedSyncJvmWorker | FeedSyncIosWorker)
   performUpload:  download first -> apply pending changes to sync DB
                   -> snapshot sync DB (cloud-upload-*.db) -> accountSpecificUpload
   downloadLocked: stage (cloud-download-*.db) -> accountSpecificDownload
                   -> installDownloadedFile (validate/migrate + Files.move) -> merge
        |
        v
 provider data source (Dropbox, Google Drive, iCloud)
```

- The current account is not stored as one key. `AccountsRepository.getCurrentSyncAccount()` derives it from each provider's settings, and `isSyncEnabled()` lists the cloud-backup accounts by hand.
- Every provider shares one multiplatform-settings `Settings` instance, so key names must be unique across providers.
- `SyncAccounts` is used in many exhaustive `when` blocks, in Kotlin and Swift, and in some sets that are not exhaustive (for example `FeedRecoveryService.LOCAL_ACCOUNTS`).
- Upload errors are always reported as `SyncUploadError.DropboxUploadFailed` (SU2). Download errors are mapped per account in `syncDownloadErrorForAccount`, with Dropbox as the default branch.
- The desktop app has only an AWT `FileDialog`, which cannot choose a folder on Linux. The `feedflow.e2e.fileDialogResponse` hook scripts file dialogs in desktop E2E tests.
- Android has no `OpenDocumentTree`, persisted URI permission, or `DocumentFile` code yet. `androidx.documentfile` is not a dependency.

## Goals / Non-Goals

**Goals:**
- Add Folder as a peer of Dropbox and Google Drive with the smallest change to the shared sync flow: the worker keeps its download-merge-snapshot logic, and only the transport is new.
- Keep the transport testable without a device: one small interface, a plain-file implementation on JVM, and a SAF implementation on Android.
- Give Folder its own error codes.

**Non-Goals:**
- Conflict detection (the Dropbox revision check). Folder is last-writer-wins, as Google Drive and iCloud are.
- Watching the folder for changes. Sync runs at the existing triggers.
- macOS and Windows desktop, iOS.
- Changes to the main database or to the sync database schema.

## Decisions

### D1. New module `feedSync/folder`
It holds `FolderSettings` (commonMain), the `FolderSyncDataSource` interface (commonMain), `FolderSyncDataSourceJvm` (jvmMain), and `FolderSyncDataSourceAndroid` (androidMain). The iOS target gets only the settings and a Koin module, so that common code compiles.
- Why: this follows `feedSync/dropbox` and `feedSync/googledrive`. The cloud-sync docs say provider adapters belong under `feedSync/<provider>/`.
- Alternative: put the code in `shared`. Rejected, because `shared` would grow and the harness docs expect a provider module.

### D2. Transport interface
```kotlin
interface FolderSyncDataSource {
    fun isLinked(): Boolean
    suspend fun checkAccess()                 // throws FolderNotAccessibleException
    suspend fun upload(source: File, fileName: String)
    suspend fun download(fileName: String, destination: File) // throws CloudBackupNotFoundException
}
```
- The worker keeps its own staging files (`cloud-upload-*`, `cloud-download-*`) in the app data folder. The data source only copies bytes between a local file and the folder.
- A missing backup file throws the existing `CloudBackupNotFoundException`, so the existing `BackupNotFound` path (first upload, `firstSync`) works without change.
- File name: the existing sync database name with extension (`FeedFlowFeedSyncDB.db`, or `FeedFlowFeedSyncDB-debug.db` in debug builds). This keeps debug and release backups apart in the same folder, as the other providers do.

### D3. Desktop transport: plain path, atomic move
- Upload: copy to `<folder>/.<fileName>.tmp`, then `Files.move(tmp, final, ATOMIC_MOVE, REPLACE_EXISTING)`. If the file system does not support an atomic move, fall back to `REPLACE_EXISTING` only.
- The `.tmp` name starts with a dot. Syncthing ignores temporary files only when they match its own patterns, so the user's ignore patterns may still pick up this file. The window is short, and the file has a different name, so the receiver never reads it as a backup.
- Download: if the file does not exist, throw `CloudBackupNotFoundException`. Otherwise copy it to the staged file.
- Link check: the folder exists, is a directory, and is writable (create and delete a probe file). This catches a Flatpak build without filesystem access at link time.

### D4. Android transport: SAF tree URI, no new dependency
- Link: `ActivityResultContracts.OpenDocumentTree()`, then `contentResolver.takePersistableUriPermission(uri, READ or WRITE)`. Store the tree URI string in `FolderSettings`.
- File lookup: query the children of the tree with `DocumentsContract.buildChildDocumentsUriUsingTree` and match `COLUMN_DISPLAY_NAME`. We use `DocumentsContract` directly instead of adding `androidx.documentfile`.
- Upload: create or reuse the child `.<fileName>.tmp`, write it with `openOutputStream(uri, "wt")`, delete the old final document, then `DocumentsContract.renameDocument(tmp, fileName)`. SAF has no atomic replace. Between the delete and the rename the final name is briefly missing, but it never holds a partial file. This matches the Android scenario in the spec.
- Download: open the final child with `openInputStream` and copy it into the staged file. If the child does not exist, throw `CloudBackupNotFoundException`.
- Access check: the URI is in `contentResolver.persistedUriPermissions` with read and write, and the tree is still queryable. Otherwise throw `FolderNotAccessibleException`.
- Unlink: `releasePersistableUriPermission` and clear settings.
- The worker runs in WorkManager and uses only the application `Context`.
- Alternative: write straight to the final name with "wt". Rejected, because Syncthing could pick up a half-written file.

### D5. Settings and account state
- `FolderSettings` keys: `FOLDER_SYNC_LOCATION` (desktop path or Android tree URI), `FOLDER_SYNC_LAST_UPLOAD_TIMESTAMP`, `FOLDER_SYNC_LAST_DOWNLOAD_TIMESTAMP`, plus `clearAll()`. The prefix prevents collisions in the shared `Settings`.
- `AccountsRepository`:
  - Add `setFolderAccount()`.
  - Add a FOLDER branch in `clearOtherSyncCredentials`.
  - Add FOLDER in `getCurrentSyncAccount()` (linked when the location is set) and in `isSyncEnabled()`.
  - Add FOLDER to `generateAndroidAccounts()` for both flavors and to `generateLinuxAccounts()` only.
- Add FOLDER to `FeedRecoveryService.LOCAL_ACCOUNTS`, and to every grouped `LOCAL, DROPBOX, GOOGLE_DRIVE, ICLOUD` branch in the repositories (`FeedCategoryRepository`, `FeedSourcesRepository`, `FeedActionsRepository`, `FeedImportExportRepository`, `GReaderRepository`). Folder is a cloud-backup account, so feeds are managed locally.

### D6. Worker branches and error codes
- New codes in `ErrorCode.kt`: `SyncUploadError.FolderUploadFailed` (SU6), `SyncUploadError.FolderNotAccessible` (SU7), `SyncDownloadError.FolderDownloadFailed` (SD5), `SyncDownloadError.FolderNotAccessible` (SD6).
- `syncDownloadErrorForAccount` maps FOLDER to SD5 (SD6 for `FolderNotAccessibleException`).
- Upload errors are mapped per account for FOLDER only. The existing Dropbox and Google Drive behavior (SU2) stays the same, to keep this change narrow.
- Android and JVM workers: the FOLDER branch in `accountSpecificUpload` and `accountSpecificDownload` calls the data source, then updates the `FolderSettings` timestamps.
- iOS worker: FOLDER joins the no-op group. It can never be the current account there.

### D7. ViewModels
- One `FolderSyncViewModel` in `commonMain`. It does not depend on the location format, because linking takes an opaque location string that the platform UI provides.
- Flow (copied from `GoogleDriveSyncViewModel`):
  - Link: `linkFolder(location)` -> `dataSource.checkAccess()` -> `setFolderAccount()` -> `firstSync()` -> `fetchFeeds()`.
  - `triggerBackup()` -> `performBackup(forceBackup = true)`.
  - `unlink()` -> release access -> `feedSyncRepository.deleteAll()` -> `clearAccount()`.
- On Android the screen takes the persistable permission before it calls `linkFolder`, because that needs the `ActivityResult`.
- The UI state reuses `AccountConnectionUiState` and `AccountSyncUIState`, so the existing `last_upload` and `last_download` strings work.

### D8. UI
- Android: a `FolderSync` `NavKey` plus an `entry` in `MainActivity`, like the server providers. This is simpler than a separate Activity (Dropbox and Drive need one only for OAuth).
  - Add `onFolderClick` to `AccountsContent`, plus a title and an icon (a Material folder icon).
  - The screen uses `rememberLauncherForActivityResult(OpenDocumentTree())`.
- Desktop: `FolderSyncScreen.desktop.kt` built on `CloudAccountContent`. The connect button opens a `JFileChooser` in `DIRECTORIES_ONLY` mode.
  - The chooser first checks `DesktopE2eHooks.fileDialogScript()`, so E2E flows can script the choice.
  - Add FOLDER to `DesktopAccountDetail`, `desktopTitle`, `desktopIcon`, `isBackupProvider`, and `AccountTestContent`.
  - Alternative: the XDG desktop portal through D-Bus. Rejected for now: it needs a new dependency. `JFileChooser` works on Linux, and in Flatpak when the user grants filesystem access.
- Display name in English: "Folder" (strings `folder_sync_*` and `desktop_accounts_folder_title`, following the Google Drive key pattern).

### D9. Tests
- Unit tests:
  - `FolderSyncDataSourceJvm`: upload and replace, missing file, folder that is not writable, leftover `.tmp`.
  - `FolderSettings`.
  - `AccountsRepository` (Linux and Android lists, current account, `isSyncEnabled`).
  - `FolderSyncViewModel` (link, link failure, unlink).
- The Android SAF data source is tested with Robolectric where `DocumentsContract` can be faked. If that is not practical, keep its logic thin and cover the copy and rename order behind a small seam.
- Cloud-sync harness: add `CloudProvider.FOLDER`, a JVM transport and an Android transport backed by the in-memory `CloudStore`, and the provider entries in `DesktopCloudSyncSuccessTest`, `AndroidCloudSyncSuccessTest`, `AndroidCloudWorkManagerSuccessTest`, and the artifact exchange tests. Follow the onboarding steps in `docs/CLOUD_SYNC_LIVE_TESTING.md`.
- E2E:
  - Add `folder` to `E2eSeedAccount` and extend the REG-118 mock-state flow.
  - Desktop: `ProviderContentTest` picks FOLDER up automatically. Add a `DSK-` flow that links a temporary folder through the file dialog hook and checks that a backup file appears.
  - The real Android SAF picker is system UI and is recorded as a Maestro limitation.

## Risks / Trade-offs

- [Both devices change state before Syncthing moves the file] -> Last writer wins for the conflicting rows. Download-before-upload narrows the window. Syncthing keeps the other version as a `.sync-conflict-*` file, which FeedFlow ignores. This is documented in the FAQ.
- [Android: brief window with no final file between delete and rename] -> Another device that downloads in that window gets `BackupNotFound`. On the upload path it then bootstraps from local data and can overwrite newer remote rows. Through Syncthing this window cannot reach another device, because Syncthing transfers whole files and renames them atomically on the receiver. The risk exists only with direct copy tools.
- [Flatpak: `JFileChooser` sees only the folders that the sandbox allows] -> The link check fails with a clear "not writable" message. The FAQ explains `flatpak override --user --filesystem=<path> com.prof18.feedflow`.
- [Android OEM document providers that do not support `renameDocument`] -> Fall back to delete and then write the final name directly. Log this. Test with the default external storage provider, which Syncthing-Fork folders use.
- [New `SyncAccounts` value breaks Swift switches] -> Add `.folder` cases in `iosApp`. These cannot be built on Linux, so the macOS CI `build-ios-app` job is the check.
- [Baseline profiles record worker constructor signatures] -> Do not hand-edit them. A slightly stale profile is still effective.

## Migration Plan

- Additive: no schema change and no settings migration. Existing accounts are not affected.
- Rollback: revert the change. A user with Folder linked then has no current account (no `FOLDER_SYNC_LOCATION` reader), which is the same as LOCAL. The backup file stays in the folder.

## Open Questions

- Should the Folder display name be "Folder" or "Local folder"? This changes only English strings and can be settled during review.
