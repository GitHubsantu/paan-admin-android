# Safar Paan Admin

Native Android admin app for the Safar Paan pickup-ordering shop. The shop owner uses it to run the order queue, manage the menu and customers, and get a loud full-screen alert when a new order arrives.

Package: `com.paan.safaadmin` · Talks to the PAAN Laravel API (admin role).

## Features

- Admin login with a saved session
- Live order queue and order detail, with status updates
- Dashboard with shop overview
- Menu management: sold-out toggle, categories and products (create, edit, delete, photo upload)
- Customers: list, create, edit, block, delete, and a recycle bin
- Shop settings
- Firebase push notifications: full-screen lock-screen alert with a custom sound for new orders, and a setup screen for alert permissions
- QR code generation

## Tech stack

- Kotlin, Jetpack Compose, Material 3
- MVVM with repositories and StateFlow
- Hilt for dependency injection
- Navigation Compose
- Retrofit + OkHttp + kotlinx.serialization
- DataStore (session)
- Coil (images)
- ZXing (QR codes)
- Firebase Cloud Messaging

## Getting started

Requirements: Android Studio (current stable), JDK 17, an Android device or emulator.

1. Clone the repo and open it in Android Studio.
2. Add your Firebase config: download `google-services.json` from the Firebase console into `app/`.
3. Check `API_BASE_URL` (a `buildConfigField` in `app/build.gradle.kts`) points to your PAAN API.
4. Run the `app` configuration.

`google-services.json`, keystores and `keystore.properties` are git-ignored. Never commit them.

## Building release APKs

The release script builds separate 32-bit and 64-bit APKs plus one universal APK, verifies them, and puts them in `dist/` with SHA-256 hashes:

```powershell
powershell -ExecutionPolicy Bypass -File tools\build-release.ps1
powershell -ExecutionPolicy Bypass -File tools\build-release.ps1 -VersionName 1.1.0   # bump version
```

| APK | Use for |
|---|---|
| `SafarPaanAdmin-<version>-v8a-64bit.apk` | Most modern phones |
| `SafarPaanAdmin-<version>-v7a-32bit.apk` | Older 32-bit phones |
| `SafarPaanAdmin-<version>-universal.apk` | Any device (largest) |

Full setup (signing, Gradle config, publishing a GitHub release) is in [docs/RELEASE.md](docs/RELEASE.md).

## Project structure

```
app/src/main/java/com/paan/safaadmin/
  core/        utilities (formatting, multipart, QR)
  data/        remote (Retrofit API, models), local (session), repository
  di/          Hilt modules
  domain/      domain models
  navigation/  routes and nav host
  push/        FCM service, notification channels, new-order alert
  ui/          theme, components, screens
tools/         build-release.ps1
docs/          RELEASE.md
```

## Security

Do not commit signing keys or Firebase config. Keep a backup of your release keystore: without it, installed apps cannot be updated.

## License

[MIT](LICENSE) © 2026 Dharanidhar Mahata ([imdevops.in](https://imdevops.in))