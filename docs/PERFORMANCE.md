# Performance

## Budgets

| Metric | Target |
|---|---|
| Warm navigation | Less than 1% missed deadlines at display cadence |
| First visual input response | p95 below 50 ms |
| Warm Home usable | Below 250 ms |
| Cold first display | Below 1 second |
| Static idle PSS | Below 100 MiB |
| Enriched Home without player | Below 150 MiB PSS |
| Idle CPU | Close to zero |

These are acceptance targets, not a certification of this development build. Physical warm-navigation acceptance remains unresolved. Do not relax a target or claim smoothness from compilation, generated profile counts or a narrow emulator sample.

## Measurement protocol

Use optimized non-debuggable `benchmarkRelease` with R8/resource shrinking. Record source identity, APK SHA256, hardware/SDK, display cadence, catalog size, exact journey, compilation/profile state, cache state and limitations. Keep HOME, accessibility, display, radio and power settings unchanged; explicitly start this app. Label emulator results separately.

Compare matched conditions in alternating order and retain every accepted/rejected sample. For the reference warm navigation protocol, use 30 priming keys and 30 measured keys with device-side synchronous dispatch and 150 ms pauses, the same PID and a two-second tail. Record actual cadence, focus destinations, trace loss and frame/presentation joins. Frame CPU duration, deadline misses, frame overrun and physical input latency are distinct metrics.

Measure startup, the fixed 60-key journey, settings writes, source activity, external return, image loading, a settled 60-second idle interval and 10/30-minute memory trends for their respective claims. `am start -W` is Activity timing, not Home usability. The host-driven `measure-tv.py` legacy tour differs from the reference warm protocol and must be labeled accordingly.

## Implementation constraints

Preserve the shared 12 MiB Coil memory cache, no duplicate disk cache for installed artwork, request/decode caps, artwork preparation, glass quality, fixed input bounds and reduced motion. No largeHeap, explicit garbage collection, wake locks or permanent idle animation.

Keep package/provider work subscription-owned with zero stop timeout. Share bounded enabled-source snapshots and conflate invalidations. Keep focus local; compute catalog organization only when its inputs change. Coalesce superseded pending preference snapshots without cancelling accepted transactions. Inject IO/Default contexts for blocking/CPU work. DI and modularization have no assumed speed benefit.

## Tools and profiles

`benchmark/` owns shared optimized journeys and strict profile generation. Regenerate profiles after critical paths stabilize, verify their APK/source identities and installed compilation, then measure benefit separately. BaselineProfileRule can reinstall its target on API 33; use disposable fixtures.

See [scripts](../scripts/README.md) for profile merge/presence, trace, measurement and runtime-inventory tools. Keep generated reports and private evidence outside tracked source. Compose compiler reports are emitted per Compose module; inspect them before changing stability annotations.


## 2026-10-22 interaction-study diagnostic

Final optimized `benchmarkRelease`, R8/resource shrinking, physical Smart TV Pro/API 34,
60 Hz display. Production source `a077c6be8726243ac1efd366b2f5ed09c6e6afe1a7259b439b597b18f3937af1`;
APK `e0f259a69ba1f0c99ba6354fc995c0db31af3ed7e780529d2ab9ffbbcfe0bf8a`.
The installed compilation state is recorded privately; no explicit compilation reset or
profile collection was performed. Process-cold force-stop retained disk/system caches;
artwork was mixed/warming during the tour. This is the existing host-driven legacy tour
(60 keys, 120 ms host pauses, 17.262 s wall time), not the reference warm-navigation protocol.

`gfxinfo` reports 349/673 janky frames (51.86%), frame histogram p95 89 ms. No matched baseline
was collected, so this establishes neither improvement nor regression, physical input latency,
or reference-budget acceptance. The global frame-budget work remains open (TVL-29).

After a five-second settle, one 60.213-second idle interval added five process ticks at 100 Hz
(0.0830% of one core) and one rendered frame; final PSS was 69,852 KiB. Endpoint checks found
the app foreground and TV awake. Diagnostic overhead and endpoint-only visibility checks
limit the result; this does not prove uninterrupted visibility, leak freedom or long-term
idle behavior. Raw protocol, package/compilation, memory and frame outputs remain private.
All existing budgets, cache/decode limits, graphics quality and reduced-motion paths remain.

## Home card appearance work — October 22 follow-up

Home now prepares bounded artwork request sizes from the dock width and Adaptive grid
geometry before individual cards are measured. This lets lazy precomposition start image
loading without the previous zero-size → measurement → recomposition gate. Remainder pixels
are included before applying the existing focus-zoom bucket and 480×270 cap. Folder cards
retain measured sizing where their container does not supply prepared geometry.

Artwork loading phase and palette callbacks have their own composable restart scope, leaving
card input/focus state outside those invalidations. The Compose compiler reports both scopes
as restartable/skippable; no stability annotations were added. Shared cache size, decode
quality, effects, native input bounds and reduced-motion behavior retain their existing values.

A physical API 34 optimized trace of the loading-only change identified 18 reflection hardware
readbacks: 318.36 ms inclusive in total, with a 56.94 ms maximum. These overlapping trace
slices must not be added to frame times. Earlier sizing alone did not improve the overall
frame-time tail in the exploratory alternating comparison, so it is not a performance win
claim. Sampled Home artwork now explicitly requests CPU-readable decoding before its single
normal rendering upload, avoiding upload → readback → sample. Reflection colors, filtering,
request bounds and the shared cache budget remain unchanged. Other callers retain the existing
hardware-bitmap compatibility path.

The CPU-readable artwork build (`2c022d6d29b0b048d34ff6986a72ee6809e1653067f3adac900610a4791e252b`,
optimized APK `7f42692ee57a886bb6d27e3f6b9103846651bcae3412325723f5e1b952b3e37d`)
was checked on the same physical API 34/60 Hz TV. A second 12-second trace of four Down/four Up
inputs found 18 reflection preparations totaling 3.649 ms, maximum 0.381 ms, and zero reflection
hardware-copy/readback slices. The prior loading-only trace had 18 preparations totaling
334.61 ms, maximum 60.23 ms. This establishes removal of the sampled readback work, not a
corresponding reduction in input latency or overall frame time.

An exploratory ABBA comparison used three process-cold samples per block, full app compilation,
a three-second startup settle, four Down/four Up keys with 250 ms host pauses, and a two-second
tail. Memory artwork caches began cold; OS/driver caches were retained. Median per-sample gfxinfo
p95 frame duration was 136.58 ms before and 134.89 ms afterward, with substantial missed deadlines
in both. This does not demonstrate an overall tail-latency improvement or meet the existing
performance budgets. It is not the reference warm-navigation protocol. Source/APK identities,
individual samples, compilation state and private traces are retained outside tracked source.

The final Home path also uses Foundation's bounded `LazyLayoutCacheWindow`: 320 dp ahead and
zero additional space behind. Nearby rows can compose/measure and start their final-size
requests during idle prefetch, including the first catalog row beyond the preview row. This
is a fixed window, not eager composition of the catalog. Prefetched loading cards stay static;
focus requesters and lazy state remain owned by `HomeFocusState`. Retaining the Home header
glass showed no useful frame-tail improvement, so that experiment was discarded.

Final 320 dp build: production source
`f32b3486d64c9eec4f3b792317083351bb9a5dab8c698cd5d297853803ab327e`, optimized APK
`20bdadaa73d1876c4b00eec83d8c3d62d75004b4de9eb93aea69efc8c0402b87`.
All required build gates, 61 unit cases and 41 focused physical-TV cases pass. The preview-row
prefetch fixture confirms that catalog requests begin while dock focus remains selected and
the catalog card is offscreen. Actual optimized artwork/focus rendering was inspected.

In the final 12-second trace of the same eight-key diagnostic, reflection preparation had
12 calls totaling 2.675 ms, maximum 0.375 ms, with zero hardware readbacks. Nearby preparation
now also happens during the three-second startup settle, before tracing. The largest recorded
measure/layout slice was 52.063 ms versus 94.290 ms with the smaller 160 dp window, both at the
first catalog reveal after previews. This is a single exploratory trace comparison, not an
input-latency result or overall frame-budget acceptance. Rendering spikes remain unresolved.
Configured datastore bytes match the private post-authorization backup; default HOME is unchanged.

## Search result appearance and navigation — October 22 follow-up

Search already requested bounded 256×144 artwork during composition and did not request
reflection sampling. Its large menu glass was still evaluated live on every result redraw.
The panel now retains only the settled optical backdrop using the existing `RetainedMenuSurface`;
query, results and input stay live above it. Wallpaper/scene identity, geometry, density,
theme and transparency changes invalidate the retained surface. The existing capture-size cap
and live fallback remain. Rounded clipping and IME padding preserve the original panel bounds.
No blur quality, source pixels, image limits or shared 12 MiB Coil-cache budget changed.

Foundation's 240 dp ahead/zero-behind cache window prepares a bounded set of nearby result
rows. Stable result/group keys and request-tagged ranking retain their existing behavior.
Ranking remains on injected Default; only presentation work changed.

A physical API 34/60 Hz ABBA navigation diagnostic compared the previous optimized APK
`20bdadaa73d1876c4b00eec83d8c3d62d75004b4de9eb93aea69efc8c0402b87` with the retained Search
build (source `f781302257e609337092a125b352fa45710d9219cb16bf71fa23622898109046`, APK
`da6514ce309d6d257fee3341b1ab79234ebb7ec7f0fafd56b0f2a28a9af04168`). Each sample used full
app compilation, process-cold explicit start, the same empty-query Search, keyboard dismissal,
20 priming keys and 20 measured keys (ten Down/ten Up), 250 ms host pauses and a two-second
tail. The PID stayed unchanged during each sample. OS/driver caches remained; artwork warmed
during priming. Actual host/ADB intervals and real source data are retained privately.

| Sample | gfxinfo frame duration p95 | Render duration p95 | Missed deadlines / frames |
|---|---:|---:|---:|
| Before A1 | 487.54 ms | 247.93 ms | 81 / 81 |
| After B1 | 18.18 ms | 12.96 ms | 8 / 120 |
| After B2 | 18.64 ms | 13.07 ms | 9 / 120 |
| Before A2 | 487.09 ms | 248.12 ms | 81 / 81 |

These four exploratory samples support a substantial Search-navigation rendering improvement
on this TV. They do not establish physical input latency, cold opening/typing performance,
broad compatibility or the less-than-1% missed-deadline budget. This is not the reference
30-key warm-navigation protocol. Raw frames, inputs, compilation, source/APK manifests and
preservation checks remain outside tracked source.

The final build additionally keys the retained backdrop to the wallpaper's softening target,
so delayed Home return and nested panel transitions cannot leave stale optical pixels.
Final source: `63af2df96283c016c4d98543fb435c012493f17accbe5acea9600cccffc8f3e9`;
optimized APK: `7b61a69449736077bca813a0e53b733a86863bcdbde782e4f31c211b03375003`.
All required gates, 61 unit cases and 17 focused physical-TV cases pass. A final single sample
with the same priming/navigation protocol recorded 17.48 ms frame-duration p95, 12.96 ms render
p95 and 9/121 missed deadlines. It corroborates the navigation improvement while leaving the
existing frame budget open. Final optimized artwork, focus, glass and clipping were inspected.
The optimized build is installed; configured datastore bytes and default HOME are unchanged.

## Folder management — October 22 follow-up

The new Add apps list initially redrew its live glass surface on every focus update. A valid
physical Smart TV Pro/API 34, 60 Hz probe recorded a 200 ms gfxinfo p95 histogram bucket,
152/153 missed deadlines and 152 slow draw-command frames. No artwork is loaded by these
rows. Folder transfer/name forms now reuse the existing RetainedMenuSurface for settled
background pixels; foreground fields, rows and focus remain live. The wallpaper frame and
softening state invalidate the retained scene. Existing capture bounds, material quality,
opaque/live fallback and shared 12 MiB Coil cache remain unchanged.

An exploratory ABBA comparison used the exact archived live-glass APK and final retained
APK, full `speed` compilation without reset/profile collection, the same folder/list and
wallpaper, ten priming keys and thirty measured Down/Up keys with 150 ms device-side pauses.
Memory/list caches were warm; OS/driver caches were retained. Foreground and the actual
picker were verified. The two B repeats share a process. This is not the reference
thirty-priming-key protocol and does not measure physical remote-to-photon latency.

| Add apps sample | p95 histogram bucket | Missed deadlines / rendered frames |
|---|---:|---:|
| Live A1 | 200 ms | 152 / 153 |
| Retained B1 | 14 ms | 0 / 356 |
| Retained B2 | 14 ms | 0 / 354 |
| Live A2 | 200 ms | 151 / 153 |

These samples support a substantial improvement in this warmed two-row navigation scenario
on this TV. They do not establish cold opening, text-entry, long-list scrolling, broad hardware
compatibility or general frame-budget acceptance. An earlier attempt had left Oree after a
native return-focus race and recorded zero frames; it was rejected. Focus ownership was
corrected and optimized native Back to Add apps was separately verified. No zero-frame
sample is counted as a successful rendering measurement.

A separate exploratory folder open/close pair used three priming cycles, fifteen measured
cycles, 350 ms pauses after Select/Back and a two-second tail. The original APK recorded
47/560 missed deadlines (8.39%), p95 bucket 61 ms; the final APK recorded 54/577 (9.36%),
p95 bucket 65 ms. This pair establishes no opening improvement, does not resolve the
less-than-1% budget and is insufficient to distinguish a small regression from run variation.
The known opening cost remains open. A bounded follow-up trace contains composition and
native window/measure work, but its ring buffer overwrote 8,646,656 bytes and the device
reported 35 ftrace setup errors. It is not a complete recomposition count or acceptance trace;
no exhaustive causal or latency claim is drawn from it.

Live-glass comparison APK: `c8e1a505a02684537acf16de631f833befdf64e1f1eca43d60b630e9ac16c79c`.
Final production source: `d1c8f86587724a295b74a75283c90292a260b1ba361bc0a3293fb90e5ac04dae`.
Final optimized APK: `d88d92048f386c1341d8a4777784d81c9e91f91e40407633702457d0b9240735`.
The opening pair's original APK is the preceding Search follow-up's archived `7b61a694…` build.
Private manifests record source hashing, APKs, SDK/device, compilation, caches, scenarios,
all accepted/rejected samples, frames, captures and trace limitations. All required Gradle
gates, 61 distinct unit cases, avatar validation and the final 26-case physical suite pass.

After the comparison, configured profile datastore bytes still match the pre-instrumentation
archive. Default HOME resolves to the same component. The exact final optimized APK was
replacement-installed, compiled with `speed` and explicitly started; its installed bytes were
verified against the build artifact. No data clear, launcher removal, settings change, public
push or release was performed. Initial byte preservation remains limited by the original
non-debuggable APK's refused archive, as recorded in TEST_PLAN.md.

## Home scroll and Search follow-up — exploratory physical diagnostics

The configured Smart TV Pro/API 34 received replacement installs of optimized R8/resource-shrunk
benchmarkRelease. No HOME, display, power, radio or audio settings changed. These are short
host-driven diagnostics, not the reference 60-key acceptance protocol. The catalog remained
configured; one 3 Down/3 Up cycle primed app artwork, then Up to header/Down to dock reset the
start, with 1.2-second waits. Six measured directions used 350 ms host pauses and a 600 ms tail.
Installer-default compilation/JIT and caches were not pinned for the early variants.

| Diagnostic variant | Frames | Janky frames | gfxinfo p95 |
|---|---:|---:|---:|
| Padded viewport + retained moving dock, warm | 83 | 62 | 85 ms |
| Stationary header glass experiment | 81 | 58 | 109 ms |
| Native-scale Home artwork layers, warm | 95 | 51 | 89 ms |
| Same final APK, explicit speed compilation and own-app relaunch | 92 | 46 | 81 ms |

The header experiment was reverted. Native-scale artwork caching reduced the missed-frame
fraction in these samples but did not improve the p95 consistently. Zoom/press/tilt artwork
stays live, preserving decoded detail while magnified. The moving dock can retain one settled
optical snapshot across lazy disposal, refreshing exact glass after motion settles. Cache keys
include source/style/density/size; invalid sources and unsupported capture retain the existing
live/opaque fallback. The 12 MiB Coil cache and decode limits are unchanged.

A local 10-second scrolling trace was dominated by rendering/buffer waits rather than
recomposition: main-thread postAndWait maximum 38.35 ms; GPU completion wait maximum 41.08 ms;
Recomposer:recompose maximum 15.91 ms. This supports investigating rendering, without isolating
a single GPU instruction as the cause. Driver GPU percentile counters were invalid and ignored.
The final-layer sample reported 38.66 MiB GPU allocation versus 29.67 MiB before artwork layers.
One settled process snapshot reported 57,012 KiB PSS; this is not long-duration memory acceptance.

