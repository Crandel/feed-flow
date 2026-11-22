---
title: "Will FeedFlow support Feedly, Inoreader, Nextcloud News or other services?"
weight: 260
---

Supported today: iCloud, Dropbox, Google Drive, Folder (a backup file in a folder you choose, kept in sync by Syncthing or another file-sync tool), FreshRSS, Miniflux, BazQux and Feedbin. Many self-hosted readers that offer the Google Reader API can be used through the FreshRSS or Miniflux option.

- **Feedly** isn't planned: it doesn't offer API access to independent developers.
- **Nextcloud News, OneDrive, WebDAV and others**: requested, not planned yet. Add your vote to the [open requests on GitHub](https://github.com/prof18/feed-flow/issues).
- **Dropbox and Google Drive aren't in the F-Droid and Flathub builds.** Folder sync is available in those builds instead: point FeedFlow at a folder that Syncthing or a similar tool keeps in sync.

To move from another service now, export an OPML file there and import it into FeedFlow.
