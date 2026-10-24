# Testing

## Transparency regression verification — 24 October 2026

The user subsequently authorized repair and physical millisecond comparisons. The experimental
direct wallpaper snapshots were removed after opaque dock/header output in transparent mode.
Production now retains the live mapped/Haze input path; the obsolete direct-path comparison
argument and capture helpers are removed. The initial source-only handoff is historical.

Verify actual transparent dock/header rendering after initial loading, first scroll/reversal,
wallpaper replacement, source resizing/reattachment, separate-window glass and opaque override.
Use WallpaperBackgroundTest, HazeGlassTest and the opaque split/precomputed variant of
GlassStageFidelityTest. Hardware-window captures establish sampled appearance only.

ArtworkReflectionsTest waits for optional palette completion before changing its fixture.
WallpaperBlurTest adds a seeded comparison with the original neighborhood-sum algorithm across
narrow/small images and all radii. Use matched optimized A1/B1/B2/A2 vertical tours and trace
quality, scene, input and focus checks for timing; preserve and compare configured files and
restore the exact corrected APK. Record actual outcomes separately from these requirements.

## Inward glass sampling bounds — 24 October 2026

`GlassOpticalMapTest.inwardBilinearSamplesStayInsideUnpaddedMaterials` checks every pixel
centre of representative dock, header, minimum-eligible, narrow and low-density materials,
including nine-patch stitching and bilinear numeric-channel sampling at maximum displacement.
Tiny materials keep their original padding. Preserve this invariant if changing the map field.

The accepted smaller-bounds build passes all seven physical `WallpaperBackgroundTest`,
`HazeGlassTest`, `ArtworkReflectionsTest` and opaque/split `GlassStageFidelityTest` cases.
Twenty comparable captured movement/source/resize frames differ from the preceding control
by at most one channel level; the initial frozen-clock capture is not pixel-comparable because
asynchronous optical-map readiness differs. Both initial appearances retain a visible source.
All twelve final optimized ABBA timing traces pass quality, delivered-input, native-focus,
visible-scene and mapped-path/no-fallback checks. See Performance for complete blocks,
rejected experiments and the slower initial control iteration; compilation is not acceptance.

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

## Presentation refactor regression coverage

`FeatureOperationStateTest` checks accepted-write serialization after waiter cancellation,
retained write failure/retry, duplicate profile commands, restore flush barriers and blocked edits,
committed-snapshot backup review with a delayed observation, and cancellation of obsolete catalog
reads while preserving cached content. `LauncherStateTest` covers retained wallpaper import,
profile-write failure after route cancellation and bounded initialization retries.

`SearchViewModelTest` checks subscription ownership, latest-request ranking and identifiable
replay. `RegisteredFocusTest` checks late requester registration, the one-second missing-target
bound and cancellation using virtual time. These tests do not establish native focus correctness
or remote-to-photon latency. Keep existing D-pad/modal/recreation instrumentation for that work.

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

## tvOS 26 interaction study regressions

`SettingsScreenTest` covers explicit choice browsing/commit, Back without changing a choice,
category-before-exit navigation, reset confirmation, retained save failure and recreation.
`TvInteractionStudyTest` adds stable option/scroll return across categories, uninstall submenu
return, cancel-first folder removal, async Search focus ownership and photo-direction/Back
contracts. Its photo collection uses project-owned resource fixtures, not MediaStore permission
or real-photo evidence. `FolderSearchTest`, `AppContextMenuTest`, `FavoriteReorderTest`,
`AppPreviewsTest`, `ProfilePanelTest`, `CommandPanelTest`, `PhotoGalleryTest` and
`ExtensionPanelsTest` retain the surrounding native input and lifecycle regressions.
`ContentRepositoryTest` verifies optional Watch Next metadata and fallback when a provider
rejects its optional projection. Real caller probes remain separate from cursor fixtures.

