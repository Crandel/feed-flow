# Spec Delta

## Purpose

Lets users sync feeds, categories, and read and bookmark state between devices through a local folder that an external tool (for example Syncthing) keeps in sync, without a third-party cloud account.

## ADDED Requirements

### Requirement: Folder provider availability
The system SHALL offer Folder as a cloud-backup sync provider on Android (all distribution flavors) and on Linux desktop (including Flatpak). The system SHALL NOT offer it on iOS, macOS, or Windows.

#### Scenario: Provider listed on Android
- **WHEN** the user opens the sync accounts list on Android, in the Google Play or the F-Droid build
- **THEN** Folder is listed next to the other cloud-backup providers

#### Scenario: Provider listed on Linux desktop
- **WHEN** the user opens the accounts settings on Linux desktop, including a Flatpak build
- **THEN** Folder is listed next to the other cloud-backup providers

#### Scenario: Provider not listed on other platforms
- **WHEN** the user opens the sync accounts list on iOS, macOS, or Windows
- **THEN** Folder is not listed

### Requirement: Single active sync account
The Folder provider SHALL follow the same rule as the other sync providers: only one sync account is active at a time.

#### Scenario: Another provider is linked
- **GIVEN** a different sync account is linked
- **WHEN** the user opens the accounts list
- **THEN** the user cannot link Folder until the other account is unlinked

### Requirement: Link a folder
The system SHALL let the user choose a folder with the platform folder picker. On Android, the system SHALL keep persistent read and write access to the chosen folder so that it stays usable after an app or device restart.

#### Scenario: Folder linked on desktop
- **WHEN** the user chooses a writable folder in the folder dialog
- **THEN** Folder becomes the active sync account and the screen shows the folder location

#### Scenario: Folder linked on Android
- **WHEN** the user chooses a folder in the system folder picker
- **THEN** Folder becomes the active sync account and the screen shows the folder name

#### Scenario: Access kept after restart on Android
- **GIVEN** a folder is linked on Android
- **WHEN** the device restarts and a sync runs
- **THEN** the sync reads and writes the folder without asking the user again

#### Scenario: Picker cancelled
- **WHEN** the user cancels the folder picker
- **THEN** no account is linked and the previous state stays

### Requirement: Restore on first link
When the user links a folder that already holds a FeedFlow backup file, the system SHALL download that backup and merge it into local data, as the other cloud-backup providers do on first link.

#### Scenario: Folder has an existing backup
- **GIVEN** the chosen folder holds a backup file written by another device
- **WHEN** the user links the folder
- **THEN** the feeds, categories, and read and bookmark state from the backup are merged into local data

#### Scenario: Folder is empty
- **GIVEN** the chosen folder holds no backup file
- **WHEN** the user links the folder
- **THEN** the system writes a new backup file from local data

### Requirement: Upload backup to the folder
The system SHALL write the backup file to the linked folder at the same moments as the other cloud-backup providers. The system SHALL write the full backup under a temporary name first, so that the final name never holds a partly written file.

#### Scenario: Upload after a change
- **GIVEN** a folder is linked
- **WHEN** the user marks an article as read and an upload runs
- **THEN** the backup file in the folder contains the new read state

#### Scenario: Interrupted upload on desktop
- **WHEN** an upload on desktop is interrupted before it completes
- **THEN** the backup file under the final name is either the previous complete file or the new complete file

#### Scenario: Interrupted upload on Android
- **WHEN** an upload on Android is interrupted before it completes
- **THEN** the final name holds either a complete backup file or no file, never a partly written file

#### Scenario: Leftover temporary file
- **GIVEN** an earlier upload left a temporary file in the folder
- **WHEN** the next upload runs
- **THEN** the system replaces or removes the leftover temporary file

### Requirement: Download backup from the folder
The system SHALL read the backup file from the linked folder at the same moments as the other cloud-backup providers and merge it into local data with the existing merge rules.

#### Scenario: File updated by another device
- **GIVEN** another device wrote a newer backup file into the shared folder
- **WHEN** a download runs
- **THEN** the feeds, categories, and read and bookmark state from that file are merged into local data

#### Scenario: Backup file missing
- **GIVEN** a folder is linked and holds no backup file
- **WHEN** a download runs
- **THEN** the system treats it as "backup not found", as the other providers do, and does not delete local data

### Requirement: Backup file is portable
The backup file in the folder SHALL be a single self-contained file with a fixed name, so that a file copy tool can move it between devices. The system SHALL NOT leave other files in the folder after an operation completes.

#### Scenario: Copy by hand
- **GIVEN** the user copies the backup file from the desktop folder to the Android folder with any file copy tool
- **WHEN** a download runs on Android
- **THEN** the desktop state is merged on Android

#### Scenario: No leftover temporary files
- **WHEN** an upload completes
- **THEN** the folder holds only the backup file and files that FeedFlow did not create

### Requirement: Folder not available
If the linked folder no longer exists, is not writable, or access to it was revoked, the system SHALL report a sync error with a Folder-specific error code (not a Dropbox or Google Drive code) and SHALL keep local data unchanged.

#### Scenario: Folder deleted
- **GIVEN** a folder is linked
- **WHEN** the folder is deleted and a sync runs
- **THEN** the sync fails with a Folder error code and local data is unchanged

#### Scenario: Folder not writable on Flatpak
- **GIVEN** a Flatpak build without filesystem access to the chosen folder
- **WHEN** the user tries to link that folder
- **THEN** the link fails with a message that the folder is not writable, and no account is linked

#### Scenario: Access revoked on Android
- **GIVEN** a folder is linked on Android
- **WHEN** the persistent access is revoked and a sync runs
- **THEN** the user sees a Folder sync error that asks to choose the folder again

### Requirement: Unlink a folder
The system SHALL let the user unlink the folder. Unlinking SHALL NOT delete the backup file in the folder. On Android, unlinking SHALL release the persistent folder access.

#### Scenario: Unlink
- **GIVEN** a folder is linked
- **WHEN** the user unlinks it
- **THEN** no sync account is active, the backup file stays in the folder, and local feeds stay unchanged

### Requirement: Main database location unchanged
Adding the Folder provider SHALL NOT change where the main app database is stored on any platform.

#### Scenario: Existing installation upgrades
- **GIVEN** an existing installation with data
- **WHEN** the user installs the version with the Folder provider
- **THEN** the app opens the same main database file as before
