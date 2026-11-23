# IRCTC Safe Assistant for Android

A lightweight Android WebView shell that opens the official IRCTC website in-app and offers user-triggered passenger-field autofill.

## Build
Open the `android/` folder in Android Studio (JDK 17, Android SDK 35), sync Gradle, then Build > Build APK(s).

A GitHub Actions workflow builds a debug APK on pushes and pull requests. Download it from the workflow run's Artifacts section.

## Safety and scope
- Uses the official IRCTC website; no unofficial booking API.
- CAPTCHA, OTP, login, booking submission, and payment remain manual.
- Autofill runs only after the user taps Fill details; review all values before continuing.
- No automatic form submission, CAPTCHA solving, OTP interception, payment automation, or credential storage.
- Passenger details are held in the current app screen only and are not persisted by this app.

IRCTC may restrict embedded browsers or change its form fields. If so, use the official IRCTC app or supported browser.
