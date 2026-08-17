# MIMI Money for Android

Native Android client for MIMI Money, including messaging, calls, wallet, dApp, notifications, Firebase integrations, Google Drive and cloud storage.

## Features

- Real-time messaging with end-to-end encryption
- High-quality voice and video calls
- Secure digital wallet for cryptocurrencies
- Decentralized application (dApp) integration
- Push notifications for important updates
- Firebase integration for analytics and crash reporting
- Google Drive and cloud storage for file backup and synchronization
- AI agents - AI-powered personal assistants for managing finances, scheduling, and more
- AI agents Marketplace


## Requirements

- Android Studio with Android SDK 36.1 and Build Tools 36.0.0
- JDK 17 (the project compiles Java/Kotlin source to JVM 11 bytecode)
- Android NDK `21.4.7075529`
- Git

Android Studio can install missing SDK and NDK packages from **Tools > SDK Manager**. The Gradle wrapper downloads Gradle 8.13 automatically.

## Clone and configure

Create `local.properties` in the project root, or let Android Studio create it when opening the project:

```properties
sdk.dir=/absolute/path/to/your/Android/sdk
```

Add private runtime configuration to your user-level `~/.gradle/gradle.properties` (recommended) or the ignored project-level `local.properties`:

```properties
BACKEND_SOCKET_TOKEN=replace_with_the_server_socket_token
WALLETCONNECT_PROJECT_ID=replace_with_your_reown_project_id
# Optional; the Google test ID is used when omitted.
ADMOB_APP_ID=ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy
```

`app/google-services.json` contains the Firebase Android client configuration required by the Google Services Gradle plugin. If using another Firebase project, register application ID `com.money.mimi.app` in Firebase and replace this file with the downloaded configuration.

Never commit signing keystores, signing passwords, wallet recovery phrases, private keys, access tokens, or a populated app database. The included `.gitignore` excludes those files.

## Build and test locally

macOS/Linux:

```bash
./gradlew clean assembleDebug
./gradlew testDebugUnitTest
```

Windows PowerShell:

```powershell
.\gradlew.bat clean assembleDebug
.\gradlew.bat testDebugUnitTest
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. Install it on a connected device or emulator with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

An empty `BACKEND_SOCKET_TOKEN` permits compilation but authenticated socket features require the matching server-side token. WalletConnect features similarly require a valid Reown project ID.

## Release signing

Release credentials are intentionally excluded. For an unsigned bundle:

```bash
./gradlew bundleRelease -PunsignedReleaseBundle=true
```

For a signed release, place the upload keystore at `keystore/mimimoneyupload-keystore.jks` and create ignored file `keystore/.env`:

```properties
storePassword=replace_me
keyPassword=replace_me
```

Then run `./gradlew bundleRelease`. Keep the keystore and passwords in a secure secret manager; do not upload them to GitHub.

## Repository hygiene

Generated Gradle/Android Studio files, build outputs, signing material, local SDK paths, databases, logs, and OS metadata are excluded. Realm schemas and migration source are included, but no populated database or user data is present in this repository.
