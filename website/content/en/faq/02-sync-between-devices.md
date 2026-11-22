---
title: "How do I sync FeedFlow between my devices?"
weight: 20
featured: true
---

Go to **Settings → Sync & refresh → Accounts** and connect the same account on every device. FeedFlow supports two kinds of accounts:

- **Backup services**: iCloud, Dropbox, Google Drive and Folder. Your feeds, categories, read articles and bookmarks are saved to a file in your own storage and picked up by your other devices.
- **Reader services**: FreshRSS, Miniflux, BazQux and Feedbin. Your feeds and read status live on the server, and FeedFlow syncs with it.

Folder is the backup service that needs no account: FeedFlow saves the sync file in a folder you pick on each device, and a file-sync tool like [Syncthing](https://syncthing.net/) moves the file between the devices. The folder may hold your other files; FeedFlow reads and writes only its own sync file.

You can connect one account at a time. Sync runs when feeds are refreshed and when you close the app; on iPhone and iPad, changes are also uploaded every couple of minutes while FeedFlow is open. **Back up now** sends your latest changes right away. Your other devices pick up the changes the next time they refresh, so on iPhone and iPad, where background refresh is up to iOS, open the app to see them.

Not every account is available in every version:

| Version | iCloud | Dropbox | Google Drive | Folder | Reader services |
|---|---|---|---|---|---|
| iPhone, iPad, Mac (App Store) | ✓ | ✓ | ✓ | | ✓ |
| Android (Google Play) | | ✓ | ✓ | ✓ | ✓ |
| Android (F-Droid) | | | | ✓ | ✓ |
| Windows | | ✓ | ✓ | | ✓ |
| Linux `.deb`, `.rpm`, AppImage | | ✓ | ✓ | ✓ | ✓ |
| Linux (Flathub) | | | | ✓ | ✓ |

F-Droid doesn't allow the non-free Dropbox and Google libraries, and Flathub builds the app from public sources, so the private keys these services require can't be included. Folder needs no account or extra libraries, so it works in those versions too. To sync these versions with your other devices, use a reader service or Folder, or install the Google Play version on Android or the `.deb`, `.rpm` or AppImage package on Linux.

**Folder details**

- Pick the folder once on each device: on Android in the system folder picker, with access kept across restarts; on Linux in a folder dialog. Unlinking never deletes the sync file.
- Sync runs at the same moments as for the other backup services, so the other device picks up a change after the next refresh, once the sync tool has copied the file.
- If both devices change the same data before the file has been copied, the most recent change wins. A sync tool keeps the losing version as a conflict copy (for Syncthing, a `.sync-conflict` file); FeedFlow ignores that copy.
- Flatpak: the app can write only inside its sandbox. Allow it to reach the folder once: `flatpak override --user --filesystem=<path> com.prof18.feedflow`.
