# Real Simple MD editor

An Android app for simply reading and editing Markdown (`.md`) files.
It opens in reading mode, and one button switches to editing.

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
