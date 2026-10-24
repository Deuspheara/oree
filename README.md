# Oree

A local, remote-first Android TV and Google TV launcher, built with Kotlin and Compose for TV.

![Oree Home with sample apps](docs/screenshots/home.png)

Browse apps, organize favorites and folders, and switch between local profiles. Oree combines a glass dock, animated focus, local search, wallpapers and controls designed for a D-pad. English and French are included.

- **Your layout:** favorites, folders, app organization and independent profile appearances.
- **Your visual workload:** six previewable steps, from static Simple to Live Liquid Glass. The final step uses live Haze refraction and retains full focus animation during rapid navigation; it costs more GPU time. Reduced motion and contrast preferences take precedence. Open **Settings → Appearance → Visual experience**.
- **Local by default:** no Internet permission, ads, telemetry or required account. Optional Android TV content providers supply their own available content; streaming apps own playback and their accounts.
- **Optional extras:** accessible local photos, ambient display, an Android screensaver and validated configuration export/import with undo. Image files are referenced rather than embedded in backups.

This is an early development project with no published release. Minimum API support is a build target; physical device coverage and performance acceptance remain limited. See [compatibility](docs/ANDROID_COMPATIBILITY.md) and [performance](docs/PERFORMANCE.md).

| Profiles | Visual experience |
| --- | --- |
| ![Profile switcher](docs/screenshots/profiles.png) | ![Live Liquid Glass option](docs/screenshots/live-glass.png) |

These are physical-TV captures of the actual UI with in-memory sample data and project-owned artwork, rather than a personal catalog. See [capture details](docs/screenshots/README.md).

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

## Public source preparation

See [publication notes](docs/PUBLICATION.md) for reviewing Git history, keeping local evidence private, and preparing a source archive.

## License

Original project work is licensed under [Apache-2.0](LICENSE). Dependency and asset notices are listed in [THIRD_PARTY.md](THIRD_PARTY.md). There is no published release yet.
