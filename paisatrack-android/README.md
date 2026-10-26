# PaisaTrack Android

Standalone native Android client for PaisaTrack. It does not modify the existing CAP Compass app.

## Current implementation

- Kotlin, Jetpack Compose, Material 3, MVVM, Navigation Compose, Room/SQLite, Coroutines/Flow, WorkManager and Android Keystore.
- Home, Transactions, Insights, Bills, Settings, onboarding/privacy, transaction entry/review/detail, bill management and error/permission states.
- Amounts use integer paise in the domain and encrypted Room columns. No financial records or message bodies are sent to a server.
- Transaction summaries count only confirmed records. Refunds and reversals adjust the right totals; own-account transfers and credit-card repayments are excluded from spending.
- SMS alerts are parsed locally into pending candidates; OTPs and unrelated message content are not stored. SMS import is an optional, restricted-permission feature.
- Reminders use inexact WorkManager requests. No guessed due dates or exact alarms are used. Notification denial pauses scheduling without removing bills.
- CSV export uses Android's system document picker. Delete all removes user data, import checkpoints and sensitive-data keys.
- Synthetic finance and SMS records exist only in `app/src/debug` Compose previews, marked as preview-only.
- Public privacy policy: [PRIVACY_POLICY.md](PRIVACY_POLICY.md).

## Google Play SMS policy and build variants

`manual` is the Play-safe default variant and does not declare `READ_SMS` or `RECEIVE_SMS`; it remains fully usable with manual transaction and bill entry. `smsReview` adds the SMS permissions and incoming-message receiver. Google Play treats these as restricted permissions and may require an approved exception/Permissions Declaration Form for a qualifying money-management use case. Approval is not guaranteed. Do not publish the `smsReview` variant until Google Play has approved the use case and the submitted build, disclosure, permission request and Data safety form have been reviewed together.

Only publish one flavor for a given application ID/version code. If SMS access is not approved, use `manualRelease`. The app requests SMS permission only after an in-app disclosure and explicit consent, handles denial/revocation, and always permits manual use. The permission declaration and policy may change; check the current [Google Play SMS and Call Log permissions policy](https://support.google.com/googleplay/android-developer/answer/10208820) before submission.

## Build and verification

Requires Android Studio with Android SDK 36 and JDK 17 or newer (Android Studio's bundled JBR works).

```powershell
./gradlew.bat :app:testManualDebugUnitTest :app:testSmsReviewDebugUnitTest
./gradlew.bat :app:assembleManualDebug :app:assembleSmsReviewDebug
./gradlew.bat :app:bundleManualRelease
```

Minimum SDK is 24 (Android 7.0), retaining a broad supported device range and meeting the current WorkManager 2.12 runtime floor. Compile and target SDK are 36, aligned with current Play submission target requirements at this release date. Kotlin `2.2.21` and KSP `2.3.10` are pinned together so the processor and Android dex toolchain use compatible Kotlin metadata.

Release signing is not stored in source control. To produce a signed bundle, set all four environment variables `PAISATRACK_UPLOAD_STORE_FILE`, `PAISATRACK_UPLOAD_STORE_PASSWORD`, `PAISATRACK_UPLOAD_KEY_ALIAS`, and `PAISATRACK_UPLOAD_KEY_PASSWORD`. With no signing values, the release task creates an unsigned local artifact that cannot be uploaded to Play as-is.

## Key dependencies

- Compose BOM `2026.06.00`, Material 3, Activity Compose `1.12.4`, Navigation Compose `2.9.8`, Lifecycle `2.10.0`.
- Room `2.8.5` with KSP `2.3.10`; Gradle plugin/Kotlin versions are pinned in the root build files.
- WorkManager `2.12.0`, Coroutines `1.10.2`, JDK 17 APIs via desugaring.