Use direct replacement installs and the runner above; do not reset configured device stores.
Record exact passing/failing classes and source/APK identities privately, inspect window
captures, and keep release/performance acceptance separate from these focused tests.

`LauncherLifecycleTest` additionally opens two different Settings destinations from Search
within one Activity, including diagnostics recreation, so a previous visit cannot replace the
new entry category. An expected submenu return must target its own action row rather than Open.

LocalPhotoThumbnailTest uses a synthetic portrait through an in-process provider to verify
that thumbnail reads avoid the original, checks resource fallback/fullscreen cache isolation,
and rejects arbitrary file schemes. It does not grant photo permissions or read personal media.
Check Search → Wallpaper → Gallery → Android picker → cancel on device, including saved
Search-result return and absence of parent Search cards behind the gallery.

## Home artwork prefetch regression

`HomeGridStructureTest` covers adaptive column thresholds, remainder pixels and the existing
480×270 request cap. `HomeArtworkPrefetchTest` compares the prepared request bucket with
actual Foundation grid measurements across column changes, and composes an unmeasured card
with a synthetic failing image loader to verify that its bounded request starts before layout.
It also verifies that palette-sampled artwork requests CPU-readable decoding. The fixture
restores the application image loader afterward. A resting Home fixture with a content-preview row verifies that nearby
catalog artwork starts loading while dock focus remains selected and the catalog card is not
displayed. Keep `HomeArtworkLoadingTest`, `HomeCardResponseTest` and `HomeFocusTest` for loading, reduced motion, input bounds and focus
restoration. These checks establish behavior, not frame-time improvement; compare optimized
builds on physical hardware for the first catalog reveal and newly appearing rows.

For Home's bounded recent-row retention, compare the settled-header first catalog reveal and
rapid return through the dock separately. Match actual device-side events, focus walks and
visible scenes in alternating optimized builds. Run the vertical motion/reversal, header
restoration and artwork-prefetch native cases, then check repeated-tour PSS, HWUI cache and
background/resume outside timed runs. Larger windows are experiments rather than assumed
improvements; retain the existing visual, request, quality and motion policies. A screen
recording can diagnose trajectory, but encoding perturbs rendering and cannot certify budgets.

## Search rendering regression

`SearchRenderingTest` verifies that a retained glass backdrop keeps query editing, result
replacement, selection and native focus live across backdrop identity changes. A synthetic
image loader verifies that nearby offscreen results prefetch without moving query focus or
requesting the entire result set. `RetainedMenuSurfaceTest` retains source-color, position and
opaque-fallback coverage. Keep `FolderSearchTest`, `TvInteractionStudyTest` and
`LauncherLifecycleTest` for grouped result focus, removal, Close, IME/Back, nested return and
recreation. The runtime backdrop identity includes both wallpaper frame and scene softening;
changing the material must not reset the saved query or grid position.

## Folder management follow-up

`FolderManagementTest` adds first-row Up/title navigation, surviving-header focus under
package changes, held folder-opening release suppression, create-name focus and nested Back
(including IME dismissal), explicit transfers from other folders, Add apps trigger restoration
and the all-added Close fallback. See [Folder management](FOLDER_MANAGEMENT.md) for contracts.

The final focused physical API 34 run passes 26/26 cases across FolderManagementTest,
FolderSearchTest, TvInteractionStudyTest, FolderVisualTest, FavoriteReorderTest and
AppContextMenuTest. Earlier intermediate runs found native focus races and an IME-aware
Back expectation; those were corrected. One attempt could not start because the CLI inspection
helper already owned UiAutomation; ending that helper allowed the direct runner to proceed.
Do not sum overlapping runs. The final appearance fixture asserts actual English/French labels
inside the dialog and produces project-only captures of dark/light and 200% text with contrast,
reduced transparency and reduced motion. These captures were visually inspected. Configured
profile datastore bytes match the private archive taken before instrumentation. Initial archive
access was refused by the original non-debuggable APK; initial byte-for-byte preservation is
not established. No system HOME/accessibility/display/power setting was changed.

