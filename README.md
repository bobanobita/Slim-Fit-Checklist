# English Logic Android

Offline Android package for the private English Logic learning app.

- Package: `vn.englishlogic.app`
- Minimum Android: 6.0 (API 23)
- Target Android: API 35
- Content: bundled in the APK; ChatGPT sign-in is not required
- Permissions: Internet and microphone (only requested when recording IPA)
- Local features: 60-session course, grammar map, exercises, vocabulary, review, IPA recording, WebView storage, JSON import/export and back navigation

The GitHub Actions workflow builds a debug-signed APK suitable for direct installation. A Play Store release should use a private release keystore and an Android App Bundle.
