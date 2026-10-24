# MMail

Demo Android email client built with Kotlin and Compose, using Material 3 Expressive.

## Modules

- `sharedUI`: library containing the email model, sample data, theme, and screens. Uses the Kotlin Multiplatform plugin with Android as its only target.
- `androidApp`: Android application, minimum API 26.

## Getting started

Open the folder in Android Studio or IntelliJ IDEA. Use Android Studio's bundled JDK and configure `sdk.dir` in `local.properties`. The Android build requires SDK 37.2.

```powershell
.\gradlew.bat :androidApp:assembleDebug
```

On macOS/Linux, use `./gradlew` instead of `.\gradlew.bat`.

## Verification

```powershell
.\gradlew.bat :sharedUI:testAndroidHostTest :androidApp:assembleDebug :androidApp:assembleDebugAndroidTest
```

Host tests cover search and filters, marking messages as read, starring, moving between folders, and simulated sending with field validation. Android UI tests also cover the filter bottom sheet, compact navigation, and attachment actions. Run them on a connected device or emulator with `:androidApp:connectedDebugAndroidTest`.

## Demo behavior

The UI includes search, filters in a bottom sheet opened from the top bar, starred messages, archiving, trash with undo, reading, and composing with replies. Attachments use rounded action chips. The layout shows the message list and detail from 840 dp, and permanent folder navigation from 1200 dp. Below these thresholds, it uses one screen at a time and a drawer, with shared transitions for avatars, subjects, and attachments.

The theme follows the system, with dynamic colors on Android 12+ and a green seed color on earlier versions. Navigation and list animations use Material Expressive motion tokens and respect the system animation scale. Material Symbols Rounded icons are generated with SymbolCraft.

There are no providers, authentication, email connections, or persistent storage. Sending adds a message to the Sent folder for the current session only. Attachments are sample filenames with no associated files. Mailbox changes reset when the app restarts or the Android Activity is recreated.

Theme and motion reference: [Material 3 Expressive in Compose](https://android-developers.googleblog.com/2025/05/androidify-building-delightful-ui-with-compose.html).
