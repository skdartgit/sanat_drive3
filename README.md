# SKD Data Drive

Android personal data drive.

## Project

- Project: sanat_drive
- Package: com.sanat.drive
- Language: Kotlin
- Build scripts: Gradle Kotlin DSL
- Gradle: 9.5.0
- Android Gradle Plugin: 9.3.0
- JDK: 17

## Default PIN

123456

Change the PIN from:

Admin Panel -> Reset PIN

## GitHub Actions

The workflow:

.github/workflows/android.yml

builds:

app/build/outputs/apk/debug/app-debug.apk

The APK is uploaded as a GitHub Actions artifact.

## Main navigation

- My Notes
- My Portfolio
- My Credentials
- Add Note
- Add SIP
- Add Credentials
- Export/Restore All
- Admin Panel
