# Minimal TCL / Android TV Launcher (`com.example.minimaltv`)

A distraction-free, ultra-fast Kotlin Jetpack Compose Android TV Launcher designed for TCL and Google TV / Android TV devices (`minSdk 26`, `targetSdk 34`).

## Features

- **4 D-Pad Focusable Streaming Cards**:
  1. **YouTube** (`com.google.android.youtube.tv`)
  2. **Netflix** (`com.netflix.ninja`)
  3. **Hotstar** (`in.startv.hotstar` with fallback `in.startv.hotstar.dplus.tv`)
  4. **Prime Video** (`com.amazon.amazonvideo.livingroom`)
- **Top Live Clock & Date Header**: Real-time clock (`hh:mm a`) and date display.
- **D-Pad Focusable Settings Button**: Launches Android TV system settings (`android.settings.SETTINGS`).
- **Native Leanback & Home Intent Support**: Registers `HOME`, `DEFAULT`, and `LEANBACK_LAUNCHER` categories plus Android 11+ `<queries>` declarations in `AndroidManifest.xml`.
- **Cloud APK Build**: Automatically builds `app-debug.apk` on GitHub Actions using Java 17 (Temurin) and Gradle 8.7.
