# Proposal

## Why

Cloud-backup sync of feeds, categories, and read and bookmark state works only through Dropbox, Google Drive, or iCloud. A user who wants to sync a Linux desktop and an Android phone without a third-party cloud has no option. The main database location is hardcoded, and sharing the live SQLite file (for example with Syncthing) is not safe: WAL files are left behind, the two devices cannot merge their changes, schema versions can differ, and Android cannot open SQLite on shared storage.

## What Changes

- Add a new cloud-backup sync provider, **Folder** (`SyncAccounts.FOLDER`). It uses the same sync database and merge flow as Dropbox and Google Drive. The only difference is the transport: the backup file goes to a user-chosen local folder.
- An external tool (Syncthing, `adb push`, a USB copy, a network share) moves the backup file between devices. FeedFlow only reads and writes the file in the folder.
- Desktop: the user picks a folder once with a folder dialog. The app stores the absolute path.
- Android: the user picks a folder once with the Storage Access Framework (`OpenDocumentTree`). The app keeps a persistable URI permission and copies the backup file through content streams. SQLite never opens a file on shared storage.
- Upload writes a snapshot of the sync database into the folder with a temporary file and an atomic rename. Download copies the file from the folder into a staging file, and then the existing merge runs.
- The user can unlink the folder. Unlinking does not delete the backup file in the folder.
- The main database location does not change.

Out of scope:
- Windows, macOS, and iOS validation. The desktop code is shared JVM code, but only Linux is validated. iOS does not show the provider.
- Offline article content (`articles/`). Only the state that the existing cloud-backup providers sync is synced.
- Configuring the main database location.
- Automatic detection of new files in the folder (file watchers). Sync runs at the same moments as for the other cloud-backup providers.

## Capabilities

### New Capabilities
- `folder-sync`: Cloud-backup sync through a user-chosen local folder: linking and unlinking a folder, upload and download of the backup file, error handling for a missing or unreadable folder, and platform availability.

### Modified Capabilities
<!-- None: no specs exist yet in openspec/specs/. -->

## Impact

- `core`: new `SyncAccounts.FOLDER` value. Every exhaustive `when` over `SyncAccounts` (Kotlin and Swift) must handle it.
- `feedSync`: a new `folder` module (settings, data source contract, desktop and Android implementations), patterned on `feedSync/dropbox` and `feedSync/googledrive`.
- `shared`: `FeedSyncAndroidWorker` and `FeedSyncJvmWorker` get a Folder branch for upload and download; a new `FolderSyncViewModel`; Koin wiring; accounts list availability.
- `androidApp`: Folder sync screen with an SAF folder picker; route and navigation entry.
- `desktopApp`: Folder sync UI with a folder dialog in the Settings window accounts pane.
- `i18n`: new English strings only.
- Tests: unit tests for the folder data source and the worker branches; a folder transport in the cloud-sync regression harness.
- Docs: website FAQ entry for sync providers; Maestro and desktop E2E coverage where possible, or documented limitations.
- No new third-party dependencies. No new Android permissions (SAF needs none).