After the measured retained-glass change and native nested-window focus correction, the same
26-case suite passes again against the final source. The strengthened transfer case changes
the backdrop key while retaining focused row actions. Optimized native checks separately
confirm held Select, the compact French count badge, Add apps entry and Back to the exact
Add apps header control. Opening/closing and list navigation have separate performance
samples; neither functional passes nor a warm list sample establish general smoothness.

### Home, continuation artwork and Control Center follow-up

At the user's request, verification was limited to formatting, optimized compilation, physical
screens/layout inspection and short diagnostic frame samples. The full build gates/test suite
above were not rerun. Screens confirmed the dock's complete outline on focus return, compact
Control Center, sound controls and unchanged default HOME after replacement installs. No Wi-Fi
or volume adjustment was executed. Native-window/IME dismissal was separated from Search Close.
Read-only diagnostics confirmed cross-app TV entries and zero local-art entries in the visible
sample; HTTPS artwork for Twitch/CANAL+/Prime Video has not been confirmed end-to-end. Notification
access was not added. See PERFORMANCE.md for remaining budget failures and experiment limits.

## Overview, Peek and motion follow-up

`HomeNavigationTest` covers section jumps and section-specific position memory, native Back
return to Overview, explicit Peek entry, member launch/return, empty-folder full browsing, unchanged ordinary Down navigation,
opening held-release suppression and surviving header focus across membership changes.
TV Material3 buttons must be activated with D-pad Select; a touch click is not an equivalent
precondition in these keyboard-mode fixtures. Wait for native window focus when crossing
modal boundaries. `HomeCardResponseTest` adds controlled-clock entry/release continuity;
this is not a physical latency measurement. Keep `HomeFocusTest`, `SettingsScreenTest` and
`ProfilePanelTest` for surrounding input, recreation, settings acceptance and identity checks.
The dock regression uses the current six-favorite cap; the glass regression exercises the
current draft slider before Select commits. Avatar badges are descendants of merged profile
buttons and their tag assertions use the unmerged tree.

On optimized physical builds, inspect continuation spacing during rapid horizontal movement,
vertical scrolling, Overview/Peek/Back and the profile side background. Compare Settings entry
using a destination-checked Control Center → Settings scenario with unchanged preferences,
warm artwork, the same device and compilation treatment. Keep raw frame dumps, screenshots,
configured-data archives and intermediate failures outside tracked source.

Lazy Settings tests locate offscreen controls through `performScrollToNode` on the section
list before querying the leaf. Category restoration deliberately targets Defaults below the
initial viewport, then restores Theme for chooser checks. SettingsVisualTest covers the
same settled layout, preview, light/dark, large opaque text and English labels. The fixed
source dark-top captures before and after lazy composition have identical PNG hashes.
`LauncherLifecycleTest` additionally opens/closes Settings three times, verifies covered Home
semantics disappear, and waits for the original header to regain native focus. A visible ready
marker alone is insufficient evidence that the next frame's focus restoration has completed.
The repeated Settings-return integration uses shell-injected D-pad Up/Select/Back rather than
requesting the Home header through semantics; it checks the actual remote navigation path.
The production header omits Overview and opens Settings directly from the gear button. The
integration checks that Overview is absent and the command pill is not opened on Settings entry.
Inspect the profile picker over the unblurred Home background; the editor retains its own surface.
HomeNavigationTest explicitly covers a continuation row followed by header focus, covered
Home, reveal and provider replacement. Provider refresh/restoration must not take focus from
the active header. Reveal the lazy hero item before requesting its continuation requester.

## Visual setup and navigation workload regressions

