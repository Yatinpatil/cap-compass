# PaisaTrack Privacy Policy

**Effective date:** October 26, 2026  
**Publisher:** Yatin Patil  
**Privacy contact:** [GitHub profile](https://github.com/Yatinpatil)

This policy applies to the PaisaTrack Android application (`com.paisatrack.app`). PaisaTrack is an offline-first personal finance app. It does not require an account and does not send finance records or SMS content to a PaisaTrack server.

## Information stored on your device

PaisaTrack stores transaction amounts, transaction type, merchant or description, date, category, optional notes, recurring payment details, reminder preferences, and app settings in its private on-device database. Sensitive finance fields and SMS import checkpoint values are encrypted with AES-GCM using keys held by Android Keystore. The app disables Android backup for its private data. Records remain on the device until you delete them, clear app storage, or uninstall PaisaTrack.

You can export confirmed transactions and saved bills as CSV. Export happens only after you choose it and select a destination in Android's document picker. Files saved outside PaisaTrack are under the destination's control and are not removed by PaisaTrack's delete action.

## Optional SMS transaction tracking

The `manual` app variant does not declare or request `READ_SMS` or `RECEIVE_SMS`. Manual transaction entry and bill tracking work without SMS access.

The separate `smsReview` variant can request Android's restricted SMS permissions. SMS tracking is off until the app presents an in-app disclosure, you consent, and you grant the Android runtime permissions. When enabled, PaisaTrack checks inbox messages received in the most recent 90 days and processes new SMS alerts locally to identify eligible financial transactions.

The app does not upload SMS. Message bodies are transient parser input and are not stored in PaisaTrack's database. OTP and non-transaction messages are excluded from transaction creation. For an eligible alert, PaisaTrack stores only parsed transaction fields as a **pending review** candidate and a keyed fingerprint used for duplicate detection. A candidate does not affect spending totals until you review and confirm it. You can turn tracking off in Settings or revoke permission through Android settings.

Google Play restricts SMS permissions. The `smsReview` variant must not be distributed through Google Play unless the app's use case and permission declaration have been approved. Approval is not guaranteed. If SMS permission is unavailable or denied, manual features remain usable.

## Bills and notifications

Bill reminders use Android WorkManager on this device. Android may delay reminders; PaisaTrack does not promise exact delivery and does not use exact alarms. Android 13 and later may require notification permission. Reminder notifications use generic text and do not show bill names or amounts. Denying or revoking notification permission pauses reminders but does not delete bills.

PaisaTrack does not infer a payment due date for scheduling. A date marked as inferred must be confirmed by you before a reminder can be enabled.

## Sharing, analytics, and advertising

PaisaTrack has no account service, analytics SDK, advertising SDK, or cloud sync. The app does not sell or share your finance records or SMS content. Android and Google Play handle app installation, permission dialogs, and notification delivery under their own terms.

## Your choices and deletion

- Use manual entry without SMS permissions.
- Enable or turn off SMS tracking in Settings, or revoke its permissions in Android settings.
- Allow or deny notification permission. You can change it later in Android settings.
- Export your saved records as CSV to a destination you choose.
- Use **Delete all data** in Settings to remove saved transactions, recurring payments, reminders, custom categories, settings, and SMS import checkpoints from PaisaTrack and delete the associated encryption keys.

Deleting app data does not remove CSV files you previously exported or messages that remain in your Android SMS app.

## Children's privacy

PaisaTrack is a general personal finance utility and is not designed for children. It does not knowingly collect information from children or transmit user information to a server.

## Policy changes

This policy may be updated when app behavior changes. The effective date above will be revised when a new policy is published. The policy text and app's Google Play Data safety disclosures should be reviewed together for each release.

## Contact

For privacy questions or requests, contact the publisher through the [Yatinpatil GitHub profile](https://github.com/Yatinpatil).
