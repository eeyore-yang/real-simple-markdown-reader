# Real Simple MD editor

An Android app for simply reading and editing Markdown (`.md`) files.
It opens in reading mode, and one button switches to editing.

## Features

- Open `.md` files from the app or with "Open with" from a file manager
- Reading mode (rendered) ↔ editing mode (source) with one button, single pane
- Auto-save (1.5 s after you stop typing, and when switching modes or leaving the app)
- Adjustable font size, remembered across launches
- Calm reading style: phrase-based Korean line breaking (Android 13+), soft headings,
  centered text column on wide screens (DeX)
- Recent files list

## Shortcuts (DeX / hardware keyboard)

| Input | Action |
|---|---|
| Ctrl + mouse wheel | Font size bigger / smaller |
| Ctrl + `+` / Ctrl + `-` | Font size bigger / smaller |
| Ctrl + `0` | Reset font size |
| Ctrl + E | Toggle reading / editing |
| Ctrl + S | Save now |
| Ctrl + O | Open a file |
| Pinch (touch) | Font size |

## Getting the APK

Every push is built by GitHub Actions.

1. Repository → **Actions** tab → open the latest **Build APK** run
2. Download **real-simple-md-editor-apk** under *Artifacts* and unzip it
3. Install `app-debug.apk` on the phone (allow installing from unknown sources)

All builds are signed with the same key (`keystore/debug.keystore`), so a new
build installs over the old one without uninstalling. This key is for personal
sideloading only and is not meant for Play Store distribution.

## Building locally

Requires JDK 17+ and the Android SDK (API 35).

```
./gradlew assembleDebug
```
