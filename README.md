# English Logic Android 1.3.0

Offline Android package for the private English Logic learning app.

- Package: `vn.englishlogic.app`
- Minimum Android: 6.0 (API 23)
- Target Android: API 35
- Content: bundled in the APK; ChatGPT sign-in is not required
- Permissions: Internet, microphone (only requested when recording IPA), and camera (only requested when scanning a pairing QR)
- Local features: 60-session course, grammar map, exercises, vocabulary, review, IPA recording, dark mode, JSON import/export and back navigation
- Storage: offline-first normalized v2 repository in origin-scoped WebView localStorage
- Upgrade: legacy `english-logic-v0-progress` data is migrated non-destructively and retained as a backup
- Local identity: random `local_user_id` and `device_id` UUIDs are created on first run; no email, password or OAuth is used
- Optional PC pairing: scan a QR or enter an eight-character one-time code; pairing is never required to learn
- Credential storage: the permanent device token is encrypted with an AES-GCM key held by Android Keystore; no API key or server secret is bundled
- Change tracking: stable record IDs, timestamps, per-record revisions, sync states and tombstones remain local; Phase 4 pairs devices but does not upload or download learning progress
- Offline behavior: all lessons and progress storage continue to work with the network and pairing server unavailable

The GitHub Actions workflow builds a debug-signed APK suitable for direct installation. A Play Store release should use a private release keystore and an Android App Bundle.