Search Close was sampled separately after dismissing the IME and verifying Search remained
open. The original scale transition produced 6/7 janky frames (p95 300 ms); stationary sampling
bounds with a fade produced 1/17 (p95 77 ms). Both endpoints were verified to close Search.
Earlier zero-frame/keyboard-dismissal samples were rejected. The original APK was not archived
for this Search comparison, so this is exploratory evidence, not a matched regression benchmark.

Warm-navigation acceptance remains unmet. These measurements do not establish remote-to-photon
latency, broad smoothness, long-term memory behavior or performance of remote artwork loading.
No full unit/instrumentation/lint suite was run for this follow-up, at the user's request.

Final production-source digest (sorted tracked main sources/build configuration): `9341c8cb91dfe879c134048c1e4d5a8701c5f14f83ca02ca6675dd8056a224d1`.
Final APK SHA256: `cf0bc589ebb13aac3e6ceea600bae02aa726646d19b117b27aab7c4845296c60`. Existing baseline profiles shipped; no profile generation or compilation reset was used. Raw captures, frame dumps, trace and comparison APK stay outside tracked source.

## Six-column visual follow-up (2026-10-22)

The retained moving-dock experiment above is superseded: users reported an objectionable
optical jump when its settled capture replaced the frozen image. The current dock draws the
existing preblurred wallpaper at its window position. Position is consumed in draw rather
than composition; there is no dock-specific snapshot readback. This removes that replacement
mechanism; it does not establish a measured frame-time improvement. Native-scale artwork
caching, the 12 MiB shared Coil cache and bounded content decodes remain unchanged. No new
60 fps or remote-latency claim is made.

Verification for this follow-up: `spotlessApply verifyArchitecture test lintDebug
:app:assembleBenchmarkRelease` passed with JDK 21. The three pure dock/blur policy tests
passed. Existing content repository tests now inject the virtual scheduler for Default work.
The minified, resource-shrunk benchmarkRelease APK SHA-256 is
`d20356800a75416c5d7c8156adfd729b66be166d363b8066a849644063bd85ce`.
It was replace-installed and explicitly launched on the connected physical Smart TV Pro,
API 34. App artwork was warm; no additional ART compilation or frame-time capture was run.
Private screenshots confirmed six catalog columns aligned with dock positions and the
fullscreen glass slider. Right then Back cancelled its draft; the HOME resolver was unchanged.
The large continuation layout, RTL and maximum text size still require device acceptance.
Raw screenshots and build logs remain outside tracked source.

## Refractive glass follow-up (2026-10-22)

The preblurred-texture dock in the previous follow-up is superseded at the user's request:
it lacked the refraction expected of liquid glass. The dock now uses the existing live Haze
CLEAR material, without retained capture/readback. This restores optical detail; it is not a
claim of lower GPU cost. Continuation row clipping moves to the physical viewport edges and
the grid scrolls behind the transparent header rather than stopping below a rectangular gap.
Control Center reuses the bounded one-shot scene-copy helper before opening and applies
Haze's supported progressive optics to the side region. No persistent Home capture was added.
No performance budgets are relaxed and no measured frame-time improvement is asserted.

## Tile-local Control Center correction

The user rejected the broad side blur. The one-shot scene now supplies Haze through an
invisible source, using the existing app-context-menu recording technique. Only individual
controls render glass; there is no full-height effect surface or composited scene overlay.
The previous side-region implementation is removed. Audio uses a horizontal Left/Right rail
with retargetable fill and focus emphasis. This corrects visual scope and remote behavior;
no frame-time or GPU improvement is claimed without new measurements.

## Preferred right-side backdrop restored

The user subsequently preferred the broader right-side progressive blur. That presentation
is restored using the same bounded scene snapshot and original side-region effect. The
horizontal audio control and focus refinements remain. Tile-local-only rendering is no
longer the current visual contract. This is a preference reversal, without a performance claim.

The restored-side version passed formatting application, architecture verification and
optimized compilation. APK SHA-256: `47d2f8fcc89d6ca21559d66ccce83a1cac559959fcca068a34995f6b7bfa3110`. Source: working changes over
`fc2b6a9`. Replace-installed and explicitly opened on the physical Smart TV Pro, API 34;
existing warm artwork, no additional ART compilation. A settled Control Center screenshot
confirmed the restored progressive side blur with the newer glass tiles. No frame-time
measurement was performed. Raw capture/logs remain in private temporary storage.

## Compact Control Center underlay

The current preference supersedes the right-side blur: the effect is bounded by the measured
Control Center content plus a feathered 32 dp halo. The panel no longer fills the available
height when its content is shorter. The effect output is the tile glass source, rather than
independently sampling the original sharp screenshot. Power UI and its focus branches are
removed. This is visual/state ownership work; no performance improvement is claimed.

Compact-underlay verification: optimized build, architecture, formatting and Settings lint
passed. APK SHA-256: `a96592ba1ad5a98b6a7ce1f0f9694678cc6b61c54fc97ad0186ba454dd840b52`. Replace-installed and explicitly launched
on the physical Smart TV Pro, API 34, using warm existing artwork and no additional ART
compilation. Private settled captures confirmed no power control, localized Control Center
blur, and a matching softened Audio surface. No volume or system power change was made,
and no frame-time measurement was performed.

The lighter variant reduces only underlay radius (24 to 12 dp) and dark tint (18% to 10%),
and removes the normal Control Center heading. Formatting/architecture/optimized compilation
passed. APK SHA-256: `a870bdbdc11c49c8fabc81331b7e2dbccf8be853d4f1b83e5009dd522983e0af`. Replace-installed and explicitly opened
on the same physical Smart TV Pro/API 34, with warm artwork and no additional ART compilation.
A private settled screenshot verified the title removal and lighter compact underlay. No
frame-time measurement or exact macOS visual-match claim is made.

## Larger continuation and inward blur feather

Large continuation posters now target two columns, bounded by the available hero height and
font scale. Caption clearance accommodates focus enlargement. Only this large layout changes
its outer insets; compact content retains its spacing. Control Center removes its 32 dp outer
halo and feathers inward over 24 dp instead. Its sampled source remains shared by the tiles.
These are layout/material changes, without a performance claim or relaxed budget.

Verification: formatting, architecture, Home/Settings lint and optimized compilation passed.
APK SHA-256: `408bd40ce4b3a1c93ac040662687c8f843d687201b75466982d93c09ba1e2955`. Replace-installed and explicitly launched
on the physical Smart TV Pro, API 34, with warm existing artwork and no additional ART
compilation. Private settled screenshots confirmed larger posters, softened medium/semibold
labels, and the inward Control Center feather. High-contrast labels keep full opacity by
implementation; high-contrast/large-text device acceptance was not repeated. No frame-time
measurement or pixel-identical Apple/macOS claim is made.

## Remote navigation and Settings entry — 23 October 2026

Home now has section Overview, explicit folder Peek, pace-aware decorative motion and a
static more-below cue. Continuation cards retain a stable artwork compositing path and use
bounded insets/gaps. App, folder and continuation lifts share a retargetable critically damped
spring. Unconditional composition-local reads preserve the focus animation state on entry
and release. Profiles adds a feathered progressive blue side material in the glass path.

Settings retains covered Home without its clock, preview/restoration work, focus or semantics;
its unused wallpaper glass recording stops. The opaque-panel policy invalidates its readers
rather than the entire scene. Appearance uses keyed lazy controls and a bounded asynchronous
preview decode. Entry places the category pane/content heading before composing the independent
controls; readiness survives recreation to preserve saved lazy scroll/focus. Control Center
opens Settings immediately, with duplicate activation disabled. These changes preserve the
settled Settings rendering, accepted writes, image quality and the shared 12 MiB Coil cache.

Verification source: working tree over `fc2b6a9`; production-source manifest SHA-256
`c11158916520d84ae4687fcbe027ecb3f1eac7a50a7946196eef3b0fcf9204bb`.
Final optimized benchmarkRelease APK SHA-256:
`45b1de618b5d8dd1abe375203c85be6510575e37ba413fdeff29dd8bcb485828`.
The manifest covers production/build inputs, excluding unit/instrumentation tests and docs;
its file hashes and the exact APK remain in private temporary evidence.

All required Spotless, architecture, module/app unit, lint, instrumentation compilation,
benchmark compilation and optimized compilation gates passed; 64 distinct unit cases passed.
Physical TV coverage includes real remote Settings return/recreation, continuation/header
ownership, remembered section destinations, Overview/Peek/Back, held Select suppression,
member changes, controlled-clock lift continuity, deep lazy Settings restoration and the
visual matrix. The final placement/recreation refinement passed 13 targeted cases; the
unchanged Home navigation code also passed the preceding 19-case matrix. Controlled-clock
continuity is not physical remote latency evidence.

The settled dark Settings fixture before/after has the identical PNG SHA-256
`202a25317888b18b2992e1889737c492c5855ee22036f961417cff0058b56469`.
Debug physical fixtures additionally cover light/English/large opaque Settings and profile
materials. Optimized Home spacing, Overview's folder jump and Peek entry/Back were visually
inspected on the actual configured Home. Current configuration files were byte-identical
before/after final verification, excluding installer profile stamps; system HOME was unchanged.

### Matched optimized entry diagnostic

Physical Smart TV Pro, Android API 34, 60 Hz; current unchanged Economy preferences and
existing wallpaper/artwork caches. Replace-install each APK, explicitly start Oree, navigate
Home header → Control Center → Settings, verify the native destination, reset gfxinfo for
each entry and collect its frame dump. No additional ART compilation, compilation reset,
data clear or cold-cache reset was requested. The first visit follows replacement installation;
it is not a fully cold-cache benchmark. Subsequent visits are warm. The original reference APK
is `408bd40ce4b3a1c93ac040662687c8f843d687201b75466982d93c09ba1e2955`.

| Entry | Original max UI interval | Final max UI interval | Original gfxinfo p95 | Final gfxinfo p95 |
| --- | ---: | ---: | ---: | ---: |
| First after replacement | 442.6 ms | 328.0 ms | 500 ms | 350 ms |
| Warm 1 | 373.0 ms | 209.6 ms | 400 ms | 200 ms |
| Warm 2 | 317.2 ms | 180.6 ms | 350 ms | 200 ms |

The UI interval is HandleInputStart → SyncQueued for valid frames, not remote-to-photon
latency. Each short entry has only 13–14 valid frames; these are diagnostics, not a statistical
budget certification. The mean of the two warm maximum intervals decreased from 345.1 ms to
195.1 ms in this sample. Earlier intermediate candidates and failures remain private rather
than being silently discarded. A final Home memory snapshot was 63,729 KiB PSS; it is not
leak or peak-memory evidence.

A first-entry hitch remains, and these results do not meet or relax the published frame
budgets. They do not establish 60 fps, broad TV compatibility or Balanced-mode frame pacing.
Economy/reduced-motion paths intentionally suppress decorative lifts; Balanced-mode entry/
release continuity is covered by the controlled-clock physical fixture. Raw traces, screenshots,
frame dumps, configuration archives and device identifiers remain outside tracked source.

## Navigation rendering and visual setup — 23 October 2026 follow-up

Card static artwork, rounded clipping, edge and shade now have a separate retained GPU layer
at the full focus enlargement resolution. Reflection, tilt, lift, press and shadow remain live
with their existing springs. There is no bitmap readback, saved-state bitmap or additional Coil
cache. Each composed card owns its layer; oversized raster bounds fall back to live drawing.
Home settlement and Settings focus-memory updates also avoid invalidating their parent lists.
Visual setup exposes five explicit relative workloads without changing existing preferences.

Physical Smart TV Pro/API 34, Mali-G52/OpenGL ES, 60 Hz; optimized R8/resource-shrunk
benchmarkRelease. The current unchanged profile is Balanced, with normal motion, glass disabled
and existing artwork/wallpaper caches. This supersedes the preceding entry-only diagnostic's
Economy preference description for this follow-up. Replace-install and explicitly start each
APK; no ART reset, extra compilation, data clear or cold-cache reset. Installer/JIT state and
OS/driver caches were retained, so these are exploratory diagnostics, not the reference warm
acceptance protocol or remote-to-photon latency measurements.

The protocol uses 20 Right/Left keys in Continue Watching, 20 horizontal plus 6 vertical keys
in the catalog, and 20 Down/Up keys in Appearance, with 110 ms host pauses plus adb overhead.
Verify each native destination and reset gfxinfo before its workload. Frame dumps retain only
the last 120 valid frames; histogram/deadline counts cover the entire reset interval. The
sequence below includes a reverse-order original reinstall to check warm/order effects.

| Sample | Continue Watching p95 histogram / janky frames | Catalog p95 histogram / janky frames |
| --- | --- | --- |
| Original A1 | 101 ms / 168 of 220 | 109 ms / 216 of 260 |
| Full-focus cache B1 | 77 ms / 90 of 272 | 85 ms / 146 of 291 |
| Same cache warm B2 | 69 ms / 97 of 294 | 81 ms / 167 of 334 |
| Original A2 reinstall | 101 ms / 164 of 218 | 109 ms / 214 of 245 |

Valid-frame IntendedVsync → FrameCompleted p95 was 115.73/114.26 ms for the two original
continuation runs and 83.23/83.22 ms with the cache; catalog values were 118.76/120.51 ms and
87.74/87.94 ms. These distinct metrics must not be conflated with physical input latency.
The unchanged-material state-only intermediate recorded 105/113 ms histogram p95, so reduced
recomposition alone did not improve these navigation tails. Settings' layout changed to one
column; its original 73/73 ms and candidate 85/77 ms histogram values establish no Settings
navigation performance win. Frame-budget acceptance remains unresolved; no target is relaxed.

A 12-second private catalog trace identified repeated static Skia operations and render/buffer
waits. It reported 35 ftrace setup errors, with no reported trace-loss/overrun counters; this is
limited diagnostic support, not exhaustive GPU timing or recomposition attribution. An early
cache run copied the previous APK before compilation finished and was stopped/rejected.

Original optimized APK: `45b1de618b5d8dd1abe375203c85be6510575e37ba413fdeff29dd8bcb485828`.
State-only intermediate: `f8b318586d4c2ed7c6492835e2ac9184bce9bee049f621c0e7bb48674f34efc6`.
Measured cache APK: `085f0e27237da015f33172998121e9aa60b8dc829cf885ed6c832953ed0cbeb9`.
Raw inputs, all accepted/rejected samples, profiles, APKs, traces and captures remain private.

The final production manifest is
`7515fec5d7be6ec3178d2f091e539ce63a66e22457ea880c9163caef74f12932`;
final optimized APK is `299e21df93b35ad53544b57d07c23675383d37bdb24ad6677ed549358a5971ac`.
The final bounded-cache/ring refinement and folder-hint removal passed all required gates,
66 distinct unit cases, avatar validation and 25 focused physical-TV interaction cases.
The exact final optimized APK repeated 77 ms continuation p95 (88/263 janky frames) and
85 ms catalog p95 (149/298), with valid-frame total-duration p95 of 83.20/87.64 ms.
These results retain the improvement but still exceed the published frame budget. Setup,
explicit choices, retry focus, profile/backup compatibility, Back/category restoration,
carousel viewport alignment, folder Peek and retargeting continuity are covered separately
from timing. Configured datastore files were byte-identical after the final native tests.

The installed APK bytes match that final artifact. Optimized French full-glass preview,
full-width continuation edges, first-card spacing and enlarged card/dock artwork were visually
inspected on the configured TV. A final settled Home endpoint was 86,832 KiB PSS; it is not
peak, leak or long-term idle evidence. System HOME was only queried, never changed. The
existing appearance preferences were preserved; preview browsing was cancelled without saving.

## Navigation baseline and evidence audit — 23 October 2026

A bounded native wallpaper layer produces a repeatable partial improvement on the physical
TV. The deadline and first-visual-response targets remain unchanged and are **not certified**.
Cross-route, glass, memory and fidelity checks below determine the retained scope; a lower
CPU-frame metric alone is not acceptance.

### Baseline, artifacts and environment

The starting implementation includes every original staged, unstaged and untracked edit.
Its base commit is `fc2b6a9df4ef2350bb624442e27785ef8dd96bb5`; original tracked-patch SHA-256
`8001b5ca6a0d17941368e1f28d0088dbbc13ca207b97262685c312e0bcc27fdd`, original
file-manifest SHA-256 `069e31e4292c157810c6fff8e82aa0b0845dc4338e15c23c922bc654c0401217`.
The original index patch is preserved separately. Historical APKs from before the existing
artwork optimizations are not this audit's baseline.

| Artifact | SHA-256 |
| --- | --- |
| Original optimized/installed A | `299e21df93b35ad53544b57d07c23675383d37bdb24ad6677ed549358a5971ac` |
| Matched diagnostic A2 optimized APK | `562762a667f05a58dea9743ed505fddaa9170eaed2e97eb97f6a84676add30c9` |
| Wallpaper candidate B2 optimized APK | `7796749bff521824fa30edc2124d5c94b81a9ccc7abfebe7e8fe1f36258e0016` |
| Matching benchmark APK | `ed7a10a95946f58ca37816b9f22a01f7556ccc50937d686321430ebb12983084` |
| A2 production-source digest | `b30706ce4f52b031b2cde1a5779f72eab573cdb2639973f17cbe448290d8ba4f` |
| B2 production-source digest | `c05b67371270c0d1d9f8022c201768cf70abe34392266463261e170c9c0940b1` |