VisualExperienceTest covers draft-only browsing, recreation, explicit completion, pending-write
disablement and retry, plus Keep current. ProfileExperienceCodecTest covers optional-field
round trips, legacy completed/full-motion defaults, profile identity and malformed imports.
HomeNavigationTest checks the full viewport and first-card/heading alignment. SettingsScreenTest
checks one-column category absence, Select entry and Back restoration. Compare optimized
physical-TV navigation using unchanged preferences, cache conditions and normal D-pad input;
controlled-clock Compose tests establish interaction contracts, not smoothness or latency.

## Live dock compositing and full-page investigation

`GlassStageFidelityTest` records native-window synthetic clear/soft/progressive material through
movement, reversals, a press-like transform, translucent source replacement, resizing and
reattachment. Pass `-e glassNativeLayer true` to exercise the bounded native layer around clear
glass. Its stationary-frost brightness checks, including the wide source-aligned path, reject a stale or empty surface. The controlled
clock, screenshot settling and captured pixels establish appearance/freshness cases, not physical
input latency. Review moving frames and optimized Home separately, including Overview scaling,
card press/tilt, source/profile changes and Settings return.

`LauncherNavigationBaseline.fullPageFirst` and `fullPageWarm` use six Down/six Up presses per
full-page tour at an absolute 500 ms device cadence. First traversal restarts the process and
sets the real hero/header anchor; warm traversal primes one complete tour. A privately supplied
`fullPageHeroTag` selects an existing stable item without injecting or modifying provider data.
`continuationStablePair` prepares that item and its real next neighbor together in view, then
uses the existing 150 ms horizontal cadence. Exported event identities, focus destinations,
visible content/bounds and trace quality must agree before comparing builds. Provider row-ID
churn is not permission to silently substitute a different focus destination. Keep memory,
first-display and native interaction diagnostics outside the timed bursts; see PERFORMANCE.md
for the measured scope, rejected variants and unresolved targets.


## Continuous vertical return

`HomeFocusTest.heldUpReturnStaysAnimatedAndDownInterruptsIt` injects a native repeated Up
with normal motion, checks an intermediate dock position, then reverses Down before completion.
Keep `onlyUpFromDockAnimatesItsReturnToBottom`, held-Up traversal, reduced-motion endpoint
and original-column tests alongside it. Inspect the optimized app with rapid vertical input on
the physical remote; these animation-clock contracts do not establish frame-rate smoothness.

`HomeNavigationTest.reversingHeaderDownDoesNotFinishAnObsoleteContinuationRequest` starts
with the large continuation item completely outside the viewport, begins its reveal from the
fixed header, then reverses Up before that deferred focus request completes. It must retain
header focus rather than complete the obsolete content request. Keep the offscreen precondition
explicit: a partially visible lazy item can skip the asynchronous reveal and miss this race.

`HomeFocusTest.economyKeepsVerticalPageReturnAnimated` matches the Economy effects setting
with normal requested/system motion and requires an intermediate page position before return
settles. Run alongside the explicit reduced-motion endpoint and rapid reversal cases. Prior
normal-quality fixtures did not cover Economy's separate animation bypass.

`HomePageReturnMotionTest` checks the upward handoff's carried starting speed, zero final
speed, monotone/bounded fast returns, and stale/opposite/reset fallbacks. It is a deterministic
motion-policy test, not native frame pacing. Keep the existing Economy, held-Up interruption,
fully offscreen reversal, endpoint and Settings-restoration cases on the physical TV. In an
optimized rapid-vertical comparison, check `TVL:pageReturnHandoff` before attributing results
to the new branch; match actual keys/focus destinations and scenes, and report changes to the
intended intermediate trajectory separately from rendering cost. Inspect the physical remote
result without a recorder or benchmark active.

## Separate wallpaper filtering

Run `GlassStageFidelityTest` with `-e splitWallpaper true -e opaqueWallpaper true` to exercise
prepared sharp/frost inputs, and with `-e splitWallpaper false -e opaqueWallpaper true` for
the original renderer. The controlled-clock fixture asserts fallback while the wallpaper
crossfade runs. Compare its 21 native-window frames through scroll-like movement, reversal,
press/tilt, wallpaper replacement, resize and reattachment. `-e opaqueWallpaper false` with
prepared inputs enabled verifies the translucent-source fallback. These synthetic captures
establish sampled appearance and freshness, not input latency or frame pacing.

