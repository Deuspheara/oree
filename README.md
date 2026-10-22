# Oree

A local Android TV launcher built with Kotlin and Compose for TV. This is an early development project; features and UI are still evolving.

Oree includes an installed-app catalog, favorites, folders, local search, profiles, wallpapers, settings and remote-friendly controls. Optional features include local photos, public TV content providers, ambient display, an Android screensaver and configuration backup/restore. Streaming apps continue to own playback and their accounts.

The app has no Internet permission, advertising, telemetry or required account. UI strings are available in English and French.

## Build

Use JDK 21 and Android SDK 37. Configure `ANDROID_HOME` or an untracked `local.properties` containing `sdk.dir`.

```sh
sdkmanager "platforms;android-37" "build-tools;36.0.0"
./gradlew :app:assembleDebug
```

The Gradle wrapper and dependency catalog pin the toolchain. Minimum Android API is 26; target API is 36. APKs are written under `app/build/outputs/apk/`.

## Run

Install and explicitly start the application without changing the current default launcher:

```sh
adb -s "$TV_DEVICE" install -r app/build/outputs/apk/debug/app-debug.apk
adb -s "$TV_DEVICE" shell am start -n fr.deuspheara.oree/.MainActivity
```

Keep another launcher installed and read [Recovery](docs/RECOVERY.md) before device testing. The application ID is `fr.deuspheara.oree`. Profiles and settings belong to this app's private sandbox; configuration exports can be reviewed and imported locally. Backup files reference wallpaper images rather than embedding them.

## Project layout

| Directory | Purpose |
|---|---|
| `app/` | Android entry points, dependency assembly and navigation |
| `core/` | Models, domain contracts, persistence, Android adapters and design system |
| `feature/` | Home, settings, profiles and search presentation |
| `benchmark/` | Optimized benchmarks and baseline-profile journeys |
| `build-logic/` | Shared Gradle conventions |
| `assets/` | Original avatar masters and export metadata |
| `scripts/` | Asset, profiling and dependency-inventory tools |
| `docs/` | Current product and engineering contracts |

See [Architecture](docs/ARCHITECTURE.md) for module boundaries and [Contributing](CONTRIBUTING.md) for development rules.

## Verify

```sh
./gradlew spotlessCheck verifyArchitecture test lintDebug \
  :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleBenchmarkRelease
./gradlew :app:assembleDebugAndroidTest :benchmark:assembleNonMinifiedRelease
python3 scripts/export-avatars.py --check
```

Formatting uses Spotless/ktlint. Android Lint and module-boundary checks are mandatory. Device tests, accessibility checks and physical performance measurements are separate requirements; build success does not establish release readiness. See [Testing](docs/TEST_PLAN.md) and [Performance](docs/PERFORMANCE.md).

## License

Original project work is licensed under [Apache-2.0](LICENSE). Dependency and asset notices are listed in [THIRD_PARTY.md](THIRD_PARTY.md). There is no published release yet.
