# Release guide

One Gradle run produces three signed APKs. `tools/build-release.ps1` runs it, checks every APK and copies the results to `dist/`.

| File | For |
|---|---|
| `SafarPaanAdmin-<version>-v8a-64bit.apk` | Modern phones (arm64-v8a). Pick this if unsure. |
| `SafarPaanAdmin-<version>-v7a-32bit.apk` | Older 32-bit phones (armeabi-v7a). |
| `SafarPaanAdmin-<version>-universal.apk` | Works on any device, largest file. |

## 1. One-time setup

### a) Gradle: ABI splits and signing

The ready-made `app/build.gradle.kts` in this repo already has this. For reference, these are the parts that were added:

```kotlin
import java.util.Properties

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    signingConfigs {
        create("release") {
            if (keystoreProps.isNotEmpty()) {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            if (keystoreProps.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
            // keep your existing isMinifyEnabled / proguardFiles settings here
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = true
        }
    }
}
```

The script reads `versionCode = N` and `versionName = "x.y.z"` from the `defaultConfig` block of this file, so keep them as plain literals.

### b) Release keystore

```powershell
keytool -genkeypair -v -keystore release-keystore.jks -alias safarpaanadmin `
  -keyalg RSA -keysize 2048 -validity 10000
```

Copy `keystore.properties.example` to `keystore.properties` and fill it in. Back up the `.jks` file and its passwords somewhere safe: if you lose them, existing installs can never be updated.

### c) Firebase

Put `google-services.json` from the Firebase console into `app/`. It is git-ignored.

### d) API address

`BuildConfig.API_BASE_URL` is set per build type in `app/build.gradle.kts` and read by `NetworkModule.kt`. Change it there if your API address changes.

## 2. Build

From the project root:

```powershell
powershell -ExecutionPolicy Bypass -File tools\build-release.ps1
powershell -ExecutionPolicy Bypass -File tools\build-release.ps1 -VersionName 1.1.0
powershell -ExecutionPolicy Bypass -File tools\build-release.ps1 -Clean
powershell -ExecutionPolicy Bypass -File tools\build-release.ps1 -VersionName 1.1.0 -Clean
```

`-VersionName` with a new value sets `versionName` and adds 1 to `versionCode`. Without it the current version is kept.

The script:

1. Checks `gradlew.bat`, `app\build.gradle.kts` and `app\google-services.json` exist.
2. Reads (and optionally bumps) the version.
3. Deletes old APKs, then runs `assembleRelease`.
4. Opens each APK and checks `classes.dex`, the manifest and the native ABI folders.
5. Verifies the signature with `apksigner` when the Android SDK is found.
6. Copies the APKs to `dist\` and writes `SHA256SUMS.txt`.

If the app has no native libraries, Gradle only makes the universal APK and the script says so instead of failing.

## 3. Publish on GitHub

1. `git tag v1.1.0 && git push origin v1.1.0`
2. On GitHub: Releases, Draft a new release, pick the tag.
3. Upload the three APKs and `SHA256SUMS.txt` from `dist\`.

Never commit the keystore, `keystore.properties` or `google-services.json`.

## Updating an installed app

Phones update in place only if the new APK has the same package name, the same signing key and a higher `versionCode`. The script raises `versionCode` for you when you pass a new `-VersionName`.