# Testing

## Build gates

Use the pinned wrapper and JDK 21:

```sh
./gradlew spotlessCheck verifyArchitecture test lintDebug \
  :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleBenchmarkRelease
./gradlew :app:assembleDebugAndroidTest :benchmark:assembleNonMinifiedRelease
python3 scripts/export-avatars.py --check
```

These cover formatting, module direction, portable boundaries, module unit tests, Android Lint, optimized compilation and instrumentation/benchmark compilation. Koin graph verification and test-dependent construction of every ViewModel are unit gates. Compilation does not replace device acceptance.

## Unit and persistence coverage

Use `runTest` and injected schedulers/dispatchers for cancellation, rapid preference edits, profile switching, revision acknowledgements, retry, deletion, flush barriers and restore races. Assert bounded pending work rather than timing assumptions.

Cover every content-source toggle combination, preview-only zero Watch Next reads, shared concurrent consumers, filtering before limits, cached failure, explicit refresh, foreground refresh and zero observers after the last subscriber. Validate schema/legacy fixtures, malformed/future input refusal, missing-package order and atomic restore/undo.

A delayed shared observer must not make export read stale preferences after a successful flush. Successful committed-read retry must recover a failed observation without a stop/start cycle. Accepted wallpaper import and its progress must survive route recreation and retain the captured profile ID.

## Direct Android instrumentation

Use a disposable TV emulator where possible. On a configured TV, preserve a private data backup and replace-install only this package. Connected-test tooling may uninstall targets; do not use it on configured user data.

```sh
adb -s "$TV_DEVICE" install -r app/build/outputs/apk/debug/app-debug.apk
adb -s "$TV_DEVICE" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s "$TV_DEVICE" shell am instrument -w -r \
  fr.deuspheara.oree.test/fr.deuspheara.oree.LauncherTestRunner
```

Use `-e class` for focused cases. Verify native D-pad navigation, row edges, held/repeated Select suppression, modal containment, nested return, folder/favorite reorder and Favorites/catalog relocation without losing focus. Cover removal, empty/error/retry and external return. Test Activity recreation and actual background process death separately; force-stop/relaunch only establishes persistence.

Test photo/provider permission denial without shell grants. Real-photo cases require intentionally authorized fixture photos and must report a missing prerequisite as a skip. Keep stores isolated. Test oversized/malformed/revoked imports and unchanged original state after rejection.

## Visual, lifecycle and performance acceptance

Inspect actual optimized rendering, focus/press/loading/error states, EN/FR, RTL, large text, contrast, reduced transparency/motion and relevant input devices. Confirm observers stop below STARTED and refresh on return. Record source/APK, hardware, SDK, build, scenario, caches, failures and limits. Label emulator evidence explicitly.

Follow [Performance](PERFORMANCE.md) for optimized comparisons and [Recovery](RECOVERY.md) for safety. BaselineProfileRule on API 33 may reinstall its target during compilation reset, even with direct instrumentation: collect profiles only on disposable fixtures. Keep raw logs, APKs, traces and captures outside tracked source. Release acceptance requires evidence against the actual changed behavior and physical budgets.