A2 and B2 have identical diagnostics and differ in production only in `GlassMaterial.kt`'s
bounded wallpaper compositing strategy. Package `fr.deuspheara.oree`, versionCode 1,
versionName `0.1.0-dev`; same-signature compatibility with the installed package was verified
before replacement. Private manifests include individual source and artifact hashes.

| Component | Resolved version |
| --- | --- |
| Gradle / Temurin JDK | 9.6.0 / 21.0.8 |
| AGP / Kotlin and Compose compiler plugin / R8 | 9.2.1 / 2.4.20 / 9.2.14 |
| Compose runtime, UI, Foundation, runtime-tracing | 1.12.1 |
| TV Material3 / Coil | 1.1.0 / 3.2.0 |
| Haze / Haze Glass | 2.0.1 / 2.0.1 |
| Macrobenchmark / UI Automator | 1.4.1 / 2.3.0 |
| compile / target / minimum SDK | 37 / 36 / 26 |

The resolved graph supersedes the declared Compose BOM 2025.04.01. The original packaged
baseline.prof/baseline.profm are 10,734/557 bytes; packaging alone does not prove AOT coverage.
All dependencies and tools came from existing local caches; no upgrade or remote upload ran.

Hardware: physical Smart TV Pro/API 34, Mali-G52, Skia OpenGL renderer, approximately 60 Hz
(16.67 ms), native app surface 1920×1080 on a 3840×2160 display. Full device/build/driver
identities remain private. Thermal service reports status 0, but identical sensor readings
are insufficient to establish controlled thermals. Frequency/scheduler coverage is recorded;
GPU execution counters and full driver allocation accounting are unavailable. The configured
profile is Balanced, full motion, intensity/blur 1, ordinary transparency/contrast. Glass was
originally ON; OFF experiments use only the existing app control and restore ON afterward.

An old host ADB server initially hid the authorized TV connection. Restarting that host server
restored it. This did not require changing TV debugging, network, display, power or HOME settings.

### Supported rendering change and rejected hypotheses

`WallpaperBackground` groups its full-resolution image, live crossfade/frost and shading into
one native offscreen compositing layer up to 4,194,304 pixels, with Auto fallback above that
bound. At the measured surface its RGBA target is approximately 7.91 MiB, before renderer
bookkeeping. This is native raster reuse, separate from Coil decoded-image reuse and retained
artwork commands. It introduces no CPU bitmap capture, lower-resolution image, quality flag,
shortened animation, removed reflection/shadow or altered focus bounds. The existing Haze
source remains inside the layer and must track replacement, movement and reattachment.

The evidence supports a render-path bottleneck, not a blanket recomposition problem. In one
matched ~4.88-second A2 window, 217 `postAndWait` calls total 3,927 ms inclusively; in the B2
window, 286 calls total 161 ms. Inclusive RenderThread Drawing falls 2,635→1,912 ms despite
more frames. Main running time rises 728→971 ms and RenderThread running time rises
1,104→1,355 ms because more frames are processed. These overlapping wall-time slices must
not be added or subtracted to invent GPU time. The largest observed recompose slice across the six inspected matched traces is
3.49 ms; this does not justify a broad composition rewrite. Available traces do not isolate the hardware
mechanism behind the reduction; the isolated compositing experiment establishes the effect.

Repeated static card recording was falsified for warmed continuation navigation: zero
`TVL:artworkRecord` calls in those measured windows, with a startup positive control recording
15 calls and valid process attribution. Existing retained artwork, live reflection and transform
behavior remain. The proposed remembered-draw-modifier experiment was not applied. No
prefetch/cache enlargement, focus coordinator rewrite, blanket stability annotation, dependency
upgrade, ModulateAlpha substitution or graphics backend change was justified or retained.
Haze's pinned implementation was inspected locally; upstream fixes were not fetched under
the no-new-network constraint, and no claim of upstream equivalence is made.

### Measurement protocol and trace validity

Both optimized non-debuggable builds use R8/resource shrinking and the same instrumentation.
`LauncherNavigationBaseline` uses `CompilationMode.Partial(Require, warmupIterations=3)`,
three measured iterations and no COLD startup/cache-drop mode. Bytecode inspection confirmed
API 34's non-destructive ART/profile reset; the harness refuses configured-device execution
below API 34 where the pinned reset can uninstall. Connected-test installers are not used.
OS, image and driver caches are retained and setup explicitly primes the route after install.
Full AOT and Ignore are not used as representative acceptance modes.

The device schedules every Down/Up pair at absolute 150 ms intervals. Preallocated event
records include action, key, repeat, down/event uptime, monotonic injection start/end and
accepted status. Late events are never skipped. Measured delivery spans 149–151 ms with
median 150 ms in the OFF confirmation. Native hierarchy access and file IO stay outside bursts.
App markers verify every received event identity, focus ordinal and stable destination identity.
Raw IDs remain private. Visible routes use 30 alternating Right/Left pairs; continuation
traversal uses 30 RRLL pairs; grid traversal uses 60 horizontal/vertical pairs. Each case primes
the same keys before measuring. The analysis window is first received key through 500 ms
after the last received key, excluding unrelated idle/setup. The last endpoint is checked in
trace because Macrobenchmark kills the app during final cleanup.

The independent OFF confirmation was predeclared B/A/A/B, each block with fresh installation
and compilation treatment and three measured iterations. Exclude failed injection/receipt,
changed identities/geometry/profile, foreground loss and trace errors/loss; preserve failures.
All twelve iterations pass those checks, with identical per-key destination walks across blocks.
The glass-ON cross-route A/B screen is separately predeclared; it is not a balanced sustained
confirmation and cannot extend the OFF confirmation's strength to every route.

Default Macrobenchmark tracing produced 82 setup errors and buffer loss and was rejected for
precise attribution. The custom 64 MiB CPU/input/FrameTimeline config has no such quality
issues in accepted samples. It omits explicit gfx but still captures some HWUI/Skia slices via
other categories/application tags; it does not supply GPU hardware counters. Standalone
startup tracing additionally needs task creation/rename and process polling to avoid attributing
app slices to `usap32`. A positive control verifies full-package attribution. Supported and
unsupported source coverage, exact configs and quality reports are archived. Passing the
checker establishes prerequisites, not workload or input-to-photon acceptance.

App frame overrun is `(actual.ts + actual.dur − expected.ts − expected.dur)` after joining actual
and expected application FrameTimeline rows by process and surface-frame token. Surface
frame duration is actual.dur; its endpoint must not be substituted for display presentation.
Neither metric is CPU-running time or input latency. App Deadline
Missed and Buffer Stuffing classifications are counted separately and may overlap.
`frameDurationCpuMs` from FrameTimingMetric is its CPU-side frame interval, not a sum of
scheduler-running slices, shader time or the first visual response. Quantiles below use the
navigation windows rather than diluting failures with idle frames.

### Glass-OFF confirmation

| Independent block | Frames | Positive overrun | App Deadline Missed, including overlaps | Buffer Stuffing, including overlaps | Iteration overrun p95 (ms) |
| --- | ---: | ---: | ---: | ---: | --- |
| B2 first | 852 | 831 | 6 | 825 | 12.70 / 12.58 / 12.77 |
| A2 first | 645 | 645 | 206 | 642 | 66.54 / 66.54 / 66.54 |
| A2 second | 645 | 645 | 209 | 642 | 66.58 / 66.57 / 66.51 |
| B2 second | 852 | 832 | 6 | 825 | 12.58 / 12.60 / 12.72 |

Each block delivers 90 Down/Up pairs with 90 matching focus moves; all 360 moves are accounted
for. There are 1,290 A2 and 1,704 B2 frames. App-miss incidence falls 415/1,290→12/1,704,
but B2 still has positive app-completion overrun on 1,663/1,704 frames and 1,650 Buffer Stuffing
classifications. Therefore **the <1% display-deadline target is not established**. Buffer Stuffing is
a separate pipeline classification, not automatically an additional missed display refresh. More frames
and fewer app misses do not remove downstream presentation latency or prove 60 fps.

Joining app surface tokens to SurfaceFlinger display tokens separately gives 1,296 A2 and
1,710 B2 mapped display frames; these include one unresolved app-duration row per iteration,
excluded from the completed-app-overrun counts above. Every mapped compositor frame is
classified On-time Present / None. App-buffer presentation spacing p50/p95/p99 ranges are
A2 16.68/33.37–33.38/33.40–33.42 ms versus B2 16.67/16.70–16.71/16.71–16.74 ms.
There are 428/1,290 A2 versus 6/1,704 B2 mapped intervals above 25 ms. Intervals can include
the settling tail; this is a short OS FrameTimeline cadence diagnostic, not a sustained pass
or per-event changed-pixel association. It distinguishes late app production/buffering from
compositor deadline misses. The app-actual-end to mapped display-end p95 is about 31.9 ms
for A2 and 23.6 ms for B2; this combined pipeline interval is not isolated GPU execution.

Device BOOTTIME injection records also align with app markers: all 720 receipt markers
fall inside their synchronous injection-call bounds. Per-iteration injection-start to app-marker
p95 ranges are A2 28.4–41.6 ms and B2 12.9–22.5 ms. App Down receipt to focus callback
p95 remains small (A2 0.95–1.20 ms; B2 1.61–1.94 ms). These synthetic-input components
support rendering backpressure rather than slow focus search as the main observed delay;
they omit the first changed draw/presented pixel and do not certify remote-to-photon latency.

The initial separate A1/B1 screen (earlier identical artwork-marker instrumentation) gave
FrameTimingMetric CPU-frame p95 49.8→13.8 ms and overrun p95 approximately 66.5→13.4 ms.
It is supporting screening evidence, not pooled with A2/B2 confirmation. Its settled wallpaper
ROIs differ by at most 1/255 per channel (mean absolute difference <0.0005 8-bit levels),
with clocks excluded. This narrow comparison cannot certify motion or moving glass.

Other OFF baseline routes, three iterations each, used A1 markers: continuation traversal
200 frames/iteration, overrun p95 77.61–77.63 ms; grid visible 192 frames/iteration,
71.59–72.37 ms; grid traversal 373 frames/iteration, 72.36–72.42 ms. Their received events and
ordinal walks pass quality checks, but no matched candidate result is inferred from them.

In the matched OFF confirmation, Macrobenchmark's CPU-frame p50/p95/p99 are
40.63/50.11/51.58 ms and 40.72/49.91/51.17 ms for A2 blocks, versus
8.87/13.06/13.56 ms and 8.88/12.82/13.58 ms for B2 blocks. Its frame filters produce
643/643 A2 and 851/852 B2 samples, distinct from the explicit app-key analysis window's
FrameTimeline denominator above. Do not mix those denominators. Explicit-window surface
frame-duration p50/p95/p99 ranges across iterations are A2 63.25–63.89/67.95–68.45/
72.58–72.90 ms versus B2 24.95–24.99/28.69–28.80/28.95–29.55 ms. These are application
surface intervals, not a claim of equivalent display cadence or input response.

### Glass-ON cross-route screen

The original Balanced/glass-ON profile uses the same real destinations, input counts and
geometry in A2 and B2. All four cases pass three iterations per build, clean trace checks and
per-key stable identity comparison. These are one ordered A/B screen, separate from the
balanced OFF confirmation; do not pool them as independent sustained acceptance.

| Route/build | CPU-frame p50/p95/p99 (ms), Macrobenchmark | App misses/completed app frames | App-overrun p95 range across iterations (ms) |
| --- | --- | ---: | --- |
| Continuation visible A2 | 48.73 / 51.83 / 52.63 | 288 / 572 | 78.99–79.05 |
| Continuation visible B2 | 32.34 / 41.67 / 42.46 | 93 / 770 | 52.96–62.28 |
| Continuation traversal A2 | 49.01 / 56.45 / 57.22 | 309 / 552 | 88.58–88.60 |
| Continuation traversal B2 | 32.39 / 41.74 / 42.27 | 93 / 770 | 53.61–53.89 |
| Grid visible A2 | 47.48 / 48.86 / 48.99 | 288 / 573 | 79.50–79.55 |
| Grid visible B2 | 10.00 / 14.31 / 14.88 | 3 / 855 | 9.98–10.01 |
| Grid traversal A2 | 47.83 / 49.03 / 49.18 | 558 / 1,116 | 79.80–79.83 |
| Grid traversal B2 | 10.48 / 27.06 / 27.48 | 4 / 1,668 | 10.37–27.02 |

Visible/continuation-traversal cases deliver 90 Down/Up pairs per build; grid traversal
180. App-miss counts include classifications overlapping Buffer Stuffing. Completed-app
counts use the explicit key window; CPU-frame quantiles use Macrobenchmark's own filters.
Variation is retained: one candidate grid-traversal iteration has materially higher overrun,
and candidate continuation visible has a 62.28 ms tail rather than the other two ~53 ms tails.
No warmed artwork-record calls occur in any of these A2/B2 measured routes. First allocation
and uncached artwork preparation are separate from these already primed journeys.

Mapped app-buffer presentation spacing p95 remains ~33.35 ms for candidate continuation
(~33.4 ms baseline). Intervals above 25 ms fall 288/572→90/770 for visible continuation and
309/552→90/770 for traversal. Grid p95 spacing improves ~33.38→16.70 ms, with above-25-ms
intervals 285/573→0/855 visible and 558/1,116→1/1,668 traversal. Mapped compositor frames
are all classified on time with no compositor jank in these windows. One unresolved app
completion row per iteration is counted in display mapping but excluded from app-overrun
calculations. Display mapping does not prove which frame first changes after each input.

Enabled continuation still has 93/770 (12.08%) app deadline misses in each candidate case:
**the overall <1% deadline target remains unmet**. The grid's favorable short-run fractions
do not certify sustained acceptance. The extra glass cost differs by visible surface;
these results do not justify changing every glass implementation or disabling glass.

A second isolated experiment moved the identical native cache inside `hazeSource`, based
on pinned Haze bytecode showing source capture through `record(drawContent)` and effect
sampling through `HazeArea.contentLayer`. Its three physical source/softening cases passed,
configured bytes stayed unchanged and all measured destinations matched B2. However app
misses remained 93 and overrun p95 was 52.77/52.80/62.26 ms versus B2
53.00/52.96/62.28 ms. This failed the predeclared >5% screening threshold. The experiment
was rejected and the exact B2 source/APK restored. Do not interpret retained Haze commands
as proof of pixel caching, or this negative result as an isolated GPU shader measurement.

### Memory, lifecycle, held input and idle diagnostics

Memory queries run in a separate host-driven pass, never in the frame-timing bursts. Each
build receives four cycles of 30 grid key pairs, returning to the same native anchor, followed
by five seconds settled, twelve seconds behind the unchanged default HOME, and explicit
resume. Sampling starts actually span 1.30–1.57 seconds, with query duration recorded; these
are sampled peaks, not guaranteed allocation peaks or first-start peaks.

| Measurement | A2 | B2 |
| --- | ---: | ---: |
| Sampled maximum PSS | 78,094 KiB | 78,491 KiB |
| Settled PSS | 71,930 KiB | 72,407 KiB |
| Settled HWUI GPU cache | 50,342,204 bytes | 67,053,884 bytes |
| Scratch render-target entries at cycle 3 | 76 | 78 |
| HWUI cache after background | 3,974,400 bytes | 861,120 bytes |
| HWUI cache after resume | 24,296,764 bytes | 41,041,212 bytes |

The measured cost is **15.94 MiB additional reported GPU cache**, not merely the nominal
7.91 MiB wallpaper target. Two additional render-target entries account for a byte delta
consistent with two aligned 1920×1088 RGBA8 targets; roughly half the extra endpoint cache
is reported purgeable. Candidate cycle caches stay 66,971,964/66,971,964/67,053,884/
67,053,884 bytes. This supports a bounded working-set increase over the tested cycles,
with release under background context policy; it does not prove long-term leak freedom.
The 12 MiB Coil cache, decode/request limits and renderer cache policy are unchanged.

Reported PSS stays below the existing 100 MiB static and 150 MiB enriched thresholds in these
samples. HWUI GPU-cache accounting is separate: the device reports Graphics PSS as zero,
which is not evidence of zero GPU allocation. Do not add PSS and cache values as an exact
physical total. Full driver memory, rapid profile turnover peaks and 10/30-minute trends
remain unverified; no resource budget is relaxed.

A separate optimized synthetic D-pad longpress pass delivers five holds with one repeat each:
15 app-received events, repeat count 1 and 404–417 ms actual Down-to-repeat observation.
Right/Left reversals return to the original continuation ID, and holding Left at the boundary
produces no unintended move. The eight expected focus changes are all observed. This does
not establish a physical remote's repeat cadence or visual response time.

One settled candidate idle interval spans 60.89 seconds including final snapshot overhead:
16 process ticks at 100 Hz, 0.263% of one core, one rendered frame. App foreground and TV
awake are verified at both endpoints. This supports absence of a permanent idle animation
in that interval, not uninterrupted visibility, a matched idle CPU saving or long-term behavior.

