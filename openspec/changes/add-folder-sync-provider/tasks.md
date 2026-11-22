# Tasks

## 1. Account type, error codes, and module scaffold

- [x] 1.1 Add `SyncAccounts.FOLDER`. Handle it in every exhaustive `when` and grouped list (FeedSyncWorker `syncDownloadErrorForAccount`, Android/JVM/iOS workers as a temporary no-op, `FeedCategoryRepository`, `FeedSourcesRepository`, `FeedActionsRepository`, `FeedImportExportRepository`, `GReaderRepository`, `FeedRecoveryService.LOCAL_ACCOUNTS`, `E2eSeedProfile`/`E2eSeedRunner`, Android `AccountScreenComponents`, desktop `AccountsScreen.desktop`/`DesktopAccountPresentation`/`AccountTestContent`). Verify with `./gradlew --quiet --console=plain :shared:compileKotlinJvm :desktopApp:compileKotlinJvm :androidApp:compileGooglePlayDebugKotlin`.
- [x] 1.2 Add `.folder` cases to the Swift switches in `iosApp/Source/Accounts/` (`AccountScreenContent.swift`, `AddAccountScreen.swift`, `AccountsScreen.swift`). Folder is never shown on iOS. Verify by review now, and by the macOS CI `build-ios-app` job (iOS cannot be built on Linux).
- [x] 1.3 Add the error codes `SU6 FolderUploadFailed`, `SU7 FolderNotAccessible`, `SD5 FolderDownloadFailed`, `SD6 FolderNotAccessible` to `ErrorCode.kt`, and map FOLDER in `syncDownloadErrorForAccount`. Document the codes in `website/content/en/faq/25-error-codes.md`. Verify with a unit test of the mapping.
- [x] 1.4 Create the `feedSync/folder` module (Gradle include, KMP targets as in `feedSync/googledrive`, Koin `expect`/`actual` module plugged into `shared/.../di/Koin.kt`). Verify with `./gradlew --quiet --console=plain :feedSync:folder:build`.

## 2. Folder settings and account state

- [x] 2.1 Implement `FolderSettings` with keys `FOLDER_SYNC_LOCATION`, `FOLDER_SYNC_LAST_UPLOAD_TIMESTAMP`, `FOLDER_SYNC_LAST_DOWNLOAD_TIMESTAMP` and `clearAll()`. Verify with a `FolderSettings` unit test (set, read, clear; no key collides with Dropbox or Google Drive keys).
- [x] 2.2 Update `AccountsRepository`: `setFolderAccount()`, FOLDER branch in `clearOtherSyncCredentials`, `getCurrentSyncAccount()`, `isSyncEnabled()`, FOLDER in `generateAndroidAccounts()` (both flavors) and `generateLinuxAccounts()` only. Thread `FolderSettings` through Koin and the test doubles (`JvmCloudDevice`, the Android cloud device factory). Verify with `AccountsRepository` unit tests: Folder is listed on Android and Linux and not listed on macOS, Windows, iOS; linking Folder clears other credentials; `isSyncEnabled()` is true for FOLDER.

## 3. Transports

- [x] 3.1 Define `FolderSyncDataSource` and `FolderNotAccessibleException` in `feedSync/folder` commonMain (design D2). Verify the module compiles.
- [x] 3.2 Implement `FolderSyncDataSourceJvm` (plain path: link check with a probe file, upload through `.<name>.tmp` and an atomic move with a fallback, download that throws `CloudBackupNotFoundException` for a missing file). Verify with jvmTest unit tests: upload creates and replaces the file, no `.tmp` stays, leftover `.tmp` is replaced, missing file throws `CloudBackupNotFoundException`, a folder that is not writable throws `FolderNotAccessibleException`.
- [x] 3.3 Implement `FolderSyncDataSourceAndroid` with `DocumentsContract` (child lookup by display name, write the tmp child with "wt", delete the final child, `renameDocument`, fallback to a direct write when rename is not supported, `persistedUriPermissions` access check, `releasePersistableUriPermission` on unlink). Verify with Robolectric tests in `androidHostTest` for upload, download, missing file, and revoked access; if a fake provider is not practical, test the write order through a small seam.

## 4. Worker integration

