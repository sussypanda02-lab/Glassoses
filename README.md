# GlassOS – iOS 26-inspired Android launcher (Kotlin + Jetpack Compose)

## Build & install
1. Android Studio Ladybug (2024.2) or newer, JDK 17. File > Open > this folder. Let Gradle sync (it fetches Gradle 8.9 from gradle-wrapper.properties).
2. Run on a device/emulator (API 29+; Liquid Glass blur needs API 31+). Or: `gradle assembleDebug` -> app/build/outputs/apk/debug/app-debug.apk, then `adb install -r` it.

## Set as default home app
Open it once > swipe down from the top-right corner > gear (Settings) > "Set as default home app".
Or: Android Settings > Apps > Default apps > Home app > GlassOS. Press Home.

## Gestures
- Swipe down on a home page: Spotlight search. Swipe down in the top-right corner (just under the status bar): Control Center.
- Swipe right of page 1: Today View (widgets). Swipe left past the last page: App Library.
- Long-press empty space: edit mode (drag icons, tap the minus, "+" adds a widget). Long-press an icon: shortcuts, Dock, info, uninstall.

## Android limits (by design)
Wi-Fi, Bluetooth, airplane mode and mobile data cannot be toggled by normal apps: those tiles show real state and open the system panel.
Brightness and rotation lock need the "Modify system settings" permission (requested on first use). Focus needs Do Not Disturb access.
Dynamic Island is a simulation; it shows charging, a demo timer and real notifications once Notification Access is granted.