### First appearance after process restart

Standalone startup captures retain OS/driver/image caches and do not reset ART again. They
use the process-discovery config and pass package/scheduler/FrameTimeline quality checks.
Android's startup library labels them cold because the process restarts; they are **not
cold-cache measurements**. TTID is first Activity display, not Home usable, wallpaper-ready
or first visual input response. Twelve artwork-record calls occur in each startup capture.

Initial A2 TTID samples are 772.07/654.62/654.34 ms and B2
784.76/656.59/668.69 ms. Their small differences motivated a separately predeclared balanced
B/A/A/B diagnostic rather than a claim of startup noninferiority:

| Block | Build | First restart after replace-install (ms) | Second process restart (ms) |
| --- | --- | ---: | ---: |
| 0 | B2 | 995.86 | 650.78 |
| 1 | A2 | 775.03 | 644.94 |
| 2 | A2 | 817.94 | 652.52 |
| 3 | B2 | 771.43 | 663.43 |

The large install-to-install variation and small subsequent-restart differences do not resolve
startup noninferiority; the 995.86 ms candidate outlier is retained. These samples cannot
certify first-display tails, actual cold-cache behavior or the 250 ms warm-usable budget.
No startup regression is hidden in warm-navigation averages, and no release-wide startup
acceptance is claimed. Controlled compilation/launch-phase and usable-pixel association
remain necessary before a stronger startup conclusion.


### Functional verification and preservation

The final offline build gates pass: Spotless, architecture, module/unit tests, lint, debug and
optimized compilation, app instrumentation and benchmark compilation. There are 66 distinct
passing unit cases; avatar validation and six trace-checker cases also pass. The final build's
production APK and source manifest still match B2. Exact commands appear below.

The physical API 34 direct-instrumentation rerun passes **31/31**: the exact 25 current
historical-equivalent methods, three synthetic wallpaper cases, and three additional native
focus cases for held Up, rapid reversal/activation and stable animated focus bounds. The first
run passed 30/31; its Settings-return fixture incorrectly assumed Control Center initially
focused Settings. The current UI can focus Wallpaper above it. The fixture now waits for
native focus and sends Down when necessary before Select. The preferred control in code remains Settings, but Wallpaper focus was already recorded
on baseline before this rendering experiment. The production entry-focus policy was unchanged;
the complete 31-case rerun passes, including three repeated Settings visits. It verifies
navigation and return, not deterministic initial Settings focus. Keep the failed
run separate rather than summing overlapping executions.

`WallpaperBackgroundTest` uses project-owned synthetic pixels on the physical renderer to
check replacement, frost/softening, resize, detach/reattach, moving Haze sample coordinates,
source replacement and a controlled-clock intermediate softening frame. These establish
freshness and continuity in the tested cases, not human-perceived smoothness, release-wide
accessibility compatibility or a native input-latency measurement. Oversized fallback and
other devices/backends remain outside this physical surface's coverage.

Matched settled glass-ON captures use the same real continuation ID and geometry. Sampled
dock-glass, dock-edge and artwork regions are pixel-identical between A2/B2; wallpaper-only
regions differ by at most 1/255 per channel, mean absolute difference below 0.0005 8-bit levels.
One real round trip to the other existing app profile visibly changes wallpaper/content and
returns to the original native anchor. Original wallpaper, glass and artwork ROIs are then
pixel-identical to the pre-switch candidate capture. This establishes the tested settled
freshness, not rapid-turnover peaks or absence of every transient artifact.

The final configured datastore and wallpaper match the original private archive byte for byte,
including restoration after the real profile round trip. The verified B2 optimized APK is
reinstalled and explicitly started. Default HOME remains the original Google TV component;
no accessibility services are enabled. The user's physical remote check reports correct-looking glass and smooth horizontal movement,
with remaining vertical lag. This is qualitative feedback, not a latency or complete focus/
restoration certification; full-page vertical navigation remains outside the favorable
bounded-grid cadence result.

The app's ordinary Export destination was unavailable because the TV's CREATE_DOCUMENT
activity is a stub. After verifying signing compatibility, a same-signature debug replacement
allowed a private `run-as` archive without clearing data or starting the debug app first.
Configured profile datastore and wallpaper bytes match that archive after restoring glass ON
and before/after both native runs. Installer profile stamps are installation metadata and are
excluded from the user-data comparison. Initial byte-for-byte preservation before the first
debuggable replacement cannot be established because the original non-debuggable app denied
access. No default HOME, enabled accessibility service, display/power policy, permission grant,
launcher removal, account, telemetry or new network endpoint was introduced. Raw APKs, traces,
captures, data archives and device/content identities remain in ignored `.private/` storage.

### Reproduction

Use the existing JDK 21 and cached dependencies; do not download tools or upload evidence.

```sh
./gradlew --offline spotlessCheck verifyArchitecture test lintDebug \
  :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleBenchmarkRelease \
  :app:assembleDebugAndroidTest :benchmark:assembleNonMinifiedRelease
python3 scripts/export-avatars.py --check
python3 -m unittest discover -s scripts -p test_trace_quality.py
```

Before any configured-device execution, archive recoverable data and verify package/signing
compatibility; use replacement installs only. Inspect the API guard and pinned installer/reset
behavior. Use the authorized serial from private local configuration, never a committed ID.

```sh
adb -s "$TV_DEVICE" install -r app/build/outputs/apk/benchmarkRelease/app-benchmarkRelease.apk
adb -s "$TV_DEVICE" install -r benchmark/build/outputs/apk/nonMinifiedRelease/benchmark-nonMinifiedRelease.apk
adb -s "$TV_DEVICE" shell am instrument -w -r \
  -e class fr.deuspheara.oree.benchmark.LauncherNavigationBaseline \
  -e additionalTestOutputDir /sdcard/Android/data/fr.deuspheara.oree.benchmark/files/navigation-evidence \
  fr.deuspheara.oree.benchmark/fr.deuspheara.oree.benchmark.LauncherBenchmarkRunner
```

Prefer a unique output directory for every independent block. Preserve each APK, source hash,
exact exported config, JSON event records and traces. Run `scripts/trace_quality.py` with the
actual cached processor binary for every trace; validate per-event focus identities and
navigation geometry before computing frame distributions. The benchmark asset is the warm
CPU/input path; standalone `trace-config-cpu.pbtxt` adds process-discovery polling. Heavy
memory/allocation work belongs in separate diagnostic passes. The legacy host-driven
`capture-tv-trace.py` remains explicitly diagnostic and cannot establish input response.

### Retained scope and target status

The retained change is the bounded native wallpaper layer outside the existing Haze source,
plus trace-only diagnostics, the guarded device-side navigation harness, trace-quality tools,
focused wallpaper regressions and the native test precondition correction. The unsuccessful
source-order variant was restored exactly; no rendering-quality, motion, input, dependency,
Coil/decode, HOME or system-policy change is retained. Existing user edits and the original
index remain preserved. Raw evidence stays ignored; no public push or release occurred.

| Target | Status after this investigation |
| --- | --- |
| <1% missed deadlines at actual display cadence | **Unmet overall.** Original glass-ON continuation still has 12.08% App Deadline Missed in each candidate case; favorable grid/OFF short runs do not certify sustained acceptance. Buffer Stuffing and app-overrun remain separately reported. |
| First visual input response p95 <50 ms | **Unverified.** Receipt/focus timings and display-token joins do not identify the first presented frame containing each intended visual change. No high-speed physical capture validates that association. |

This is a useful warm-navigation partial improvement with preserved sampled fidelity and a
measured bounded GPU-cache cost. It is not release-wide rendering acceptance. Remaining work
includes the reported full-page vertical lag, per-surface glass/GPU critical-path attribution, validated event-to-changed-pixel
association, sustained balanced glass-ON runs, controlled startup/usable-pixel measurement,
and longer resource/profile-turnover tests. Missing counters, startup uncertainty and the
extra graphics working set are material limitations, not grounds to relax the budgets.


## Full-page vertical navigation and source-aligned glass — 23 October 2026

This follow-up uses retained B2 as its baseline. After the user narrowed the task, every new
performance run measures vertical page scrolling; no further horizontal benchmark ran.
A bounded filtered-wallpaper prototype improves p95 while the dock's optics remain live,
but repeatable transition-tail regressions reject it. The exact B2 production renderer is
restored. This investigation does not claim a vertical-lag fix or certify either target.

### Reproduced workload and original remaining cost

The physical API 34 Mali-G52/Skia OpenGL TV retains its approximately 60 Hz mode, 1920×1080
app surface and 3840×2160 physical display. Its configured Balanced profile, normal motion,
glass intensity and backdrop blur remain unchanged. All dependency/toolchain versions match
the B2 inventory above. Builds use cached dependencies, offline Gradle and JDK 21.

`fullPageFirst` restarts the process, prepares a stable authentic hero/header anchor, then
measures six Down/six Up presses through hero, dock and four catalog rows. It does not prime
the catalog tour. `fullPageWarm` primes one complete tour and measures three tours. Both use
absolute 500 ms device scheduling, recorded native key identities/destinations and a 700 ms
tail. Partial Require has three compilation warmups and three measured iterations per method.
OS/file/image/driver caches remain; process restart is not cold-cache testing. No hierarchy,
file IO or memory query occurs in timed input bursts. First Home appearance is separate.

The initial ON/OFF screen used a provider row whose ID changed on package replacement; it
is screening only. The strict comparison supplies a private authentic stable hero tag and
checks the same actual focus walk plus visible content texts and bounds in every iteration.
Unfocused provider IDs can regenerate without changing a selected destination or scene.

| Retained B2, strict actual vertical route | CPU-frame p95 | App deadline misses | Mapped presentation-spacing p95 |
| --- | ---: | ---: | ---: |
| Glass ON, first traversal | 91.78 ms; return repeat 91.55 ms | 203/722; repeat 200/725 | about 66.6 ms |
| Glass ON, warm three-tour traversal | 91.94 ms | 586/2,197 | about 66.6 ms |
| Glass OFF, first traversal | 51.19 ms | 91/926 | about 33.3 ms |
| Glass OFF, warm three-tour traversal | 47.95 ms | 273/2,794 | about 33.4 ms |

These CPU-frame intervals include pipeline blocking; they are not pure CPU-running time,
GPU shader execution or input latency. Scrolling that changes the viewport is much more
expensive than moving focus within already visible rows. Main-thread postAndWait and HWUI
rendering/flush intervals expand on those transitions; the trace does not justify a focus
coordinator rewrite or identify isolated GPU-stage duration. Even the OFF route remains costly.

The scene contains the sharp/frost/veil/gradient wallpaper, small fixed header glass,
moving clear dock and existing artwork/reflection layers. Movement changes relative source
coordinates, clipped capture size/origin and optical keys. The existing 280 ms frost/veil
transition changes source pixels. Authored optics and motion timing stay fixed. A geometry
probe observes 46 layer-group release calls per tour; warm vertical tours still record 26
artwork layers as lazy content leaves/re-enters. The previous zero-artwork-record result
for bounded horizontal navigation does not describe this route.

### Isolated candidates

| Screen | First vertical CPU-frame p95 | Decision |
| --- | ---: | --- |
| Existing Haze separate source/filter/optics graph with moving crop | 106.41 ms | Reject: slower, cropped source/filter still invalidated |
| Retain layer identities across resize/invalidation | 91.73 ms | Reject: no useful improvement; 16 fixture frames pixel-identical |
| Native final dock layer | 93.84 ms | No vertical gain; parked, no further horizontal validation |
| Diagnostic zero glass-filter radius | 74.51 ms | Never retained: changed appearance, padding and graph; not isolated blur GPU time |
| Additional inner wallpaper layer while keeping B2 outer layer | 86.27 ms | Reject: marginal gain, 205/737 app misses vs repeat 200/725 |
| Equivalent equal-radius rounded-box SDF | 89.50 ms | Reject: below declared 5% screen threshold |
| Exact flat-interior surface-gradient early return | 89.64 ms | Reject: below threshold |
| Corrected source-aligned filtered-input prototype | 71.10 ms | Promising partial screen: 177/756 app misses; confirm guarded production candidate below |

The first source-aligned implementation targeted an optional source-area clipping hook that
Glass skips. It did not activate source-aligned bounds and still recorded source/filter about
50 times per tour (106.33 ms p95). This is an implementation diagnostic, not evidence against
reusable filtered input. The corrected mandatory-root hook produces a stable full-root input;
its measured source/filter recording count is zero while optical output updates 57–58 times.
Native Offscreen requests filtered raster retention. CPU recording counts alone do not prove
GPU cache hits, and no per-stage GPU execution time is inferred.

### Rejected guarded prototype and alternating confirmation

The private prototype used a pinned local Haze 2.0.1 adapter (removed after rejection). It normalizes
input identity into capture space, bounds the source-aligned capture to one translated full-root
source no larger than 2 Mi raw pixels, and selects cached filtering only for passive uniform wide
glass. Other material configurations and backdrop rendering retain the original fused renderer.
The platform capability remains unchanged in the prototype. At most five sampled targets, each bounded by that
raw root at scale no greater than 1, stay under the existing retained-pixel budget. The patch
owns no bitmaps, saved-state pixels or global mutable rendering state. Haze retains lifecycle,
trim/disposal and live child-source dependencies. No shader approximation or quality-mode
reduction is retained. The source-aligned sampling phase differs from the moving cropped grid.

| Artifact | SHA-256 |
| --- | --- |
| B2 source | `c05b67371270c0d1d9f8022c201768cf70abe34392266463261e170c9c0940b1` |
| B2 optimized APK | `7796749bff521824fa30edc2124d5c94b81a9ccc7abfebe7e8fe1f36258e0016` |
| Rejected guarded source | `70e167b3a41c1d6cd621df48f56b4f0cb49e4d3091237a441017070a470e45e2` |
| Rejected guarded optimized APK | `8d3201b57958eb938c298a53b31bc5f2f1d333e7698553a37bde1890ea076fee` |
| Fixed benchmark APK | `e32c48e57eef1b6b6b7a08f04ad0fa72c2101b7567b712c0758b83bd3539e854` |

Production-source hashing uses `scripts/profile_rules.py:main_code_sha`. Every measured install
is pulled back and hashed before warmup. Original staged changes remain untouched. Raw source
snapshots, APKs, traces, personal captures, provider IDs and configured-data archives stay ignored
in `.private/vertical-glass-20261023/`.

| Block / journey | CPU-frame p50 / p95 / p99 (ms) | App deadline misses | Frame-overrun p95 / p99 (ms) |
| --- | ---: | ---: | ---: |
| A1 / warm | 26.79 / 93.67 / 126.56 | 590/2183 (27.03%) | 148.39 / 208.41 |
| A1 / first | 11.43 / 91.83 / 132.54 | 198/726 (27.27%) | 144.39 / 213.74 |
| B1 / warm | 27.73 / 73.75 / 218.76 | 506/2243 (22.56%) | 113.88 / 358.33 |
| B1 / first | 11.36 / 72.99 / 219.18 | 177/746 (23.73%) | 111.77 / 360.24 |
| B2 / first | 11.47 / 72.69 / 218.94 | 172/749 (22.96%) | 111.48 / 361.91 |
| A2 / first | 11.33 / 91.75 / 131.36 | 200/726 (27.55%) | 145.95 / 208.30 |

Both candidate first-traversal blocks reproduce roughly 21% lower CPU-frame p95 and fewer
application misses, but CPU-frame p99 rises from 131–133 to 219 ms and overrun p99 from
208–214 to 360–362 ms. The single warm pair repeats the tail regression. The candidate is
**rejected**, rather than reporting only its favorable p95. First-traversal mapped presentation
spacing p95 remains approximately 66.6 ms; warm spacing p95 ranges 50.0–66.6 ms versus 66.6 ms.
Mapped compositor frames report no jank; that does not negate missed app-content deadlines or
Buffer Stuffing. All 18 traces pass quality checks, delivered-event and actual focus/scene
matching. Thermal-service status is 0 in all block snapshots; vendor temperatures are reported
without treating those coarse values as a calibrated throttling/frequency measurement.

The six candidate first-traversal traces have no source/filter CPU-record calls within the
measured window, with 55–58 live optical records per tour. Thus fewer command recordings do
not establish cheap pixels when live source-layer dependencies change. Separate native stage
GPU execution remains unavailable. The raw analysis retains main/RenderThread running sums,
application timeline classifications and compositor mapping; CPU running is not obtained by
subtracting blocking intervals from overlapping frame metrics.


The completed predeclared order is baseline A1/candidate B1/candidate fresh B2/baseline fresh A2. First
traversal repeats independently in every block. Warm traversal has one matched A1/B1 pair with
three measured 36-key iterations per build; do not claim independent warm ABBA replication.
All received keys, focus walks, scenes, window state and fail-closed trace quality match.
Read-only thermal/display state is recorded outside bursts. CPU-running work, app completion/
deadlines, Buffer Stuffing and mapped compositor presentation remain separate.

