---
title: "What do the error codes mean?"
weight: 250
---

Error messages end with a code that tells where the problem happened. Include it if you report the issue.

| Code | What failed |
|---|---|
| DB1–DB3 | Reading or saving data on your device |
| SU1–SU7 | Uploading your data to the sync account (SU2–SU4 Dropbox, SU5 Google Drive; SU6–SU7 the synced folder, where SU7 means the folder no longer exists, is not writable, or access was removed) |
| SD1–SD6 | Downloading your data (SD1 Dropbox, SD2 iCloud, SD3 Google Drive; SD4 means Google Drive needs you to sign in again; SD5–SD6 the synced folder, where SD6 means the folder no longer exists, is not readable, or access was removed) |
| SF1–SF2 | Syncing feeds or articles with the sync account |
| SI1–SI7 | iCloud: check that you're signed in, iCloud Drive is on and you have space available |
| FS1–FS10 | A request to your reader service (FreshRSS, Miniflux, BazQux, Feedbin) failed |
| FS11 | The reader service rejected the sync token: check that your proxy forwards the `Authorization` header |

Many of these are temporary network problems and disappear on the next sync. If the same code keeps coming back, [open an issue](https://github.com/prof18/feed-flow/issues) or write from **Settings → About & support → Report issue**.

Older versions showed **FF1** when a feed couldn't be downloaded. Current versions show a warning icon on the feed instead.