`WallpaperBackgroundTest.preparedFrostIsDiscardedWithItsWallpaperAndReattachmentUsesFreshPixels`
checks source withdrawal on disposal and fresh filtered pixels after reattachment. Keep the
existing reduced-motion, navigation/reversal, Settings-return and lifecycle cases alongside it.
Compare the exact final optimized APK on the physical TV for first-entry and rapid vertical
scrolling, validate actual events/focus/scenes and trace quality, and keep resource queries
outside timed bursts. See PERFORMANCE.md for the tested source, measured scope and limits.

For precomputed optics, pass `-e precomputedOptics true` with the prepared opaque fixture,
and `-e precomputedOptics false` for the Haze control. Keep the alpha-source variant and
`WallpaperBackgroundTest`/`HazeGlassTest`, plus the real Settings-return lifecycle case.
Check `Oree:mapped-glass-optics` and absence of `Oree:mapped-glass-fallback` in a bounded
functional trace so fallback cannot silently substitute for the renderer under test. These
frozen-clock captures verify sampled source freshness and appearance, not frame pacing.
`GlassOpticalMapTest` covers inward/symmetric displacement, neutral-center compression,
allocation guards and row-boundary cancellation on the host JVM without an emulator.

The repaired source's required local gates and seven targeted physical cases passed on
24 October 2026. The actual optimized dock/header transparency was inspected, and twelve
rapid vertical traces were collected. A provider identity refresh splits the run into two
scene groups, so the result is a small primary frame-duration difference with no demonstrated
overall speedup; see the transparent-glass repair section in PERFORMANCE.md. This does not
close broader performance, startup, memory or compatibility acceptance.


### Visible-wallpaper fusion follow-up (2026-10-24)

Three isolated wallpaper-paint/cache variants pass eight focused physical rendering cases
and required local gates but do not establish a repeatable timing win. The conditional cached
screen fails overrun nonregression; branchless cached confirmation is invalidated by provider
identity changes between blocks; direct drawing fails the matched return presentation-p95
comparison and worsens typical completion/deadline misses. All experimental production and
fixture changes are reverted. The preceding accepted smaller-bounds source and exact APK
are restored and revalidated locally. Final physical Home transparency, APK/source identity,
configured wallpaper/preferences and default HOME preservation are checked separately.
See Performance for every block and the excluded disconnected instrumentation attempt.

## Live glass and public documentation captures

`HomeCardResponseTest.liveGlassKeepsLiftDuringRepeatsButReducedMotionStillWins` checks that live mode retains lift under repeated navigation and that reduced motion wins. `ProfileExperienceCodecTest` covers profile/backup round trips, the legacy default and malformed imported live-glass values. `ProfileMenuVisualTest` includes live optics alongside the existing accessibility/palette matrix.

Run `PublicScreenshotsTest` directly on a physical TV for project-only Home, profile and quality-rail documentation scenes. It uses in-memory sample profiles/apps and original bundled wallpaper without writing configured stores. Curated documentation exports may be tracked under `docs/screenshots/`; personal captures, APKs, device identifiers and raw verification evidence remain private. Screenshots establish appearance only, not timing.

Verification on 24 October 2026: required Gradle gates and avatar export validation pass. The final physical API 34 suite passes 20 tests across PublicScreenshots, HomeCardResponse, ProfileExperienceCodec, ProfileMenuVisual, ProfilePanel and VisualExperience. Configured DataStore files compare unchanged before/after the suite; default HOME remains unchanged. Documentation screenshots are reviewed sample scenes. These runs establish the covered functional and appearance behavior, without new frame-time or broad compatibility claims.