### Appearance, resources and rejection

Both exact original-renderer and guarded debug builds pass five native glass/source cases.
The 21-frame fixture covers movement/reversal, translucent source replacement, transformed
parents, resizing, detach/reattach, and stationary frost for both narrow and wide cached glass.
The wide freshness assertions reject a stale or empty filtered input. Comparison maximum
whole-frame mean RGB difference is 0.090 levels/255, maximum channel difference 42, and at most
0.732% of frame pixels differ by more than 3 levels, localized during movement. The new settled
wide sharp/frost frames differ by at most 3/2 levels. These are small sampling-phase differences,
not bit-identical motion. Moving and stationary pairs were visually inspected. Controlled clocks
and settled screenshots do not establish event-to-first-visible response.

The separate preliminary memory screen compares exact B2 with the corrected, broader staged
prototype, not the final guarded APK: sampled maximum PSS is 87,881 vs 87,720 KiB; tail HWUI
GPU cache is approximately 75.9 vs 90.6 MB (about 14 MiB extra). Resource counts plateau in ten
vertical tours, and background trimming is observed. These separate PSS/HWUI figures are not
combined total driver memory or unsampled peaks. The final guarded candidate is rejected on
frame tails, so its planned additional startup, profile and memory runs are not executed.
That avoids spending further device time validating an implementation that will not ship.
No first-appearance or sustained-memory acceptance claim is made for that APK.

After removing the prototype, required offline gates pass again on the restored B2 source:
Spotless, architecture checks, 66 distinct unit cases, module/Debug lint, Debug and optimized
compilation, and native/benchmark APK compilation. The optimized rebuild is byte-identical to
B2 (`779674…0016`). Thirteen focused physical-TV vertical, dock/header restoration, held-Up
and Settings/recreation/lifecycle cases pass. Their debug APK is
`ebcf9f142115fa464032e42268eb7790f4c29f64a5ff51cf3de5902051251106`; test APK is
`74db4111e49638917b338738d5f05f33674ba1207b28ed26f365af8ca1e20a81`.
Configured DataStore/wallpaper bytes match the task-start archive before and after that suite.
The exact optimized B2 APK is pulled back and hashed after installation, explicitly started,
and visually inspected on the TV. Default Google TV HOME and zero enabled accessibility
services remain unchanged; no display/power setting is written.
The original staged index remains byte-identical. The retained fixture/test-plan additions
improve diagnostic coverage; they do not change production rendering behavior.

No additional physical-remote acceptance is requested for the rejected candidate. The user's
previous visual approval describes the retained renderer, not a latency measurement or approval
of this prototype. Controlled-clock fixture fidelity does not override a frame-tail regression.

Required offline gates pass for the prototype: Spotless, architecture, module tests (70 distinct unit cases),
Debug lint, optimized APK and native/benchmark compilation; avatar validation has 8 passes and
trace-quality tooling has 6. The rejected prototype also passed four additional unit cases for matrix/coordinate
equivalence and bounded geometry/rendering-policy fallbacks; these leave with the rejected code. Keep functional and frame-performance evidence
separate; compilation is not acceptance.

### Online reference review and decision

