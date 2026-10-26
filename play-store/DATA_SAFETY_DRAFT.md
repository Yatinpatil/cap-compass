# Google Play Data safety review draft

This is a source-code review aid, not a submitted Play Console declaration. Recheck the generated AAB and the current Play Console definitions before completing the form.

## What the current project does

- Planner profile values, preference choices and checklist progress are stored in local app storage. They are not sent to a CAP Compass backend; this project has no account system or developer-operated service.
- Android cloud backup and device-to-device app-data transfer are disabled.
- The app has no analytics, advertising, crash-reporting or sign-in SDK in its declared dependencies.
- The app can place a preference list on the clipboard, open Android's system share sheet with list text, and export a CSV to the share sheet from app cache. These actions happen only after the user taps the corresponding control; the recipient app or person is outside CAP Compass's control.
- The only external website link found in app code is the official State CET Cell portal. It opens after a user action in an external browser.

## Form review prompts

- Confirm all claims against the exact AAB and every bundled SDK before selecting that no data is collected or shared by the developer.
- Review how Play Console treats user-requested transfers to a clipboard, selected share target and exported CSV. Describe the handoff accurately if the questionnaire asks about it; the app's own privacy policy explains these optional actions.
- No user account can be created in the app, so there is no in-app account deletion flow. Users can clear planner records with **Reset plan** or uninstall the app.
- Do not state that CAP Compass encrypts planner data. The app relies on Android's app storage sandbox and adds no encryption layer.
- Complete the age/audience and content-rating questions separately. The app is an admissions planner and the policy says it is not directed to children under 13.

## Code areas reviewed

- `src/lib/storage.ts` for local planner persistence
- `src/lib/native.ts` for clipboard, sharing, CSV export and external links
- `package.json` for declared runtime dependencies
- `android/app/src/main/AndroidManifest.xml` for Android permissions and backup settings
