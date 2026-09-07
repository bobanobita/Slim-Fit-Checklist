# English Logic Android 1.2.0

Offline Android package for the private English Logic learning app.

- Package: `vn.englishlogic.app`
- Minimum Android: 6.0 (API 23)
- Target Android: API 35
- Content: bundled in the APK; ChatGPT sign-in is not required
- Permissions: Internet and microphone (only requested when recording IPA)
- Local features: 60-session course, grammar map, exercises, vocabulary, review, IPA recording, dark mode, JSON import/export and back navigation
- Storage: offline-first normalized v2 repository in origin-scoped WebView localStorage
- Upgrade: legacy `english-logic-v0-progress` data is migrated non-destructively and retained as a backup
- Local identity: random `local_user_id` and `device_id` UUIDs are created on first run; they are not uploaded in Phase 2
- Change tracking: stable record IDs, timestamps, per-record revisions, sync states and tombstones are ready for a later sync phase; this build makes no network sync requests

The GitHub Actions workflow builds a debug-signed APK suitable for direct installation. A Play Store release should use a private release keystore and an Android App Bundle.