- [ ] 4.1 Add the FOLDER branches to `FeedSyncJvmWorker.accountSpecificUpload`/`accountSpecificDownload` and map upload failures for FOLDER to SU6/SU7 (Dropbox and Google Drive keep SU2). Update the `FolderSettings` timestamps. Verify with `FeedSyncJvmWorkerTest` cases for success, missing backup (`BackupNotFound`), and folder not accessible.
- [ ] 4.2 Add the same FOLDER branches to `FeedSyncAndroidWorker`. Verify with `./gradlew --quiet --console=plain :shared:testAndroidHostTest --tests "*FeedSyncAndroidWorker*"` or the equivalent host test task.
- [ ] 4.3 Onboard FOLDER into the cloud-sync harness (`CloudProvider.FOLDER` in `CloudStore.kt`, JVM and Android transports backed by `CloudStore`, `selectCloudAccount` branches, `CloudFeedAndCategoryRegressions` branch, provider entries in `DesktopCloudSyncSuccessTest`, `AndroidCloudSyncSuccessTest`, `AndroidCloudWorkManagerSuccessTest`, artifact exchange tests) as in `docs/CLOUD_SYNC_LIVE_TESTING.md`. Update the provider matrix in `docs/CLOUD_SYNC_TESTING.md`. Verify with `./gradlew --quiet --console=plain :shared:jvmTest :shared:testAndroidHostTest` (or the matching host test task) and a passing Folder run in the output.

## 5. ViewModel

- [ ] 5.1 Implement `FolderSyncViewModel` in `shared` commonMain (link with access check, `setFolderAccount`, `firstSync`, `fetchFeeds`, backup, unlink with release, `deleteAll`, `clearAccount`; UI state with last upload and last download dates). Register it in Koin for Android and desktop. Verify with a ViewModel unit test using Turbine: link success, link failure (not accessible), existing backup restored on link, unlink keeps the backup file.

## 6. Android UI

- [ ] 6.1 Add English strings (`folder_sync_*`, title, description, "not accessible" message) to `i18n/.../values/strings.xml`. Verify that the Lyricist code generation compiles.
- [ ] 6.2 Add the `FolderSync` `NavKey`, the `entry` in `MainActivity`, `onFolderClick` in `AccountsContent`, and the title and folder icon. Build the Folder screen with `OpenDocumentTree` and `takePersistableUriPermission` before `linkFolder`. Add a preview with `@PreviewPhone`. Verify with `./gradlew --quiet --console=plain :androidApp:assembleGooglePlayDebug` and `:androidApp:assembleFdroidDebug`, then on a device: link a Syncthing folder, mark an article as read, see the backup file appear in the folder.
- [ ] 6.3 Add `folder` to `E2eSeedAccount`, extend the REG-118 mock-state Maestro flow (Android, both flavors) to cover the Folder row, and record in `e2e/maestro/maestro-e2e-tests.md` that the real SAF picker is not covered. Verify that the touched Maestro flow passes on Android.

## 7. Desktop UI

- [ ] 7.1 Add a directory chooser helper (`JFileChooser` in `DIRECTORIES_ONLY` mode) that first checks `DesktopE2eHooks.fileDialogScript()`. Verify with a unit test of the scripted path (choose and cancel).
- [ ] 7.2 Add `FolderSyncScreen.desktop.kt` on `CloudAccountContent`, and FOLDER support in `DesktopAccountDetail`, `desktopTitle`, `desktopIcon`, `isBackupProvider`, `SettingsSearchIndex`, and `AccountTestContent`. Verify with `ProviderContentTest`/`ProviderContentSpectreTest` (now covering FOLDER) and `./gradlew --quiet --console=plain :desktopApp:compileKotlinJvm`.
- [ ] 7.3 Add a `DSK-` desktop E2E flow: link a temporary folder through the file dialog hook, trigger a backup, check that the backup file exists, unlink, check that the file stays. Update `e2e/desktop/README.md` and the Desktop column in `e2e/maestro/maestro-e2e-tests.md`. Verify with `e2e/scripts/run-desktop.sh desktoponly` (desktop E2E runs on macOS only; if no Mac is available, record that the flow was not run).
- [ ] 7.4 Run the desktop app on Linux with `./gradlew --quiet --console=plain :desktopApp:hotRunJvm`, link a folder, and confirm that the backup file appears and that a file copied from Android is merged.

## 8. Docs

- [ ] 8.1 Update the website FAQ: `02-sync-between-devices.md` (provider list and matrix, Folder on Android and Linux, also on F-Droid and Flathub), `22-what-sync-stores.md` if needed, `26-other-services.md` (Syncthing use), and a Flatpak note on `flatpak override --user --filesystem=<path> com.prof18.feedflow`. Mention last-writer-wins and that Syncthing conflict copies are ignored. Verify by reading the rendered Markdown and checking the facts against the code.

## 9. Integration gate

- [ ] 9.1 Run `./gradlew --quiet --console=plain detekt allTests` and fix all issues. Verify that the command passes and show the full output.
- [ ] 9.2 End-to-end check with Syncthing between a Linux desktop and an Android device: mark an article as read on one device, wait for Syncthing, open the other device, and confirm the article is read. Verify by observation, and record the result in the PR description.

## Workflow follow-up

- Push to a feature branch, open a PR, and let the macOS CI jobs validate the Swift switch changes.
- Archive the change after review and merge.