At the user's explicit request, online research reviewed primary implementations without
fetching dependencies. [Kyant Backdrop's shader](https://github.com/Kyant0/AndroidLiquidGlass/blob/kmp/backdrop/src/commonMain/kotlin/com/kyant/backdrop/internal/Shaders.kt)
uses compact rounded-shape calculations and an edge-limited lens; its
[blur implementation](https://github.com/Kyant0/AndroidLiquidGlass/blob/kmp/backdrop/src/commonMain/kotlin/com/kyant/backdrop/effects/Blur.kt)
uses platform BlurEffect. [Liquid](https://github.com/FletchMcKee/liquid) is another Compose
source/effect-node reference. [Shayann's measured-model notes](https://github.com/shayann07/liquidglass/blob/main/docs/research/measured-model.md)
separate fitted iPhone optics from authored behavior and incomplete parity. These provide
ideas and fidelity methods, not a same-quality speed result on this TV. Other projects' phone
FPS or CPU-recording figures cannot establish GPU execution or vertical input response here.
No replacement dependency or externally fetched shader was incorporated.

The recommendation is **do not maintain or enable this patch in production**. The candidate's
p95 win does not compensate for its repeatable p99/overrun regression. All prototype Java,
bytecode-adapter/plugin and patch-only unit code has been removed; private immutable APKs,
source and raw evidence remain available. The production digest returns exactly to retained
B2, with no authored-visual, quality, motion, source, focus or persistence change.

The worst application frames in all three initial baseline/candidate traces occur around key
2 (entering the scrolled catalog) and key 11 (return to header); candidate long frames extend
into key 3. This aligns with the existing frost/veil source transition. Refiltering a larger
source during those transitions is a supported next hypothesis, not a directly measured GPU
stage. A future experiment would need reusable filtering of stable wallpaper components while
preserving their live composite and every optical dependency. Simply retaining commands or
swapping to another liquid-glass library does not establish that behavior.

The <1% deadline target remains unmet; the event-to-first-changed-presented-frame p95 <50 ms
target remains unverified. Even glass OFF retains significant full-page cost. Do not describe
this result as smoothness, an accepted partial implementation or an Apple-equivalent renderer.


## Continuous vertical return — 23 October 2026

At the user's request, this changes vertical motion policy rather than the renderer. Held
D-pad repeats previously selected immediate `scrollToItem` for user-directed page returns.
They now retain animation. The final hero/header return uses the measured context-row height,
row spacing and current offset for a 420 ms ease; an interrupted return resumes from its
current position. Offscreen fallback still uses Foundation's item navigation. Normal vertical
row reveal uses a critically damped stiffness-180 spring. A new direction cancels the existing
return through HomeFocusState's existing job ownership. Initial restoration stays immediate;
the reduced-motion reveal policy and explicit immediate paths remain unchanged. The policy
is recreated when reduced motion changes. The horizontal row keeps its existing local spec.

No glass, wallpaper, artwork, clipping, focus bounds, image-cache or quality setting changes.
This is animation continuity work, not evidence that the unresolved frame/input budgets pass.
No horizontal benchmark or additional glass candidate was tested.

Production source: `b7b25b2486e28db11db65a3be1bb7ce83cb21f0fd8732581f15c5c30e743553e`.
Optimized APK: `84f1491ad4754ba65af7834580ea5750e65da14543b64b03c20e28293f570481`.
Debug APK for final native checks: `f6db2ba580210e0bb34828ff8c9ca8dc71c7fb38200db86ecbb685fb39192edf`.
Test APK: `ee025042cd28eddc4a7ef5195618af5d989a22c736136c57f5a528dea644f527`.

Required offline Spotless, architecture, module tests (66 distinct cases), lint, Debug/optimized
and native/benchmark compilation pass; avatar validation passes. The initial motion candidate
passes 14 focused physical-TV vertical/focus/lifecycle cases. After preserving the original
reduced-motion reveal policy, the final source passes four overlapping physical-TV cases:
normal/held-Up intermediate positions, Down cancellation, reduced-motion held traversal and
original dock/header endpoints. Do not sum these runs or treat controlled-clock animation
checks as frame-rate measurements. Raw results, source snapshots and APKs remain ignored in
`.private/vertical-motion-20261023/`.

Configured DataStore/wallpaper bytes match before and after both native runs. The exact
optimized APK is replacement-installed, pulled back and hashed, and explicitly started.
Default Google TV HOME remains unchanged; no system setting or accessibility service is changed.
The original Git index remains byte-identical. Physical-remote feedback rejected this first motion build: rapid Up/Down still jumped or
looked wrong. Its focused passing tests did not cover a fully offscreen continuation reveal.
No general smoothness or latency certification is asserted.


## Rapid vertical reversal ownership — 23 October 2026

The user rejected the first animation-policy update because rapid Up/Down still jumped.
A pending header/dock-to-continuation reveal could outlive the opposite key press, then
request its old content focus. Header Down also cleared the saved control identity before
any card actually received focus. The opposite Up therefore had no authoritative header
return to resume during that pending reveal.

Home now cancels the existing restoration scroll and deferred focus request on a vertical
direction reversal, before dispatching the new direction. Header identity remains until
a card actually receives focus. Up while a header-to-content reveal is pending can return
the current viewport toward the top without completing the obsolete content request.
The prior animation policy, glass, image quality and layout are otherwise retained.

The first regression setup left the continuation lazy item partly visible, bypassed the
pending reveal and passed on the old build. The strengthened precondition puts that entire
item offscreen and reverses Up 64 animation-clock milliseconds after header Down. It fails
on the previous debug APK (`f6db2b…92edf`) at the final header-focus assertion, then passes
on the fixed build. This establishes the stale-focus race, not every possible visible jump.

Seven focused physical API 34 cases pass on the final source: the offscreen reversal,
held-Up intermediate animation/Down interruption, ordinary return animation, reduced-motion
return endpoints/held traversal, covered-Home content identity and repeated Settings return.
Required offline formatting, architecture, module tests, lint and optimized/native/benchmark
compilation pass. The strengthened test has a subsequent formatting/lint pass. No new timing
benchmark is run and frame/input budgets remain unverified or unmet as documented above.

Source: `31ad2a5ae3534b83037cbc9068f6c44a20cd4b3b5ddbb4b9cdfa44c0e49228da`.
Optimized APK: `be56446616f9c5fed0cfcd2be79372d8a5b04b5bcee6885ba38528c0ba42bf37`.
Debug APK: `af13c0be2d1cb502cf7854941b57ae304623e2ad3b38596d9d75f9b7aeaac644`.
Test APK: `868b23b259b945820920557d413cde5bd32f38cd1a9c2dacea830bdfa421b394`.

Configured DataStore/wallpaper bytes match before/after the seven cases. The exact optimized
APK is replacement-installed, pulled back and hashed, and explicitly started. Default HOME
and the original Git index remain unchanged. Raw evidence is ignored in
`.private/vertical-reversal-20261023/`. A further physical-remote reversal check is pending;
functional regression passes do not supersede the user's visible acceptance.


## Search comparison and Economy page motion — 23 October 2026

The user clarified that fast scrolling jitters in both directions and that Search feels smooth.
Search has a stationary retained optical backdrop with simple live result rows; Home has
large retained artwork, per-card effects and moving dock glass. Search's 240 dp/0 dp preparation
window is smaller than Home's 320 dp/0 dp; copying its size is not an evidenced fix. Freezing
Search's exact stationary glass is not equivalent to freezing a moving refractive dock.

[Android TV guidance](https://developer.android.com/training/tv/playback/compose/lists)
recommends standard Foundation lazy layouts and focus-driven reveal, already used here.
[Android's cache-window discussion](https://android-developers.googleblog.com/2025/11/deeper-performance-considerations.html)
and [Apple's collection-view guidance](https://developer.apple.com/videos/play/wwdc2021/10252/)
emphasize moving cell/image preparation ahead of scrolling and reusing prepared cells. These
public references do not disclose the private Google TV or Apple TV Home renderer, nor prove
performance on this GPU. The pinned Coil engine already calls prepareToDraw for decoded bitmap
images; no duplicate upload-warming path was added.

The current configured archive is **Economy**, unlike the original Balanced/glass-ON work.
That tier disables glass effects and had also been included in Home's page-reduction flag,
so the earlier page-animation tuning was bypassed. System animator scale reads unset, which
LauncherRoot treats as the normal default 1; the profile's reduced-motion setting is false.
No setting was written. Prior normal-quality fixtures missed this separate Economy branch.

Home now separates page motion from effects quality: Economy retains its existing solid-panel
and static-card treatment, while the page uses the soft vertical reveal and 420 ms return.
Explicit/system reduced motion and covered Home still take the existing reduced path. Search
already lets Foundation animate result scrolling independently of its reduced-effects policy.
The Economy-specific native test fails on the previous build and passes after this change.
Six focused physical-TV motion/reversal/lifecycle cases pass; required offline gates pass.

Source: `655e548c453adc95206b32602ded2687f179ca77d958feedc56fdd84363d0636`.
Optimized APK: `429d8589f2df648a5e30f9df9d6a0dbd3fd98cc815744c40bc843c2844321f64`.
Debug APK: `3061440e364c7c9760c98f40d9012075235e1daedb33a328d86be70c37074938`.
Test APK: `a7d5a92e8d17d02a04e6ae3cffa947a17997a0e6935d24a34b07fcb8e56bed55`.

The user confirmed the bottom-to-top improvement on this optimized build, but still reports
a hitch when moving down to the card grid. This is partial qualitative acceptance, not a
frame-rate or input-latency certification. The original stage/index, configured Economy
preferences and default HOME are preserved.

An isolated native-Auto artwork experiment was rejected and removed. On the same rapid
Economy scene, three measured 36-key iterations per build at 180 ms device cadence have matching
actual keys, destinations, visible content and valid traces. CPU-frame p50/p95/p99 regress from
22.84/32.15/42.10 ms to 31.01/47.53/56.10 ms; app misses regress from 78/825 to 151/748. These
are before the Economy page-motion correction and must not be compared with the older 500 ms
Balanced/glass-ON results or attributed to the final motion fix. The original Offscreen artwork
path remains. No additional equivalent confirmation is justified for this clear regression.
Raw evidence stays ignored in `.private/search-home-20261023/`.

A phase diagnostic in the old rapid Economy trace puts about 170 ms of main-thread running in
a 190 ms first-grid key interval, versus roughly 16–36 ms in the first three upward row intervals.
It also sees six new artwork recordings on first-grid Down and urgent lazy preparation, with
a maximum 25.34 ms measure/layout slice. These are overlapping phase observations, not isolated
GPU-stage or event-to-first-pixel timings. They motivate the separate bounded recent-row
retention experiment; the accepted return motion is preserved.

## Bounded row retention and grid entry — 23 October 2026

The user's accepted Economy page animation improved bottom-to-top travel, but Down into the
catalog still hitched. A separate experiment retains 320 dp behind Home's viewport, alongside
the existing 320 dp ahead window. This keeps a bounded set of recently visited rows prepared
when returning to the header. It changes no focus route, scroll timing, artwork compositing,
request size, Coil limit, selected quality or material appearance. This supersedes the earlier
zero-behind Home policy; Search's independent policy is unchanged.

Baseline source: `655e548c453adc95206b32602ded2687f179ca77d958feedc56fdd84363d0636`.
Retention source: `719dbaabe15946fad9f7c4d3adccde1653c3cdc35d8feca303cdaa5735ae5c1c`.
Baseline optimized APK: `429d8589f2df648a5e30f9df9d6a0dbd3fd98cc815744c40bc843c2844321f64`.
Retention optimized APK: `51175278a5777fb97226edd327610afcae0078b614510495464440f6065f5e43`.
The underlying commit is `fc2b6a9df4ef2350bb624442e27785ef8dd96bb5`; source digests identify
production files including the retained working-tree edits, rather than claiming a clean HEAD.

Physical API 34/Mali-G52, Skia OpenGL, approximately 60 Hz; the app surface is 1920×1080.
The current Economy profile is preserved. Its solid-panel policy produces no HazeGlass stage
records, so these results are not a new Balanced/glass-ON measurement. Each replacement-installed
optimized APK is pulled back and hash-verified. The fixed benchmark APK runs
`LauncherNavigationBaseline.fullPageRapidWarm` with required packaged baseline profiles,
`CompilationMode.Partial(BaselineProfileMode.Require, warmupIterations = 3)` and three measured
iterations per block. Each iteration restarts the process, selects the same real hero/header,
primes a full tour, settles at the header, then delivers three six-Down/six-Up tours at an
absolute 180 ms device cadence. OS, image, disk and driver caches are retained. No cache reset,
full-AOT-only claim, horizontal benchmark or system-setting write is involved.

The predeclared A1/B1/B2/A2 confirmation matches actual delivered events, all focus destinations
and visible scene/bounds across all twelve iterations; all traces pass quality validation.
Primary grid entry is key 2 of the first measured tour after the settled header. Later tours
can interrupt the 420 ms return and are reported separately in raw phase summaries.

| Block | Grid-entry main running median (ms) | Grid-entry overrun p95 (ms) | Grid app misses/frames | Whole-burst CPU-frame p50/p95/p99 (ms) | Whole-burst app misses/frames |
| --- | ---: | ---: | ---: | --- | ---: |
| A1 | 168.50 | 42.36 | 7/28 | 22.81 / 31.01 / 36.85 | 67/878 |
| B1 | 76.09 | 13.98 | 3/33 | 22.43 / 27.32 / 31.29 | 65/868 |
| B2 | 78.33 | 23.02 | 4/33 | 22.48 / 27.31 / 29.73 | 64/865 |
| A2 | 162.27 | 35.58 | 6/28 | 22.73 / 27.49 / 34.24 | 63/882 |

The localized main-work and overrun improvement repeats in both orders. Whole-burst p95
variation is material: A2 is close to B, so the strongest claim is grid-entry preparation,
not a universal 31-to-27 ms gain. Whole-burst application deadline fractions are effectively
unchanged at about 7.4%; the less-than-1% target remains unmet. Both builds record 18 artwork
command captures in the primary grid-entry windows and 198 across the full bursts. These
counts do not demonstrate cached GPU pixels or isolated GPU savings. Main running, inclusive
layout slices, CPU-frame intervals and presentation overruns are separate measurements;
input-to-first-changed-presented-frame p95 remains unverified.

The physical-remote user reports “perfect downward now,” with a small visual freeze near the
dock on upward return. This accepts the observed Down improvement qualitatively, not the
unresolved frame/input budgets or the remaining dock transition. A separate upward diagnostic
finds three large artwork command captures near dock arrival, about 72–78 ms main running
in that key interval and another 67–87 ms in the next interval. Preparation ahead of the dock
is a hypothesis for a separately screened candidate, not a proved GPU-upload attribution.

Raw APKs, source snapshots, protocols, runner output, match checks and Perfetto traces remain
ignored in `.private/grid-reveal-20261023/`. The required offline gates pass for the retention
source. The native, memory and deployment verification for this supported fix is recorded below.

An isolated larger ahead window (640 dp ahead/320 dp behind) was rejected and removed.
Source `d60a0bb4fff8da01993a57b202801f6fc4c4758a0f590c07615c8785e248a629`, optimized APK
`b08b24aaacae28028632f2f3dec6d8c8711d97f7d25e81c4d2e61505f7999a3b` matched B1/B2's
actual vertical inputs, focus walks and visible scene. The first-tour dock pair still recorded
nine large artwork command captures across three iterations. Main running median stayed
76.18 ms versus 77.10/76.50 ms, while dock overrun p95 worsened to 39.50 ms versus 23.15/23.17 ms.
Whole-Up app misses were 13/108 versus 11/113 and 11/112; whole-burst CPU p95 rose to
31.38 ms versus 27.32/27.31 ms. Local checks passed, but this single matched screen provides
no reason to retain or repeat the behavior. More precomposition space did not prove early
artwork rasterization. Its temporary source change is removed; the immutable rejected APK
and evidence stay private.

A second isolated screen (320 dp ahead/960 dp behind) also failed and was removed. Source
`0b944a83ab04882208ea964889afff48f5a2c75497408225f73b76a31dfba252`, optimized APK
`d6dc1ed482e20e80fe17894ffbc6c8f1c88769427ddfeb13002e18bec0294334`, matched the same
actual keys, focus walks and visible scenes. It retained nine dock-phase artwork command
captures, with 77.49 ms main-running median and 39.49 ms overrun p95; whole-Up misses were
13/111 and whole-burst CPU p95/p99 were 29.84/32.47 ms. Thus neither larger window demonstrated
the hypothesized avoidance of dock artwork preparation. The source returns exactly to the
supported 320/320 dp policy. No larger cache is kept to mask an unresolved transition.


### Retention verification

The final retention source passes required offline Spotless, architecture, module tests,
Debug lint, optimized/native/benchmark compilation and the eight-master avatar check. Nine
focused physical-TV cases pass: Economy intermediate page motion, fully offscreen reversal,
held-Up/Down interruption, original header/dock endpoint, held-Up continuation, repeated
Settings return, and three artwork-request/prefetch cases. Debug APK:
`b4599350579bf4249699e4419c0d9af347b2c00b726217250fda3d7dde0eaf00`; test APK:
`a7d5a92e8d17d02a04e6ae3cffa947a17997a0e6935d24a34b07fcb8e56bed55`.
Configured DataStore/wallpaper bytes match before and after these cases. The fixed benchmark
APK hash is `63b8396e0f50580d58f7d076dbb5b2e566499fed94b8077bca3528e00df852ef`.

Memory diagnostics use the two immutable optimized APKs separately from frame timing. Both
receive the same speed-profile command (memory-only diagnostic), one priming tour and five
six-Down/six-Up stress tours with host-driven 500 ms pauses plus ADB/query overhead. Twenty-three
baseline and twenty-two retention samples have about 2.4 s median spacing. After five seconds
settled, the unchanged default HOME backgrounds Oree for twelve seconds, then Oree is explicitly
resumed. These are short sampled trends, not unsampled peak, driver-memory or 10/30-minute
leak certification. PSS and HWUI cache are separate accounting and must not be added together.

| Measurement | Baseline | Retention |
| --- | ---: | ---: |
| Sampled maximum PSS (KiB) | 71,630 | 70,652 |
| Settled PSS (KiB) | 61,548 | 63,915 |
| Settled HWUI GPU cache (bytes) | 46,160,368 | 46,075,420 |
| Settled scratch targets | 76 | 76 |
| Background PSS (KiB) | 57,160 | 59,586 |
| Background HWUI GPU cache (bytes) | 5,648,004 | 5,648,004 |
| Resumed PSS (KiB) | 64,552 | 65,578 |

The last three cycle cache values remain around 46.1 MB in each build rather than increasing
per tour. The retained source therefore shows a small settled PSS increase without a growing
render-target count in this screen. The existing 12 MiB Coil limit and total memory budgets
remain unchanged. Personal endpoint captures remain private and are reviewed separately from
functional and timing metrics. The supported optimized APK is pulled back, hash-verified and
explicitly opened after the native suite; default Google TV HOME is unchanged.

A separate screenrecord diagnostic of the supported build uses an encoded 1920×1080 output
without changing system display settings. It is sparse and perturbs rendering, so it cannot
certify dropped-frame timing or input-to-first-visible latency. It motivates checking the
handoff between automatic upward reveal and the explicit header return, rather than increasing
cache windows again. The latter always starts its 420 ms easing with zero velocity, even when
it takes over an upward-moving page.

### Initial upward handoff — visual improvement, rejected frame-time tradeoff

The current candidate retains the supported 320/320 dp window and changes only the explicit
420 ms header-return handoff. A presentation-local observer samples existing grid displacement
while scrolling; it has no idle frame loop. A recent same-direction velocity selects a monotone
Hermite easing whose starting tangent carries that motion and whose ending tangent is zero.
A 60 ms freshness bound, direction-reset and bounded starting slope reject stale/reversed speed
and prevent overshoot. Stationary returns retain the original easing. There is no change to
Down reveal, focus destinations, duration, materials, artwork or selected quality. A trace-only
`TVL:pageReturnHandoff` marker distinguishes actual use of the new branch for subsequent tests.

Source: `dd4f7a6ddb9f110d606de77f46ff1fcf268358ca8b962737df61b9e819501b08`.
Optimized APK: `794d0520c205e61f67f7a7be6f24af9e2fc0a5c036290a513ce2bed490f43f83`.
Debug APK: `bb8b21cfa067f5621711397e3ff5f2ca6856857d23fa7c38a53b60d27dba5747`.
Test APK remains `a7d5a92e8d17d02a04e6ae3cffa947a17997a0e6935d24a34b07fcb8e56bed55`.
Three new unit cases check initial/ending velocity, monotonic bounds under fast handoffs, and
stale/opposite/reset fallbacks. Required offline gates and the same nine focused physical-TV
cases pass. Configured DataStore/wallpaper bytes match before/after. The exact optimized APK
is pulled back, hash-verified and explicitly opened with default HOME preserved. The original
Git index remains byte-identical. The user reports “it's better” on this exact optimized APK,
with the confirmed downward fix preserved. This is qualitative improvement, not evidence that
every hitch is gone. The physical check ran without other device work. A matched optimized
timing confirmation follows below; no frame/input-budget success is asserted.


The material configuration changed between the private post-native archive (Economy) and the
post-remote-check archive (Balanced, normal motion, liquid glass enabled, intensity 1, hero
above the dock). The first handoff trace has live HazeGlass work in all three iterations, so
comparing it with the earlier Economy B1/B2 is invalid. The old screen stopped at its regression
guard; that cross-quality result is excluded from causal conclusions. A read-only configuration
archive obtained through a data-preserving debug replacement confirms the current selection,
and the optimized APK is restored. The app is never switched back to Economy for a favorable
measurement.

A fresh current-quality retention control and another initial-handoff block follow sequentially.
All three blocks match actual 36-key event/focus walks and visible scene/bounds, have valid
traces and live HazeGlass stages. The current-quality archive precedes the fresh control and
repeat; the first block's live-glass state is observed in its trace and archived afterward.
Intermediate trajectory intentionally differs; these are motion-regression comparisons, not
an identical-pixel GPU workload or isolated shader experiment.

| Current Balanced/glass-ON block | CPU-frame p50/p95/p99 (ms) | App misses/frames |
| --- | --- | ---: |
| Initial handoff E1 | 33.32 / 121.85 / 145.11 | 302/700 |
| Retention control B3 | 33.64 / 98.24 / 140.95 | 313/738 |
| Initial handoff E2 | 33.03 / 120.71 / 145.30 | 298/716 |

`TVL:pageReturnHandoff` occurs three times in each measured candidate iteration, confirming
that the new branch is used. Artwork command captures remain 198 per block. The user reports
better motion, but both candidate p95 values exceed the fresh control by more than the
predeclared 10% regression bound. This is a frame-interval tradeoff, not proof of greater
isolated GPU shader time; total CPU running and deadline fractions are different metrics.
The aggressive maximum starting slope of 3 is therefore rejected as the final policy, despite
its qualitative benefit. The candidate memory/finalization driver stops before accepting it.
The existing missed-deadline and first-visible-response targets remain unmet/unverified.

The next isolated motion screen keeps the same observer, 420 ms duration, endpoints and
renderer, but caps the carried starting slope at 0.75: at most 75% of the average return speed.
This retains prompt nonzero starting motion without inheriting the much faster short-row
reveal speed. Stationary/reduced/Down behavior stays unchanged. This is a hypothesis until
matched current-quality frame timing and a separate remote check support it.

Gentler candidate source: `780e328d5ba028ec524050f53ee0e538e8a79f7b9f1bb483628503e0ff70b91d`.
Optimized APK: `a9403ed0fd9720522a0568c0c8a61219f1c11714cb5275e2d2bf2083aef8e3b9`.
Debug APK: `12f9611a3f7933392666d322b99672976bdda2f66871be4da7cc552477944ed2`; test APK: `a7d5a92e8d17d02a04e6ae3cffa947a17997a0e6935d24a34b07fcb8e56bed55`.
Required offline gates pass; physical acceptance and final regression confirmation are pending.

The gentler candidate's first screen F1 matches the fresh Balanced/glass-ON B3 control's
actual 36-key focus walks and visible scenes in all three iterations; traces pass validation
and contain live glass stages. CPU-frame p50/p95/p99 are 32.70/94.21/133.57 ms versus
33.64/98.24/140.95 ms. App misses are 314/756 versus 313/738. It passes the predeclared
no-material-regression screen, while the approximately 42% missed-deadline fraction still
fails the <1% target. Per-iteration overrun p95 is 155.73/155.49/159.00 ms versus the control's
153.30/190.29/154.33 ms; this variation does not support claiming a small universal speed gain.
Mapped display gaps above 25 ms are 316/756 intervals versus 315/738. Neither those gaps nor
CPU-frame intervals establish input-to-first-visible response or isolated GPU shader duration.
The new branch executes in every measured return; artwork detail, input counts and effects
remain intact. The evidence supports a conservative motion-regression screen, with physical
acceptance and independent confirmation still pending.


The gentler source subsequently passes the same nine focused physical-TV cases. Its current
Balanced DataStore/wallpaper bytes match both before/after native instrumentation and the
archive preceding the fresh full-glass control. Its optimized APK is pulled back and hashed,
explicitly opened, and the original Git index/default HOME are preserved. Endpoint captures
for the supported retention build show unchanged artwork, panel geometry and header restoration;
current gentler-motion remote feedback and independent timing confirmation remain pending.
No device work continues during that remote check.


## Separate sharp/frost filtering — 23 October 2026

The retained implementation prepares separate stationary sharp and frosted wallpaper filters
for clear glass, blends their outputs with the existing depth/veil alpha, and leaves refraction,
lighting and material coordinates live. The public Haze 2.0.1 APIs, authored kernel and Balanced
sampling are unchanged. No dependency patch or disk bitmap cache is introduced; shared image-cache and decode limits
are unchanged. The original source remains selected during wallpaper replacement, for alpha
images, roots above 2,097,152 pixels, other sampling modes and non-clear/progressive materials.
The prepared inputs share the visible wallpaper transition and withdraw on source disposal.

### Physical comparison and identity

Physical Smart TV Pro, API 34, 1920×1080 app rendering at the existing approximately 60 Hz
cadence; no system display/power/HOME/accessibility setting changed. The same personal
wallpaper, Balanced profile, full motion and glass intensity 1 are retained. The saved blur
value was initially 0: that diagnostic and an aborted settings preflight are excluded from
inference. Enabled comparisons use blur 1, selected through the app slider. Raw APKs, source
patches, configuration backup, failed/excluded runs, captures and traces remain private.

| Identity | SHA256 / revision |
| --- | --- |
| Baseline checkout | `d07dc0cc927e2b3686d254677633ebc12ae41c4a` |
| Baseline optimized APK | `5e2dd9b8f6a50d49bf8233851ddab49a6d2a3a9e67667d1c30c901f792e3191c` |
| Final production-source fingerprint | `c5b84da927ec1b5b329b165d32bd367dc876b219ebf0a8e97181b1aab235ff6c` |
| Final optimized APK | `30c79a19eea64314ea9dadb5d5feaa7d826bfe18396212e304f493b79e13d033` |

All timing uses optimized R8/resource-shrunk APKs, cached/offline dependencies, JDK 21 and
Partial/Require with three compilation warmups and three measured iterations per method.
Rapid warm navigation primes a tour, then measures three six-Down/six-Up tours at the existing
180 ms device cadence. First traversal restarts the process and measures one tour at 500 ms;
OS/file/image/driver caches remain, so this is not first-install or cold-cache evidence.
There are no memory queries, hierarchy dumps or recordings inside timed bursts.

The initial prototype reproduces its rapid p95 gain in A1/B1/B2/A2 order: baseline 85.54/88.47 ms,
prototype 69.76/71.37 ms; prototype p99 107.41/107.99 ms versus baseline 124.41/127.94 ms.
The final source adds shared-transition gating and existing intensity normalization. Its own
fresh block follows the return baseline A2; do not pool prototype iterations as final-source
measurements. Every accepted iteration matches delivered events, logical focus destinations,
visible text/bounds and trace-quality prerequisites. The prepared-input marker is present in
all final iterations. Source/installed APK hashes are checked after replacement installs.

| Final comparison, three iterations each | CPU-frame p50 / p95 / p99 (ms) | App deadline misses |
| --- | --- | --- |
| Baseline A2, rapid warm | 33.07 / 88.47 / 127.94 | 306/770 (39.74%) |
| Final C, rapid warm | 33.39 / 69.63 / 106.97 | 315/879 (35.84%) |
| Baseline A2, first traversal | 11.34 / 80.78 / 121.36 | 208/746 (27.88%) |
| Final C, first traversal | 13.08 / 67.97 / 102.51 | 198/805 (24.60%) |

Final rapid p95 falls 21.3% and first-traversal p95 15.9%; tails improve as well. Medians do not
improve, and rapid absolute misses are not lower: more app frames are produced. CPU-frame
intervals include pipeline blocking and are neither pure CPU/GPU execution nor input latency.
The <1% deadline target remains unmet and event-to-first-changed-presented-pixel p95 <50 ms
remains unverified. This is a partial physical-TV improvement, not smoothness certification,
independent final-source ABBA replication, hardware-wide compatibility or release acceptance.

### Sampled appearance and resources

The exact final debug/test APKs pass five native rendering cases in both control and prepared
modes. The 21 controlled-clock frame pairs cover movement, reversal, tilt, wallpaper replacement,
resizing, stationary frost and reattachment. Maximum whole-frame mean RGB difference is
0.108 levels/255, maximum channel difference 9, and at most 0.213% of pixels differ by more than
3 levels. The wallpaper-replacement midpoint is pixel-identical because it uses the original
source until its shared transition settles. Other sampled frames are very close, not identical.
Both synthetic frames and actual optimized Home were visually inspected. An empty dock-material
region over the same wallpaper differs by at most 2 channel levels (mean 0.226/255); this excludes
dynamic cards and the clock. These inspections do not establish all-scene fidelity or latency.

Resource work is separate: one priming tour and five repeated vertical tours per APK, identical
packaged speed-profile compilation, host-driven 500 ms pauses plus ADB/query overhead, followed
by settle, background and resume. Sampled maximum PSS is 69,834/68,961 KiB (baseline/final),
settled PSS 66,705/63,595 KiB. Do not attribute this small PSS difference to the cache. Settled HWUI
GPU cache increases from 71.81 to 85.54 MiB, about 13.74 MiB, and stays bounded across the sampled
cycles; background values fall to 5.13/2.16 MiB. Resumed final rendering was inspected. PSS and
HWUI cache are separate accounting, not total driver memory or unsampled peaks. This short
screen does not replace 10/30-minute memory, idle CPU, startup or Home-usable acceptance.
Existing performance budgets, shared 12 MiB Coil cache and decode limits remain unchanged.

Required formatting, architecture, module tests, Debug lint, Debug/optimized APK compilation,
instrumentation/benchmark compilation and avatar validation pass. Spotless also corrected
existing indentation in HomeContinuationRow; that file's diff is formatting only.

Nine additional native vertical-navigation, Settings-return, lifecycle and artwork cases pass,
and the alpha-source fixture passes with prepared filtering enabled but ineligible. Configured
DataStore and wallpaper bytes match the task-start archive after restoring the app blur slider
to its original 0. The exact tested optimized APK is replacement-installed, pulled back and
hash-verified, then explicitly started. Default HOME is unchanged. No public push or release
was performed. Raw preservation and deployment evidence remains outside tracked source.

## Incremental GPU work review — veil compositing

The next change removes only the wallpaper veil's intermediate group-alpha surface.
Both the visible wallpaper and prepared glass source keep their existing black rectangle
and animated opacity, but use `CompositingStrategy.ModulateAlpha`. These leaf layers contain
one rectangle, no children and no RenderEffect. Applying layer opacity directly to that
draw has the same source-over equation as applying opacity to its isolated group:
`effectiveAlpha = 0.25 * diffusion`. Rendering may still differ by intermediate-buffer
rounding; pixel identity has not been established for this change.

Compose's [graphics modifier documentation](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers#modulatealpha)
explains that this strategy avoids the offscreen buffer otherwise used for fractional
layer alpha, provided no RenderEffect is set. Its overlapping-content caveat is why the
strategy is restricted to the veil leaves. The sharp/frost filters and image/gradient groups
retain their existing compositing. The full-screen rectangle still draws; this removes an
intermediate target and its composition, not all veil bandwidth or all wallpaper passes.
The opportunity occurs at intermediate diffusion values; settled opacity zero/one already
avoids the default fractional-alpha buffer. No measured frame-time or memory gain is claimed.

### Research and existing implementation

Review the exact pinned Haze 2.0.1 sources rather than assuming an unoptimized physical
renderer. Its [glass shaders](https://github.com/chrisbanes/haze/blob/2.0.1/haze-glass/src/commonMain/kotlin/dev/chrisbanes/haze/glass/GlassShaders.kt)
already use rounded-rectangle signed distances, analytical displacement and a Fresnel-like
ambient term. The exponent-three path uses multiplication. Its
[Android backend](https://github.com/chrisbanes/haze/blob/2.0.1/haze-glass/src/androidMain/kotlin/dev/chrisbanes/haze/glass/GlassRenderEffectFactory.android.kt)
supports the [fused renderer](https://github.com/chrisbanes/haze/blob/2.0.1/haze-glass/src/commonMain/kotlin/dev/chrisbanes/haze/glass/RuntimeShaderGlassDelegate.kt),
which combines optical operations rather than requiring a separate pass for each authored
property. Source inspection establishes available paths, not physical GPU pass counts.

The remaining directions have different costs and fidelity constraints:

| Direction | Assessment for the current renderer |
|---|---|
| Dual-Kawase / dual filtering | [Arm's bandwidth-efficient rendering work](https://community.arm.com/cfs-file/__key/communityserver-blogs-components-weblogfiles/00-00-00-20-66/siggraph2015_2D00_mmg_2D00_marius_2D00_notes.pdf) uses downsample/upsample chains for broad blur. Oree already prepares broad diffusion once at 192×108. The remaining clear-glass filter is only 1.25 dp times intensity; replacing it with a pyramid adds intermediates and changes the kernel. Keep the current kernel for this increment. |
| Schlick Fresnel | [Filament](https://google.github.io/filament/main/filament.html#materialsystem/specularbrdf/fresnel(specularf)) describes the inexpensive fifth-power approximation. Haze's authored ambient response is a different visual curve; replacing it is a material change, not an algebraically identical simplification. |
| Signed-distance fields | Already used by Haze for silhouette and optical geometry. Retain the existing analytical path and full-resolution output shape. |
| Optical lookup tables | A table trades arithmetic for texture reads, storage and interpolation. It must reproduce size, corner radii, density and optical parameters and preserve moving source coordinates. There is no isolated shader-cost evidence yet to justify the extra sampling or a fork. |
| Half/quarter-resolution diffusion | The broad frost bitmap is already smaller than quarter resolution. The root-sized prepared filtered layers are still a bandwidth/cache opportunity. Haze Balanced is a quality policy, not a promised half-resolution buffer. Reducing prepared-layer resolution also changes gradient filtering and shallow detail; do not silently lower the existing policy. |
| Dock-region caching | A crop must include the union of moving sample bounds, refraction displacement, filter support and other clear-glass consumers. It must retain wallpaper-global coordinates and refresh on source, size, density or optical changes. Cropping to only the resting dock rectangle risks stale/missing samples during scroll and Overview transforms. |
| Pass fusion and reuse | Haze already retains resources and offers fused optics. The application retains separate stationary sharp/frost inputs; diffusion changes their blend. Remove the simple redundant veil intermediate first, keeping all optical parameters and sampling unchanged. |

The source-over argument is sufficient to scope this leaf-only change, not to certify its
appearance on every renderer. Existing wallpaper intermediate-frame, replacement and prepared
source fixtures remain the relevant correctness cases. No additional TV benchmark campaign,
replacement install or device interaction is part of this increment. Previous physical timings,
memory measurements and native-window comparisons belong to the previous APK; they must not
be attributed to this source revision. Build gates are recorded separately from device
acceptance, and all existing performance budgets remain in force.

Local verification passed: Spotless, `verifyArchitecture`, module tests, Debug lint,
Debug and optimized APK builds, instrumentation/benchmark compilation, avatar validation
and `git diff --check`. No new device fidelity or performance result is claimed.

## Precomputed optical renderer — manual validation build

Eligible clear controls can now use a compact, geometry-only two-dimensional optical map
through Haze's public custom-effect API. Rounded corners and edge bands retain physical
coordinates in a compressed nine-patch field; the neutral interior collapses. A 65-sample
Snell/Schlick profile drives precomputed inward displacement; fixed directional rim lighting
is baked into the same raw RGBA buffer. Map dimensions are capped at 256 per axis (256 KiB),
with half-resolution edge sampling. Generation runs on Default, is geometry-keyed and
cancellable, and does not run during scrolling or source-pixel changes. The existing split
sharp/frost background preparation remains intact.

The shader uses one linearly interpolated numeric map read and one live backdrop read,
without per-pixel SDF, normal reconstruction, refraction equations or specular powers. The
renderer owns one padded full-resolution source GraphicsLayer; source commands remain live
for movement, source replacement and depth blending. Compared with Haze Balanced, this
full-resolution capture can cost more input bandwidth; fewer optical operations/layers are
an expected tradeoff, not measured acceleration. Full-resolution Compose clipping preserves
the silhouette, while the baked light and shallow lens profile intentionally change the look.

Haze remains selected while the map is pending, during wallpaper crossfades, for unsupported
geometry/materials/accessibility overrides and after map/shader failures. The failure frame
keeps current input pixels before recomposition restores Haze. LocalPrecomputedGlass disables
the new path for comparisons. No Haze fork, additional dependency, persisted preference or
physical-TV benchmark was introduced. The user requested optimized compilation and ADB
deployment only, overriding the planned local/emulator checks; no tests or emulator were run.
Visual and performance acceptance belong to the user's manual validation of this APK.

References reviewed: [sohumsuthar](https://github.com/sohumsuthar/liquid-glass),
[Meapri](https://github.com/Meapri/liquid-glass-web),
[LeonardSEO](https://github.com/LeonardSEO/liquid-glass-react), and pinned Haze 2.0.1 shaders,
fused rendering, input-scale policy and public HazeEffectRenderer/DrawScope APIs. The native
Android implementation is authored for Oree rather than embedding the web renderers.

### Physical verification after the manual build

The user subsequently authorized physical-TV testing; no emulator was used. On the physical
API 34 TV, the mapped opaque fixture, all four WallpaperBackgroundTest cases and HazeGlassTest
pass (six cases). The same movement/source/resize fixture passes again with maps disabled,
and with translucent wallpaper falling back to Haze. Three real Settings visits restore Home
and native header focus in the lifecycle case. These are nine test executions across seven
distinct methods; overlapping fixture variants must not be counted as distinct cases.

The bounded functional trace records 19 `Oree:mapped-glass-optics` sections and no
`Oree:mapped-glass-fallback` sections, with no reported trace errors/loss. Thus the test
exercised the new renderer rather than only its fallback. Synthetic native-window captures
were reviewed beside the Haze control, including the intentionally different rim and lens.
A frozen resize frame can show the current input before the asynchronous replacement map
is applied; this is not evidence of continuous optical quality during arbitrary resizing.
Shader-failure recovery was not fault-injected. The fallback helper now preserves source
alignment on its temporary frame. No frame-time/GPU-time improvement is inferred from these
traced, screenshot-producing correctness tests.

All required local gates pass, including four new optical-map unit cases, formatting,
architecture, module tests, lint and optimized/instrumentation/benchmark compilation; avatar
validation and diff whitespace checks pass. A mistakenly module-qualified formatter task
was unavailable; the root formatter succeeded. An intermediate fallback-helper scope
qualification failed compilation and was corrected before any test APK was installed.
Configured DataStore/wallpaper files are byte-identical before/after instrumentation. The
optimized APK was restored, pulled back and hash-verified, explicitly started and visually
inspected on Home; default HOME is unchanged. Private source/APK manifests, raw traces,
captures and data archives remain outside tracked source. Final source SHA256 is
`f99105a5b4fc348af37a92b2463918c8391ba2473a03859f0692d68cc2de88c0`;
optimized APK SHA256 is `e1fc1aee6fa9981defa52e356a174c62e94e71ee79b3d02a1d98807e370e725a`.

### Optical renderer timing comparison — 24 October 2026

The new renderer improves the measured rapid vertical scrolling workload on the physical API 34
TV. A is the previous optimized Haze renderer with split backgrounds and veil modulation; B is
the mapped renderer already installed for manual validation. Both use the current Balanced
profile, full motion, intensity .85, background blur .8 and the same wallpaper. No application
code or visual settings changed during the comparison.

The planned A1/B1/B2/A2 order completed with three measured iterations and three Partial/Require
compilation warmups per block. Each measured iteration follows one priming tour with three
six-Down/six-Up tours at a native 180 ms cadence and a 700 ms tail. Processes restart in setup;
OS/file/image/driver caches remain. No emulator, cold-cache run, memory query or screenshot was
used during timed bursts.

The live Twitch entry changed between A1 and B1. A1 is excluded from the primary comparison
rather than pooled as a matched baseline. B1 became the scene reference before the final
baseline ran; B1, B2 and A2 match visible identities/text/bounds, actual delivered events and
focus walks. The retained matched subset is therefore B/B/A, not a fully matched ABBA. Each
B block independently shows the gain relative to the return baseline. The unmatched A1 supports
its direction only (CPU p95 72.37 ms, overrun p95 114.16 ms).

| p95 metric | Haze A2 | Mapped B1+B2 | Reduction |
|---|---:|---:|---:|
| CPU frame duration | 71.72 ms | 39.69 ms | 44.7% |
| Macrobenchmark frame overrun | 114.97 ms | 56.55 ms | 50.8% |
| GPU-inclusive app completion | 101.82 ms | 57.14 ms | 43.9% |
| Actual app start to display presentation | 127.48 ms | 81.92 ms | 35.7% |

The App Deadline Missed classification falls from 313/878 (35.65%) to
135/2315 (5.83%). This is not the proportion of all late-presented frames; buffer queueing remains.
The <1% missed-deadline budget is still unmet. CPU median improves 33.56→26.95 ms; actual-start
to presentation median improves 72.39→65.19 ms. CPU p99 improves 99.55→62.10 ms and overrun p99
173.47→101.06 ms. Candidate block CPU p95 values are 39.40/39.66 ms and overrun p95 values
56.52/56.54 ms, so the repeated candidate result is close. Pooled values use linear-interpolated
quantiles over correlated frames; they are not independent frame-level trials or confidence bounds.

All twelve traces pass the existing quality checker. All six candidate measured traces contain
mapped optical markers (1,338 draws in total) and zero mapped shader-fallback markers. Complete
app frames join uniquely to expected app and completed display frames, with valid predictions
and no presentation-before-completion anomalies. Custom completion/presentation statistics
exclude unfinished app slices. Native app completion includes GPU completion/buffer posting;
presentation uses mapped SurfaceFlinger display end. Neither isolates shader execution or
measures remote-button-to-first-changed-pixel latency. The different baked rim/lens is intentional;
this is the complete shipped renderer comparison, not an identical-pixel optics equivalence test.

Both APKs share the same 34 asset/resource entries; only generated baseline-profile binaries
differ. No GPU bandwidth, energy, first-entry, startup or long-duration memory/thermal claim
follows from this rapid-scrolling result. The first unmatched baseline and the single matched
return baseline limit independent confirmation. No performance budget was relaxed.

Configured files match the pre-run archive byte-for-byte. The exact B optimized APK was
restored, pulled back and hash-verified, then explicitly started; default HOME is unchanged.
A APK SHA256: `d0a9d7454693b79e31acfc92c2705ff1051312165736237aaa769753310656e9`.
B APK SHA256: `e1fc1aee6fa9981defa52e356a174c62e94e71ee79b3d02a1d98807e370e725a`.
Source SHA256: `f99105a5b4fc348af37a92b2463918c8391ba2473a03859f0692d68cc2de88c0`.
Full raw events, traces, provider scene details, manifests and data archives remain private.

## Rendering implementation for user testing — 24 October 2026

The user subsequently authorized implementing the recommended improvements and explicitly
reserved testing for themselves. No Gradle task, architecture gate, test, benchmark,
device command or deployment was run for this revision. Earlier timing/fidelity/resource
results above belong to earlier APKs and do not validate these changes.

- Clear wallpaper-only glass can directly sample two immutable full-size prepared fields,
  avoiding its per-control capture/RenderEffect layer once the fields and optical map are ready.
  The fields capture the existing filtered output, including its gradient and kernel, rather
  than replacing it with an approximate CPU blur. Their captured filter trees are removed
  after both fields are available. Preparation uses the existing graphics-layer snapshot API
  asynchronously, once per identity, not once per scrolling frame.
  Frost recording occurs before opacity is applied, allowing preparation at zero frost
  alpha instead of waiting for the first scrolling depth transition.
- Local-to-source transforms and source-over sharp/frost/veil blending remain live. Separate
  windows, non-affine geometry, preparation failures, unsupported inputs and pending maps use
  the retained mapped/Haze path. LocalDirectWallpaperGlass disables direct preparation/drawing
  for comparison without a stored preference. New direct draws carry
  `Oree:direct-wallpaper-glass` markers. Moving the source outside its original bounds remains
  transparent rather than clamping arbitrary edge pixels.
- The current source-size guard bounds each field to 2,097,152 pixels. Two nominal RGBA8 fields
  can account for up to 16 MiB before driver bookkeeping. They replace filter trees after
  preparation, but snapshot generation can transiently overlap those trees. Actual total/peak
  memory and driver residency are unmeasured. Lifecycle stop/trim drops prepared references;
  a trim suppresses recapture until the next STARTED entry.
- Both optical paths skip map sampling and rim math in the guaranteed neutral interior,
  allowing for rounded corners and a texel of filtering support. Shape edges and existing
  optical-map encoding remain intact. Shader branch savings have not been measured.
- Single-bitmap frost leaves use ModulateAlpha in both the visible wallpaper and prepared
  source blend. The opacity remains live; it is applied to the single image draw rather than
  an additional group-alpha target. Filtered multi-draw trees retain ordinary compositing.
- Reflection metadata no longer delays successful image delivery. Eight optional preparations
  are allowed, with serialized scratch work and trim cancellation. Six row copies replace
  144 individual pixel reads while retaining the same sample coordinates and color math.
- The small wallpaper box filter uses running sums, preserving per-axis integer rounding and
  clamped edges. Continuation app lookup uses a remembered package index with the previous
  first-match policy. App launch bounds no longer write Compose state each scroll placement.

The 12 MiB Coil cache, decode bounds, existing lazy preparation window, input rectangles,
native focus ownership, motion timings and quality preferences are unchanged. No native
library, new dependency or system setting is introduced. The full-scene native renderer,
larger row caches and appearance-changing shadow substitutes remain research alternatives;
they are not included as assumed improvements. Compilation, visual behavior, resource peaks,
background/resume and performance still require the user's validation of this exact source.

### Subsequent user-authorized deployment — 24 October 2026

The user requested compilation and installation on the connected physical TV for their own
manual testing. Offline JDK 21 `:app:assembleBenchmarkRelease` completed successfully; no tests
or benchmark journeys ran. The optimized APK was replacement-installed, explicitly started,
and pulled back to verify an identical SHA256:
`a4842d1ecd41cadfd2a552328a91f8c2dea2c660de845bc2134fb064273609e2`.
A single screen capture confirms Home is visible; it does not establish rendering fidelity,
active direct-path selection, performance or interaction acceptance. The previous installed
APK is preserved privately for recovery. No app data reset or default-HOME/system-setting
change was performed. Raw APKs, capture and deployment metadata remain ignored and private.

## Transparent glass repair and matched millisecond comparison — 24 October 2026

The immutable wallpaper-field/direct-sampling shortcut introduced in the preceding source-only
iteration produced black dock/header backgrounds on the physical TV despite Balanced quality,
glass enabled, full intensity, and reduced transparency/high contrast disabled. It was removed,
including its snapshot helpers and comparison flag. Live Haze-owned input with the split
sharp/frost filters is restored. The exact low-level snapshot failure was not isolated; the
before/after on-device appearance establishes the visible regression and recovery, not a
universal claim about GraphicsLayer snapshots. Neutral-interior optics, single-image frost
opacity, asynchronous/batched palettes, running-sum blur and the small Home lookup/bounds
improvements remain.

The corrected optimized APK was visually inspected after startup settled: the wallpaper is
visible through both dock and header buttons. Initial loading was captured separately and
must not be confused with a settled appearance check. Seven focused physical-TV cases pass:
four WallpaperBackgroundTest cases, HazeGlassTest, ArtworkReflectionsTest and the opaque,
split-input GlassStageFidelityTest. Twenty-one synthetic native-window frames were recovered;
a settled movement/tilt frame and actual final Home were visually inspected. The test runner
reported OK (7 tests); an initial artifact pull used the wrong directory, and a subsequent
host-Python extraction option was unsupported. The captures were then retrieved safely from
the app cache; neither collection error was a test failure or a timing sample.

All required offline build gates pass: Spotless, architecture, module unit tests, Debug lint,
Debug/optimized compilation, instrumentation/benchmark compilation and eight-master avatar
validation. The previous prohibition on execution was superseded by the user's explicit
request to fix and test timings. Mechanical formatting was applied before building.

### Physical timing protocol and results

A is the saved optimized APK from before the latest optimization batch, already containing
the previous mapped-glass renderer. B is the corrected source above. This does not compare
against the earlier, slower original Haze renderer or against the broken black-glass APK.
Physical Smart TV Pro/API 34, 1920×1080 app surface, approximately 60 Hz. Both use the unchanged
current Balanced profile, full motion, glass intensity 1 and background blur 1. App configuration
and default HOME are preserved. Each A1/B1/B2/A2 block uses three Partial/Require baseline
warmups and three measured rapid vertical tours: a priming tour, 36 native directional keys
at 180 ms cadence and a 700 ms tail. Processes restart in setup; OS/image/driver caches remain.
No screenshots, hierarchy dumps or resource queries run in timed bursts. Thermal state was not
controlled, so elapsed-order variation remains a limitation.

| Block | CPU-frame elapsed p95 (ms) | Deadline-overrun p95 (ms) | App-start-to-presentation p95 (ms) | App deadline misses / frames |
| --- | ---: | ---: | ---: | ---: |
| Before A1 | 40.80 | 56.69 | 81.92 | 69 / 1155 |
| Corrected B1 | 40.03 | 56.93 | 86.13 | 71 / 1156 |
| Corrected B2 | 39.72 | 56.96 | 86.84 | 72 / 1156 |
| Before A2 | 56.60 | 81.83 | 106.11 | 91 / 1134 |

All twelve measured traces pass quality checks and match delivered inputs and focus walks.
A live provider identity refreshed between B1 and B2; text and bounds stayed the same. Each
block is internally stable. A1/B1 match exact scene identities, and B2/A2 match each other,
but this is not one fully matched four-block scene. The initial strict summary rejected
cross-scene pooling as intended; raw traces and a rejection record are retained privately.
Primary statistics use the pre-refresh A1/B1 pair. The refreshed B2/A2 pair is a separate,
exploratory observation rather than an independently predeclared confirmation.

In the primary pair, CPU-frame p95 decreases by only 0.77 ms (about 1.9%), while deadline
overrun rises by 0.24 ms and actual-start-to-presentation p95 rises by 4.21 ms. Deadline misses
are 5.97% versus 6.14%. The second candidate stays close, but its return baseline becomes much
slower. That baseline variation exceeds the initial apparent gain. These results support
**restored transparent rendering, without a demonstrated meaningful overall speedup**.
Do not advertise the pooled result as a large gain. The <1% deadline budget remains unmet.

CPU-frame duration includes pipeline waits; it is not isolated CPU execution. App completion
includes GPU completion/buffer posting; presentation maps complete app frames to SurfaceFlinger
frames. These measurements do not isolate shader time or physical button-to-first-changed-pixel
latency. Both candidate blocks contain mapped optical markers in every iteration and no mapped
shader-fallback marker. No startup, memory, energy, long-duration thermal or broad hardware
acceptance follows from this vertical workload.

Configured DataStore/wallpaper bytes match before and after benchmarking and native tests.
Default HOME is unchanged. The corrected optimized APK was restored, explicitly started,
pulled back and hash-verified; its transparent Home was inspected after all tests.
A APK SHA256: `e1fc1aee6fa9981defa52e356a174c62e94e71ee79b3d02a1d98807e370e725a`.
B APK SHA256: `d061520534d74720ef2282bedac9768bd5bf78be36400b7bce81c11a873ec55b`.
Production source SHA256: `442201e274002dfb20932f336960830abcda69161d2a17dc75039c70090b3dc8`.
Raw APKs, source identities, event/focus/scene records, traces, captures and preservation
archives remain ignored in `.private/transparency-fix-20261024/`.


## Presentation investigation and inward sampling bounds — 24 October 2026

The user requested one-change-at-a-time physical-TV measurements, retaining transparent
dock/header rendering and only keeping measured winners. The corrected transparent optimized
APK (`d061520534d74720ef2282bedac9768bd5bf78be36400b7bce81c11a873ec55b`)
is the control throughout. The dirty starting checkout and immutable controls were preserved;
no earlier user/session work was reset. Raw configurations, captures, APKs and traces remain
in ignored private evidence.

### Diagnosis and rejected experiments

The control traces contain substantial RenderThread drawing and UI-thread `postAndWait`
time. In one complete control trace the app-completion-to-mapped-display-end median is
25.04 ms, p95 27.51 ms. These are separate stages: neither CPU recording alone nor an isolated
shader duration explains presentation latency. No GPU hardware counters were available in
these traces, so this does not identify a pure GPU-execution bottleneck.

| Isolated candidate | Presentation results | Decision |
| --- | --- | --- |
| Skip shader calculations outside the parent-clipped material | A1/B1/B2/A2 p95 86.60/84.18/84.32/82.38 ms | Reverted: screening gains were unconfirmed. A provider identity refreshed within A2, invalidating complete scene matching; its faster return-control result cannot establish a fully matched regression either. |
| Always retain the filtered sharp/frost/veil composite | Matched control/candidate p95 86.79→81.88 ms, p99 123.25→148.47 ms | Reverted: all three candidate iterations worsened p99. Sampled full-screen layer draws increased 22→36. |
| Retain that composite only when fully frosted | Matched p95 87.76→89.15 ms; overrun p95 56.97→65.65 ms | Reverted: slower presentation and overrun. A preceding both-endpoint variant passed seven rendering cases but was refined before timing; those captures are not attributed to the timed frost-only APK. |

The accepted change removes unnecessary capture/output padding from the mapped optical stage.
Its displacement field bends inward. If the minimum material extent is at least twice
(maximum displacement + 2 pixels), even the maximum inward displacement cannot cross the
opposite boundary. The original padding remains for tiny materials. The dock intermediate
shrinks from 1844×274 to 1792×222 pixels (21.3% less area); actual RenderThread layer dimensions
confirm that the smaller path runs on the TV. This adds no source snapshot or composite cache.
The live source, shader, optical map, full-resolution sampling, image/cache bounds, focus,
input rectangles, motion and opaque/unsupported fallbacks remain unchanged.

### Final matched physical comparison

Physical Smart TV Pro, API 34, existing 1920×1080 surface at approximately 60 Hz. Balanced,
full motion, glass intensity 1, wallpaper-depth blur 1 and reduced transparency OFF are
preserved. No default HOME, accessibility, power or display setting changes. The reference
HOME remains available. Only this app is replacement-installed and explicitly started.

Final production-source fingerprint:
`e959b21180089902364442bebde537a12647efadd95a7855f71e97fcdf2fbc6a`.
Optimized R8/resource-shrunk candidate APK:
`8ada0e9e0d520cbe404f070efaefa4865c4f378f8d232846a10b8d60aaa05592`.
JDK 21, pinned offline Gradle dependencies, Partial/Require compilation with three warmup
and three measured iterations per block. Each rapid warm iteration primes a tour, settles,
then injects 36 native vertical keys at absolute 180 ms cadence with a 700 ms tail.
Processes restart in setup; OS, file, Coil and driver caches remain. There are no screenshots,
memory queries or hierarchy inspections inside timed bursts.

| Block | CPU-frame elapsed p95 (ms) | Overrun p95 (ms) | App-start-to-presentation p95 / p99 (ms) | App deadline misses / frames |
| --- | --- | --- | --- | --- |
| A1 | 54.41 | 82.73 | 107.21 / 124.49 | 89/1136 |
| B1 | 39.77 | 52.59 | 81.91 / 116.37 | 69/1157 |
| B2 | 38.23 | 50.52 | 81.89 / 116.24 | 65/1161 |
| A2 | 39.64 | 56.95 | 86.62 / 122.59 | 68/1159 |

Every measured iteration in all four blocks matches actual delivered keys, native focus
walks and visible scene tags/text/bounds. All twelve complete traces pass loss/setup and
frame/display-correlation checks. All iterations exercise mapped optics with zero shader
fallback. Scene audits also check every iteration, rather than only the last scene of a block.

A1 includes a slow first iteration: presentation p95 111.94 ms versus 85.57/85.41 ms for its
other iterations; its full-screen layer count is 44 versus 22 in those other iterations.
It is retained, not discarded. Use the return pair A2/B2 as the conservative headline:
presentation p95 86.62→81.89 ms (4.73 ms, 5.5%); p99 122.59→116.24 ms (6.35 ms, 5.2%);
CPU-frame p95 39.64→38.23 ms; overrun p95 56.95→50.52 ms. Candidate blocks reproduce
81.91/81.89 ms presentation p95 and 116.37/116.24 ms p99. Median presentation remains
approximately 65.2 ms, and deadline misses improve only slightly in the return pair
(68/1159→65/1161). This is a tail-latency improvement, not a broad median speedup.

Presentation uses actual app-frame start to the correlated SurfaceFlinger display-frame end;
app completion includes GPU/buffer work. Macrobenchmark CPU-frame elapsed time includes
pipeline waits. Frame quantiles are descriptive correlated samples, not independent trials,
statistical significance, isolated GPU execution or remote-button-to-photon latency.
The <1% missed-deadline and 60 Hz budgets remain unmet; no budget is relaxed.

### Rendering and local validation

The exact accepted-source Debug/test APKs pass all seven physical wallpaper, glass,
reflection and moving/source-transition/resize/reattachment cases. Twenty comparable synthetic
window-frame pairs differ from the preceding transparent control by at most one channel
level, with no pixels differing by more than three. The initial frozen-clock capture differs
because asynchronous map readiness is not synchronized; both sampled initial appearances
retain their source, but that pair does not establish pixel equivalence. Settled optics and
motion captures are visually inspected. The comparison does not establish universal fidelity.

The bilinear sampling property test checks every pixel centre for representative dock,
header, minimum-eligible, narrow and low-density material sizes at maximum displacement,
plus tiny-material padding retention. Required Spotless, architecture, module tests, Debug
lint, Debug/optimized/instrumentation/benchmark compilation and avatar checks pass.
Configuration/wallpaper byte preservation, final installed APK and actual Home transparency
are checked separately after timing. No startup, idle, long-duration memory or broad hardware
acceptance is claimed by this experiment.

Final replacement-install verification confirms the accepted APK hash and the unchanged
production-source fingerprint. Configured DataStore and wallpaper files are byte-identical
to the pre-investigation archive. Only ProfileInstaller/compilation bookkeeping files differ;
a whole-files-directory equality check was initially too broad and its failure is retained in
private evidence. Default HOME still matches the pre-repair value. The accepted optimized
Home is explicitly started, captured and visually inspected with transparent dock and header
controls. UI scene matching does not prove identical asynchronous artwork-decode completion
at every instant; the scoped repeated frame results and initial-control outlier remain visible.


## Follow-up: visible-wallpaper pass fusion (2026-10-24)

Three isolated candidates target the visible full-screen wallpaper, independently of the
accepted smaller mapped-glass bounds. The sharp/frost blend, black veil and vertical gradient
can be expressed as one colour-managed RuntimeShader paint with two BitmapShader inputs.
This reduces draw passes without changing the live split-glass source, image decode/cache
limits, focus motion or quality settings. Original crossfades, translucent inputs, software
rendering and unsupported API paths retain the existing layered renderer. One candidate
retains the bounded full-screen cache; another samples both inputs without endpoint branches;
a third draws the conditional paint directly using Auto compositing.

This is an experiment in removing rendering work, not evidence that a native C/Rust library
would improve this workload. Traces still contain substantial RenderThread drawing/flush work
and pipeline waits. Their inclusive slice durations overlap; these traces contain no GPU
hardware counters and cannot isolate shader execution, memory bandwidth or cache eviction.

The same physical TV, configured transparency, source anchor, optimized build, Partial/Require
compilation and rapid-warm protocol as the preceding experiment are used. Every block has
three measured iterations after three compilation warmups. Native delivered keys, focus walks
and all measured scene tags/text/bounds are checked. Raw traces, APKs, device identifiers,
settings archives and captures remain ignored private evidence. A disconnected direct-path
confirmation attempt is preserved separately and excluded; its runner failed in UiAutomation
cleanup. A fresh output directory is used for the successful retry.

Eight focused physical cases pass for each experimental source, including an additional
nonvacuous fused-path test comparing both-dimensional opaque patterns at ten crop/depth
combinations and replacement/resize. All 21 moving/source/resize/reattachment capture pairs
for the final experimental variants differ from the accepted control by at most three channel
levels, with no pixels exceeding that bound. Settled transparency is visually inspected.
These rendering checks establish scoped fidelity, not performance acceptance. They do not
justify keeping a candidate which fails the repeated timing criteria.


| Variant / block | Presentation p95 / p99 (ms) | Overrun p95 (ms) | App deadline misses / frames |
| --- | --- | --- | --- |
| Conditional cached control A1 | 83.71 / 115.31 | 54.48 | 70/1156 |
| Conditional cached B1 | 81.91 / 98.44 | 55.62 | 52/1173 |
| Branchless cached control A1 | 103.91 / 118.89 | 80.56 | 85/1141 |
| Branchless cached B1 | 84.16 / 101.70 | 56.54 | 53/1171 |
| Branchless cached B2 | 95.63 / 105.17 | 68.46 | 81/1144 |
| Branchless cached control A2 | 81.91 / 115.46 | 55.51 | 75/1153 |
| Direct control A1 | 103.60 / 116.11 | 80.52 | 86/1139 |
| Direct B1 | 82.27 / 93.58 | 51.26 | 73/1145 |
| Direct B2, successful retry | 82.34 / 92.60 | 51.58 | 84/1142 |
| Direct control A2 | 81.93 / 115.97 | 54.35 | 68/1158 |

Conditional cached fusion fails the original frame-overrun nonregression screen and is
reverted. The branchless cached candidate passes its initial screen, but its repeated
improvement is unconfirmed: provider identities refresh between B1, B2 and A2. Each individual
block has a uniform scene, but the final pair does not match completely. Its slower B2 and
faster return control remain visible as descriptive results; they cannot establish a causal
regression. No mixed-scene pooling or acceptance is allowed. Both branchless and direct A1
controls contain a slow iteration, so their attractive initial tail gains are not sufficient.

The direct candidate has a completely matched final pair and fails presentation-p95
improvement (81.93→82.34 ms). Its typical app completion also worsens 39.97→44.92 ms and
missed deadlines increase 68/1158→84/1142 despite its improved p99. It is reverted. Reducing
texture operations or a full-screen cache does not by itself establish a faster complete
frame. No experimental renderer is retained. The added fusion-only comparison fixture is
also removed with that renderer; experimental sources and evidence remain private.

The preceding verified smaller-bounds source/APK is restored, replacement-installed and
explicitly started. Its production-source fingerprint and optimized APK hash exactly match
the preceding accepted result. Required local gates pass again on this restored source;
Spotless, architecture, all module tests, Debug lint and Debug/optimized/test/benchmark
compilation pass. Avatar masters pass separately. Final settings/wallpaper byte comparison
passes, with only ProfileInstaller bookkeeping differing. Default HOME is unchanged. A final
physical Home capture is visually inspected: the dock and header controls remain transparent.
No additional speedup or 60 fps acceptance is claimed from this follow-up. The existing
<1% missed-deadline budget remains unmet. Presentation durations are correlated frame
latencies, not inverse frame rates or button-to-photon measurements.
