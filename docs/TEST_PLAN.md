# Testing

This page is for contributors. For choosing a slider level, read the [visual quality guide](PERFORMANCE.md).

## Required local checks

Use JDK 21 and the pinned Gradle wrapper:

```sh
./gradlew spotlessCheck verifyArchitecture test lintDebug \
  :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleBenchmarkRelease
./gradlew :app:assembleDebugAndroidTest :benchmark:assembleNonMinifiedRelease
python3 scripts/export-avatars.py --check
```

These check formatting, architecture, module/DI tests, Android lint, optimized compilation and test/benchmark compilation. Build success does not establish physical focus, accessibility or performance acceptance.

## Run device tests safely

Prefer a disposable TV emulator for isolated fixtures. On a configured physical TV, back up app data privately and use replacement installs. Connected-test tooling and baseline-profile collection can reinstall/uninstall the target, so do not use them on configured data. Keep default HOME, accessibility, display and power settings unchanged. See [Recovery](RECOVERY.md).

```sh
adb -s "$TV_DEVICE" install -r app/build/outputs/apk/debug/app-debug.apk
adb -s "$TV_DEVICE" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s "$TV_DEVICE" shell am instrument -w -r \
  fr.deuspheara.oree.test/fr.deuspheara.oree.LauncherTestRunner
```

Add `-e class fr.deuspheara.oree.ClassName` before the runner to run a focused class. Explicitly start only this app when checking the optimized build.

## What to cover

| Area | Main checks and existing fixtures |
| --- | --- |
| Writes and persistence | Inject schedulers with `runTest`. Cover one in-flight/latest-pending preference snapshot per profile, revision acknowledgements, retry, accepted writes after waiter cancellation, flush barriers, committed export reads and atomic restore/undo. Use `FeatureOperationStateTest`, `LauncherStateTest` and codec cases. |
| Source lifecycle | Test all source-toggle combinations, preview-only zero Watch Next reads, shared bounded snapshots, package filtering, cancellation, cached failure, foreground refresh and zero observers after unsubscribe. Use `ContentRepositoryTest` and source/ViewModel cases. |
| Search and async results | Preserve query/result identity, reject stale results and keep focus on surviving targets. Use `SearchViewModelTest`, `SearchRenderingTest`, `FolderSearchTest` and `LauncherLifecycleTest`. |
| Home navigation | Use native D-pad input for row edges, held/repeated keys, scrolling/reversal, dock/header return, removal and external return. Use Home focus/navigation/vertical-motion/reversal cases. Check Activity recreation and process death separately. |
| Artwork | Verify request sizing/prefetch, decode/palette completion, loading feedback, reflection source freshness and fixed input bounds. Use `HomeArtworkPrefetchTest`, `HomeArtworkLoadingTest`, `HomeCardResponseTest`, `ArtworkReflectionsTest` and `WallpaperBlurTest`. |
| Glass | Verify initial transparency, moving bounds, wallpaper replacement, resize/reattachment, native dialog coordinates and opaque/accessibility fallback. Use `WallpaperBackgroundTest`, `HazeGlassTest`, `GlassStageFidelityTest`, `RetainedMenuSurfaceTest` and `GlassOpticalMapTest`. |
| Slider and profile circle | Verify all six levels, final-step D-pad/accessibility range, explicit Apply, legacy defaults and backup validation. `HomeCardResponseTest` checks live-mode motion during repeats; `ProfileGlassTest` checks foreground sampling, no rectangular switcher backing and transparent focused fills. |
| Profiles, folders and controls | Keep selection distinct from focus, captured edit identities, retained drafts, modal containment and nested return. Use `ProfilePanelTest`, `ProfileMenuVisualTest`, `FolderManagementTest`, `FavoriteReorderTest`, `AppContextMenuTest`, `CommandPanelTest` and `TvInteractionStudyTest`. |
| Settings | Test explicit browsing/commit, Back without changing a choice, category/viewport return, fresh entry, reset confirmation and retained failure/retry. Use `SettingsScreenTest`, `SettingsVisualTest` and `LauncherLifecycleTest`. |
| Photos and backup | Cover denied/empty/removed photos, picker cancellation, bounded decoding, photo directions, malformed/oversized/revoked imports and unchanged state on rejection. Use photo/gallery/thumbnail and backup/codec cases. Real-photo tests need authorized fixtures; report missing prerequisites as skips. |

For split/precomputed glass comparisons, use the fixture's `splitWallpaper`, `opaqueWallpaper` and `precomputedOptics` arguments. Include original-Haze and translucent-source controls. Check trace path markers so fallback cannot silently replace the renderer being tested. Frozen-clock screenshots establish appearance/freshness, not frame pacing.

## Physical acceptance

Inspect the actual optimized APK with English/French, RTL, large text, high contrast, reduced motion/transparency and the intended remote. Verify focus/press/error states, observer shutdown below STARTED and refresh on return. Do not grant photo/provider permissions by shell as a substitute for testing denial.

Record source commit, APK SHA256, physical hardware or emulator, SDK, scenario, compilation/profile state, caches, outcomes and limits. Keep raw captures, traces, APKs, device identifiers and configured data outside tracked source. Use the [performance protocol](development/PERFORMANCE.md) for timing claims; controlled clocks and passing tests do not establish smoothness or broad compatibility.

## README screenshots

`PublicScreenshotsTest` renders production screens with original project artwork and in-memory sample apps/profiles, without changing configured stores. Review every image before exporting to `docs/screenshots/`; personal viewing history and custom wallpaper stay private. See [capture details](screenshots/README.md).

Detailed past run logs remain in Git history and private evidence. Record new results with the exact changed source rather than copying old pass counts into current acceptance claims.
