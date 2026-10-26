# Google Play release checklist

## Prepared in this project

- [x] Android application ID is set to `com.capcompass.app`.
- [x] Target SDK is Android 16 / API 36.
- [x] Android release version is `1.0.0` with version code `1`.
- [x] A signed Android App Bundle build script is available as `npm run android:bundle`.
- [x] Upload-key values are read only from environment variables; keystore files and secrets are excluded from source control.
- [x] Android cloud backup and device-transfer backup are disabled for local planner data.
- [x] Privacy policy is reachable inside the app and is built from `public/privacy-policy.html`.
- [x] Store listing text, app icon, feature graphic and four mobile screenshots are prepared in `play-store/`.
- [x] Android launcher icons use the CAP Compass compass mark and match the store icon.
- [x] A Data safety review draft is included in `play-store/DATA_SAFETY_DRAFT.md`.

## Required before upload

- [ ] Confirm `com.capcompass.app` is available and create the app in the intended Play Console developer account.
- [ ] Create an RSA upload key, store a secure backup outside this repository, and enroll in Play App Signing. Never commit the keystore or passwords.
- [ ] Set `CAP_COMPASS_UPLOAD_STORE_FILE`, `CAP_COMPASS_UPLOAD_STORE_PASSWORD`, `CAP_COMPASS_UPLOAD_KEY_ALIAS` and `CAP_COMPASS_UPLOAD_KEY_PASSWORD` in the build environment, then run `npm run android:bundle`.
- [ ] Publish `public/privacy-policy.html` at a public, non-geofenced HTTPS URL and enter that URL in Play Console. Add a live support email to the store listing; the privacy policy points users to that contact.
- [ ] Review the four captured 1080×1920 screenshots against the signed Android release candidate. Replace them if native system bars or layout differ, and confirm no personal data is present.
- [ ] Complete the Play Console Data safety form based on the final AAB and current Google definitions, including user-selected share and CSV export behavior.
- [ ] Complete the Store listing, App content, Data safety, IARC rating, target-audience, ads, app-access, pricing and country/region sections in Play Console.
- [ ] Install and exercise the signed release candidate on supported Android devices, including Android 16 edge-to-edge layout, navigation/back behavior, clipboard, sharing/CSV export, local persistence, reset and external CET Cell links.
- [ ] Upload the AAB to internal testing first. Review Play Console's artifact/pre-launch checks and fix issues before closed testing or production.
- [ ] If the developer account is a personal account created after November 13, 2023, run a closed test with at least 12 opted-in testers continuously for 14 days, then apply for production access.
- [ ] Set up release monitoring and confirm the public developer identity, support contact and privacy page before rollout.

## Signing environment

Use an absolute path to the upload keystore. Set the four `CAP_COMPASS_UPLOAD_*` environment variables only in the local build shell or a protected CI secret store. The release bundle is written to:

`android/app/build/outputs/bundle/release/app-release.aab`

The Play Console release itself is intentionally not created or published by this project build.
