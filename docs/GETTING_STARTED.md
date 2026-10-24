# Build and install Oree

Oree is a development project. There is no ready-to-download release yet; this guide is for building the source yourself.

## What you need

- An Android TV or Google TV device. The minimum configured version is Android 8.0 / API 26; glass effects require Android 13 / API 33 or later. See [compatibility](ANDROID_COMPATIBILITY.md).
- A computer with JDK 21, the Android SDK and ADB. ADB is Android's command-line tool for connecting to a device.
- A copy of this repository and an authorized ADB connection to your TV.

Keep the TV's current launcher installed. These steps open Oree as an app and leave default HOME unchanged. See [recovery instructions](RECOVERY.md).

## Build the APK

In a terminal, open the repository folder. Install the SDK components:

```sh
sdkmanager "platforms;android-37" "build-tools;36.0.0"
```

Set `ANDROID_HOME` to your SDK folder, or put `sdk.dir=YOUR_SDK_FOLDER` in an untracked `local.properties` file. Use JDK 21, then run:

```sh
./gradlew :app:assembleDebug
```

The installable file is `app/build/outputs/apk/debug/app-debug.apk`. This is a debug build for development; it should not be used to judge final performance.

## Install and open

List connected devices:

```sh
adb devices
```

Replace `YOUR_DEVICE` below with the device address or serial shown by ADB. The TV may ask you to approve the connection.

```sh
adb -s YOUR_DEVICE install -r app/build/outputs/apk/debug/app-debug.apk
adb -s YOUR_DEVICE shell am start -n fr.deuspheara.oree/.MainActivity
```

`install -r` updates an existing compatible installation while keeping its data. If Android reports a signing mismatch, stop and check the installed build rather than uninstalling a configured copy.

## First steps

1. Keep the current appearance or preview a [visual experience](PERFORMANCE.md), then finish setup.
2. Use the arrow keys to browse and Select to open an app.
3. Hold Select on an app for its available actions, such as favorite or folder organization.
4. Open Settings to change appearance and optional content sources. Use Back to return one level.
5. Open the avatar control to switch local profiles or add one.

The system HOME key still opens your current default launcher. Oree's app ID is `fr.deuspheara.oree`.

If you are changing the code, continue with [Contributing](../CONTRIBUTING.md) and [Testing](TEST_PLAN.md).
