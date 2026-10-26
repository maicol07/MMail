# MMail

Android email client built with Kotlin and Compose, using Material 3 Expressive. Outlook and Microsoft 365 mail are accessed through Microsoft Graph v1.0.

## Modules

- `sharedUI`: library containing the email model, provider contract, Android Outlook integration, theme, and screens. Uses the Kotlin Multiplatform plugin with Android as its only target.
- `androidApp`: Android application, minimum API 26.

## Getting started

Open the folder in Android Studio or IntelliJ IDEA. Use Android Studio's bundled JDK and configure `sdk.dir` in `local.properties`. The Android build requires SDK 37.2.

```powershell
.\gradlew.bat :androidApp:assembleDebug
```

On macOS/Linux, use `./gradlew` instead of `.\gradlew.bat`.

## Outlook setup

1. Create an application in Microsoft Entra **App registrations**. Choose accounts in any organizational directory and personal Microsoft accounts.
2. Add the **Android** platform with package `it.maicol07.mmail` and the Base64-encoded SHA-1 hash of the certificate signing your APK. Use the signature hash instructions in [Microsoft's Android registration guide](https://learn.microsoft.com/en-us/entra/identity-platform/quickstart-register-app). Debug and release certificates require their own registered redirects.
3. Add Microsoft Graph **delegated** permissions `Mail.ReadWrite` and `Mail.Send`. Work/school tenants may require administrator consent. No client secret is needed or supported.
4. Add the following public registration values to the existing ignored `local.properties`, preserving `sdk.dir`:

```properties
outlook.clientId=YOUR_APPLICATION_CLIENT_ID
outlook.signatureHash=YOUR_BASE64_SHA1_SIGNATURE_HASH
```

Use the raw Base64 signature hash here, including any `+`, `/`, and trailing `=` characters. MMail constructs the encoded `msauth://it.maicol07.mmail/<signature-hash>` redirect for MSAL and the matching Android intent filter. Rebuild after changing these properties.

Without this configuration, MMail builds and displays a setup message with sign-in disabled. Sign in with Microsoft to load real mail. Authentication uses the system browser; [MSAL](https://github.com/AzureAD/microsoft-authentication-library-for-android) manages cached credentials and silent token renewal. Signing out clears the app's account and in-memory mail/draft state; it does not sign out the browser's Microsoft session.

## Verification

```powershell
.\gradlew.bat :sharedUI:testAndroidHostTest :androidApp:assembleDebug :androidApp:assembleDebugAndroidTest
```

Host tests cover the mailbox model, Graph response validation, pagination, token destination checks, mutation payloads, and error handling without a live account. Android UI tests use an injected test provider and cover the filter bottom sheet, compact navigation, and attachment actions. Run them on a connected device or emulator with `:androidApp:connectedDebugAndroidTest`. Live Microsoft sign-in and token renewal require a configured registration and a test mailbox.

## Mail behavior and limits

The UI follows the device language, with English as the default and Italian translations. Production contains no sample mail or hardcoded account.

The UI includes search, filters in a bottom sheet opened from the top bar, starred messages, archiving, trash with undo, reading, and composing with replies. Attachments use rounded action chips. The layout shows the message list and detail from 840 dp, and permanent folder navigation from 1200 dp. Below these thresholds, it uses one screen at a time and a drawer, with shared transitions for avatars, subjects, and attachments.

The theme follows the system, with dynamic colors on Android 12+ and a green seed color on earlier versions. Navigation and list animations use Material Expressive motion tokens and respect the system animation scale. Material Symbols Rounded icons are generated with SymbolCraft.

Mail is refreshed after sign-in, startup, sending, and manual refresh from the folder drawer. Refresh loads only the first 100 summaries from each of Inbox, Sent Items, Archive, and Deleted Items. Use **Load more messages** in the list to retrieve another page; opening a message fetches its full plain-text body. Startup does not wait for the entire mailbox. Search and counters cover loaded messages and content, initially including previews rather than full bodies. Custom/nested folders, offline storage, delta synchronization, and background refresh are not implemented. Starred corresponds to Outlook flags in loaded folders excluding trash; its load-more action follows those folders' remaining pages.

Reading, flagging, archiving, moving to trash, undo, sending, and replying update Graph. Moves use [immutable IDs](https://learn.microsoft.com/en-us/graph/outlook-immutable-id). Sending uses Graph's `sendMail` or `reply` endpoint and is never retried automatically. A successful response means [accepted for processing](https://learn.microsoft.com/en-us/graph/api/user-sendmail), not confirmed delivery. If sending cannot be confirmed, the draft remains available and an error asks you to check Outlook Sent mail before retrying.

Attachment chips open the original message in Outlook using the system browser. Native attachment downloads/uploads and HTML rendering are not implemented. Drafts are local to the running UI and are not saved to Outlook. Mail changes persist on the server, while the in-memory mailbox is reloaded when the Activity is recreated.

Theme and motion reference: [Material 3 Expressive in Compose](https://android-developers.googleblog.com/2025/05/androidify-building-delightful-ui-with-compose.html).

## License

Copyright (c) 2026 Maicol Battistini.

MMail is licensed under the GNU General Public License version 3 only (`GPL-3.0-only`). See [LICENSE](LICENSE) for the full terms. Third-party dependencies and assets retain their respective licenses.
