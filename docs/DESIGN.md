# Design

The interface is remote-first, with clear focus, generous spacing and predictable return paths. App-only mode must feel complete without media permissions or content providers. Keep visual changes separate from architecture and persistence changes.

## Home and organization

Use standard Foundation lazy layouts and TV Material3 controls. Favorites and folders use stable identities; each app appears in one organization position. Preserve absent-package positions in storage and choose a surviving logical neighbor when focused content disappears.

Focus enlarges artwork through graphics transforms while native input bounds stay fixed. Reserve room for scale/shadow and avoid clipping. Header controls remain discreet until focused. Keep sparse catalogs balanced; do not add fabricated content to fill space.

Move/reorder is an explicit local preview: arrows move the candidate, Select commits and Back cancels. Moving an app between catalog and favorites must preserve its logical focus even when the combined ID order is unchanged.

## Panels and profiles

Dialogs contain focus and return to their trigger. Nested Settings/diagnostics/photo panels retain their parent return target. Profiles keep selection separate from focus, and editing targets the captured profile ID. Draft names/avatar choices survive recreation. Invalid forms show clear disabled/error states.

Keep shared theme, glass, controls and artwork in the design system. Full settings screens and contrast/reduced-transparency paths remain readable without relying on sampled background colors. Do not reduce rendering quality as an unmeasured performance shortcut.

## Accessibility and data states

Maintain English and French strings. Support system and explicit reduced motion; retain visible focus when animation is disabled. Verify large text, RTL and contrast separately. Show loading, empty, cached failure, denied and unsupported states honestly. Status changes must not steal focus.

Image loading uses bounded asynchronous requests and static fallbacks. Only the focused visible loading app may show its existing one-pass feedback; stop it on success, error, blur, lifecycle stop or reduced motion. No permanent idle animation or network artwork backend is needed.

## tvOS 26 interaction study

The implementation keeps Oree's Home cards, bottom favorites dock, backdrop and glass.
Apple's Home, Apple TV app, Search, Photos and Control Center are separate references.
The [tvOS 26 announcement](https://www.apple.com/newsroom/2025/06/apple-tv-brings-a-beautiful-redesign-and-enhanced-home-entertainment-experience/)
provides version-specific material examples; HIG guidance is not version-specific evidence.
All dimensions and motion parameters remain Oree design choices, not Apple measurements.
General principles also come from [Apple focus HIG](https://developer.apple.com/design/human-interface-guidelines/focus-and-selection),
[designing for tvOS](https://developer.apple.com/design/human-interface-guidelines/designing-for-tvos),
[Android TV navigation](https://developer.android.com/training/tv/get-started/navigation) and
[Android focus](https://developer.android.com/design/ui/tv/guides/styles/focus-system).

### Settings — batch 1

- Documented: [tvOS 26 appearance](https://support.apple.com/en-euro/guide/tv/atvb46af4f11/26/tvos/26)
  places explicit appearance choices under General > Appearance.
  [Remote navigation](https://support.apple.com/en-euro/guide/tv/atvbe55a4b4e/26/tvos/26)
  distinguishes highlighting, selection and return to the previous screen.
- Adaptation: keep two panes. Up/Down browses categories; Select or Right enters that
  category. Merely focusing another category does not save a value. Back from options
  returns to the active category, then Back exits to the original launcher destination.
  Each category retains its scroll state. Left returns spatially to the categories.
- Theme, quality, text size and bounded glass intensity open explicit lists. The stored
  choice has a marker independent of focus. Select accepts immediately; Back dismisses
  without changing it. Booleans and accent remain direct explicit changes. Existing glass
  preview remains bounded and local. Write failure/retry remains visible in Settings.
- Reset first explains its scope and focuses Cancel. It resets appearance/accessibility/
  motion for the current local profile, preserving organization and control customization.
- Unverified: exact Apple Settings layout and animation timing. The guide's appearance
  image depicts Home, not a Settings chooser; it cannot establish chooser or Back behavior.
  Physical interaction and large-text acceptance are recorded separately from compilation.

### Folders, organization and app menus — batch 2

- Documented: [tvOS 26 app organization](https://support.apple.com/en-euro/guide/tv/atvbad14dc6a/26/tvos/26)
  uses held Select for actions, explicit editing/moving and Select to finish. Folder names
  can be selected for editing. Back returns to Home. The supplied official folder image
  shows a folder tile on Home, not its interior; no interior geometry is inferred from it.
- Adaptation: preserve Oree's grid and bottom dock. A folder opens a larger bounded app
  space, initially focusing the remembered surviving app. Select on the title renames it;
  Folder options exposes management. Removal explains that apps return to Home and starts
  at Cancel. Back dismisses confirmation/options before closing the folder. Empty folders
  retain their editable title and options. Forms focus the first available field/destination.
- Preserve explicit preview transactions: directions move neighbors, Select commits, Back
  cancels; launching is suppressed during editing. Transfers remain explicit context-menu
  destinations rather than drag gestures unsupported by a D-pad. App menu order is Open,
  Favorite, organization actions, App info, then Uninstall. Back from uninstall confirmation
  restores its own row. Android still owns the actual uninstall confirmation and protection.
- Retain existing held-key suppression, fixed input bounds, stable app keys and bounded
  requester registration. These are launcher choices; Apple's continuous drag/parallax
  behavior and exact folder animation are not reproduced or claimed.

### Search and keyboard — batch 3

- Documented: the [tvOS 26 Search app](https://support.apple.com/en-euro/guide/tv/atvb8f84a271/26/tvos/26)
  presents categorized results below the query; it is distinct from search inside Apple TV.
  The [text-entry guide](https://support.apple.com/en-euro/guide/tv/atvb2ae48ba6/26/tvos/26)
  describes Apple's keyboard, not Android IME behavior.
- Adaptation: retain the full search panel and Android keyboard. Results are grouped as apps,
  folders, actions, accessible content and local photos, preserving rank within each group.
  Apps/content/photos have bounded local artwork with static type fallbacks; folders show
  their stored app count. Loading differs from no matches. Optional-source failure leaves
  other results usable. Android search remains a separate explicitly labeled external action.
- Down from query enters results and hides the keyboard; Up from the first result returns
  to query. Back hides IME before closing Search. Retain request-tagged results, query,
  logical result identity and scroll restoration across external destinations. Async changes
  restore a missing focused result but do not refocus a surviving result or the Close row.

### Previews, continuation, profiles and command controls — batch 4

- [Top Shelf HIG](https://developer.apple.com/design/human-interface-guidelines/top-shelf)
  discusses content tied to a focused Home app. This is not the layout of the
  [Apple TV app Home](https://support.apple.com/en-euro/guide/tv/atvbe160da08/26/tvos/26),
  whose Continue Watching can open content directly. Oree retains its existing immediate
  native focus and 160 ms cancellable content settlement, package-tagged previews and
  source-validated direct launch. Missing art keeps the static local app/title fallback.
- Watch Next optionally reads episode/season and playback position/duration from public
  provider columns, without another content service. A provider rejecting those columns
  falls back to the previous projection. Only valid supplied progress is displayed; it is
  neither proof of entitlement nor a promise of exact resume. No detail screen is inserted
  where the existing direct destination already supplies the useful action.
- [tvOS 26 profiles](https://support.apple.com/en-euro/guide/tv/atvb5f549664/26/tvos/26)
  switch on Select. Oree explicitly labels local profiles and shows Active independently
  of the focus lift. Existing avatar artwork and separate held-Select editing remain.
- The [tvOS 26 Control Center](https://support.apple.com/en-euro/guide/tv/atvb0a416871/26/tvos/26)
  guide and supplied image show grouped compact controls, conditional tabs and Back to close.
  Oree retains the existing customizable four-control panel, actual Android destinations,
  status indicators and nested move/resize cancellation. Power/CEC/Siri/account capabilities
  in Apple's image do not become Oree actions. Exact Apple transition timing is unverified.

### Photos, ambience, backup and support — batch 5

- [tvOS 26 Photos](https://support.apple.com/en-euro/guide/tv/atvbfc5adda8/26/tvos/26)
  documents Select for fullscreen viewing, then Left/Right for adjacent photos.
  [Screen saver configuration](https://support.apple.com/en-euro/guide/tv/atvbfa799b87/26/tvos/26)
  is a separate flow; its automatic behavior is not transferred to the launcher.
- Oree's Photo Viewer has no slideshow timer. Left/Right browse the accessible collection
  with stops at its edges; Up/Down stay in the viewer. Select opens controls. Back closes
  controls, then returns to the original Search result. The wallpaper gallery has a separate
  selection contract: Select imports and applies that image directly, without opening the
  viewer. Success closes both wallpaper panels; failure stays visible for another selection. Denied/empty/removed
  files do not block other launcher functions. Decode bounds remain 1280×720.
- Manual Ambient retains its complete fresh-gesture-to-exit contract and 30-second slideshow.
  DreamService scheduling stays under Android control. No display/power settings change.
- Backup keeps SAF selection, reviewed profile/folder/favorite summary, missing-image warning,
  cancel-first confirmation, retained result and atomic undo. Picker cancellation now reports
  that configuration is unchanged. No format, stored identity or migration change is needed.
- Daily source toggles remain under Home settings; technical diagnostics move to About >
  Support. The existing D-pad-readable bounded report, loading state and retry remain.

A fresh Home/Commands/Search entry to Settings gets a new saveable presentation identity;
closing one visit cannot make a later search shortcut inherit its category. Nested panels and
Activity recreation retain the current visit's identity, option focus and category scroll.
Gallery loading initially offers Close; once the user navigates, arriving photos do not move
focus back to the first thumbnail.

Tracking: TVL-6 (settings), TVL-11/12/13 (menus and organization), TVL-14 (search),
TVL-9/18/20/7 (preview/resume/profile/control contracts), TVL-21/24/16
(photo/ambient, backup and support). This iteration does not close broader roadmap,
physical performance, hardware compatibility or release gates.


### Verification checkpoint — 2026-10-22

All mandatory Gradle gates pass, including Spotless, architecture, module tests, lint,
optimized APK and instrumentation/benchmark compilation. There are 60 distinct passing
unit cases; avatar export validation passes. The physical API 34 broad run has 82 passes
and one skipped real-photo case. After the final fresh-entry correction, the targeted
Settings/Folder/Search/Photo/Activity suite passes 19/19, including successive different
Search shortcuts and nested recreation. These overlapping runs must not be summed.
An earlier focused run failed its obsolete expectation that uninstall cancellation focused
Open; the correct trigger is now Uninstall. The corrected run passes; raw history is retained.

Actual optimized UI was also inspected: local artwork and grouped Search, native IME Back,
theme browsing with Light focused while System stayed selected, cancellation to Theme then
Appearance, and native User Guide launch/Back to the exact originating tile. Fixture captures
cover EN/FR, light/dark, 200% text, contrast, reduced transparency and reduced motion.
The TV's current IME displays mobile-entry guidance; character entry in instrumentation is
injected text. Full on-screen D-pad keyboard entry is not established and no IME setting changed.
No authorized local photo collection was available: viewer/thumbnail return uses project-owned
resource fixtures; actual MediaStore import remains skipped. Activity recreation is tested,
not unexpected process death across every flow. Broader hardware/accessibility coverage remains.

The ordinary-caller provider probe returned 29 accessible entries, two with episode metadata
and 29 with supplied valid progress. Counts are bounded and device-specific; destination
resolution, entitlement and exact playback remain separate. Performance remains unresolved;
see the unpaired legacy diagnostic in PERFORMANCE.md, not a performance improvement claim.

Replacement installs preserve the existing HOME route, verified unchanged. The first archive
attempt was refused by the original non-debuggable APK; initial byte-for-byte data preservation
cannot be asserted. A later archive was validated, and configured profile bytes match it after
final tests; only an installer marker changed. No data clear, system-setting change, launcher
removal, public push or release was performed. Raw captures, backups, logs and full device
identity remain private.

Final production source SHA256: `a077c6be8726243ac1efd366b2f5ed09c6e6afe1a7259b439b597b18f3937af1`.
Optimized APK SHA256: `e0f259a69ba1f0c99ba6354fc995c0db31af3ed7e780529d2ab9ffbbcfe0bf8a`.
The 82-pass broad run preceded the last entry-isolation correction; its private source/APK
manifest is retained separately. The 19-pass final run and optimized manual checks match the
final source. Existing roadmap tickets remain In Progress where broader acceptance is open.


### Screen depth and visual states — October 22 follow-up

The Home landscape is sharp at its resting hero position. Raising the dock/catalog past
96 pixels transitions to the already prepared 192×108 frost image and a 25% black veil;
returning to the hero restores the sharp image. These thresholds, 280 ms transition and
scrim are Oree design choices, not measured tvOS values. Apple’s tvOS 26 announcement
shows layered translucent controls; exact Home scroll-dependent blur has not been verified.
Reduced motion snaps this transition. No per-frame bitmap processing or larger cache is added.
Settings retains a diffuse wallpaper behind its theme-tinted veil, a compact category rail
and a separately scrollable option surface. Reduced transparency/high contrast keep the
opaque Settings background. Folder browsing expands across the safe screen area, with a
large title and contextual options above the app grid. Home cards/header stay retained but
are visually hidden behind Folder, Search and the wallpaper gallery, avoiding a second
visible app grid through those surfaces. Closing restores the same Home layout/focus.
Search uses two result columns at
normal TV widths and a single column when available width is smaller. Native grid focus,
stable result identity and Android IME handling remain in place. The Control Centre now
has an explicit heading above its customizable controls.

The wallpaper picker no longer routes through Photo Viewer or reloads the collection there.
The gallery starts its permission-checked read immediately on resume instead of waiting
100 ms. This removes known extra work; USB/provider latency is still device-dependent.

The scrolling Home grid is clipped below its 96 dp header band. Items leaving the viewport
cannot paint over the clock and top controls; their layout, bring-into-view bounds and
logical focus restoration stay unchanged.

Gallery and Search photo thumbnails now request the provider's bounded 256×144 thumbnail
on API 29+, rather than opening every original image for a small card. The cancellation
signal follows the requesting composition. Unsupported providers/older Android fall back
to the existing bounded Coil decode. Thumbnail cache identities are separate from fullscreen
photos; both use the existing 12 MiB shared memory cache. Permission denial remains an error,
not a second read. No permission, collection size or original/wallpaper decode limit changes.
See [ContentResolver.loadThumbnail](https://developer.android.com/reference/android/content/ContentResolver#loadThumbnail(android.net.Uri,android.util.Size,android.os.CancellationSignal)).
Real provider/file latency still requires an accessible local photo collection to verify.

The wallpaper gallery exposes the Android image-file picker directly, without an Other
sources expansion. Opening this panel does not automatically request library permission;
Allow access is an explicit separate action. Already granted access still loads thumbnails
immediately. Selecting an image applies it directly, without the fullscreen viewer.

Search retains its saved query, grid position and result identity while Wallpaper, Gallery
or Profiles is open, but its window is not rendered behind those destinations. Photo Viewer
keeps its existing opaque fullscreen window and retained Search parent.
Back reveals the saved parent instead of stacking visible result cards under the child.
Wallpaper/Gallery use the diffuse scene behind their own surface. Profiles, like Control Center,
keep the underlying scene unchanged; opening Profiles does not trigger wallpaper diffusion
or its full-screen dim layer. Profile editor materials remain local to their panel.

Cancelling the Android image picker returns focus to Choose an image file, preserving the
current wallpaper. Gallery thumbnail restoration remains separate from this external return.

### Folder management — tvOS 27 follow-up

[Folder management](FOLDER_MANAGEMENT.md) records the tvOS 27 references and Oree adaptation.
Folder cards use fixed preview slots, a readable name/count and the shared fixed-bounds lift.
Held Select opens options; the panel adds a source-labeled Add apps transfer list. Header
focus, member focus, nested name entry and removal confirmation have explicit return paths.
Folder dialogs retain the caller's text scale and resources across the native window boundary.

### Home and Control Center follow-up

Control Center keeps the clear underlying Home scene, with a compact heading, existing saved
move/resize layout, explicit customization and a Profiles shortcut. Sound opens an inline
media volume/mute view; fixed-volume devices show that limitation and offer system audio
settings. Wi-Fi opens Android's quick connection panel where available, falling back to Wi-Fi
settings. Modern Android does not allow ordinary apps to toggle Wi-Fi, regardless of nearby
Wi-Fi permissions; no radio, accessibility or privileged grants are requested. Control Center
retains its trigger when returning from the system panel. Volume keys remain system-managed.
The normal MODIFY_AUDIO_SETTINGS permission enables explicit on-screen volume actions only.

Home caches only native-scale, settled artwork layers. Focused/magnified, pressed and tilted
artwork stays live. The dock uses live, source-backed clear Haze glass with edge refraction and specular
highlights. It does not freeze or replace a settled capture. Reduced transparency uses an
opaque surface.

## Six-column Home and continuation layout

The dock accepts at most six favorites. Existing stored lists remain readable without losing
IDs; favorites beyond the first six available apps appear in the catalog. Both dock and catalog
use six equal columns, 20 dp gaps and matching card centers. An unavailable Add to Favorites
action explains the six-app limit; removal remains available.

Home settings offer compact continuation below the dock or large landscape cards above it.
Only enabled provider content is shown, with actual episode/progress metadata when available.
No playback progress is invented for app previews. Up/Down traverses header, continuation,
dock and catalog in their displayed order.

Text size, glass strength and background blur use centered fullscreen D-pad sliders. Left/Right
changes a local draft, Select commits once and Back cancels. The trigger regains focus. The
layout scrolls at large text sizes. Background blur mixes existing sharp/preblurred layers.

## Glass and Control Center refinement

The app viewport includes the header area, with top content padding instead of a clipped
rectangle below the header. A transparent gradient protects clock/control contrast while
cards scroll behind it. Continuation rows extend across the viewport, retaining the 52 dp
first-card alignment and enough inset to show focus enlargement at either horizontal edge.

Control Center uses one bounded 384x216 scene capture before opening. A softly feathered
blur (12 dp, with a light 10% dark tint) surrounds the measured Control Center panel rather
than the entire screen side. Its
bounds follow the actual controls or Audio content, with a 24 dp inward feather and no outer
halo. The rendered
underlay is itself recorded as the Haze source for the tiles, so their optical material starts
from the same softened scene. No live Home capture is added. Reduced transparency/economy
retain opaque panels. Customization stays in Settings and on a control long press, without
the two footer links.

The Audio view uses an output summary and horizontal volume rail, following Apple's tvOS
27 audio guide. Left/Right adjusts volume; Up/Down moves focus; Select mutes; Back returns
to the Audio tile. Fill and focus emphasis retarget smoothly. Available Android output types
are explicitly labelled available, not active or selected. Android audio settings own switching
outputs, without inventing per-device routing or independent volume controls.

Control Center has no title row or power button. System power stays with the TV remote. Profile selection
badges remain centered on the lower-right avatar circumference, outside image clipping.

References: [Apple Control Center audio](https://support.apple.com/guide/tv/atvb50fb1c50/27/tvos/27)
documents output selection and Left/Right slider input; [Apple's Liquid Glass announcement](https://www.apple.com/newsroom/2025/06/apple-tv-brings-a-beautiful-redesign-and-enhanced-home-entertainment-experience/)
shows the material keeping underlying content visible. The user-supplied Control Center image
grounds tile boundaries. Animation durations here are Oree choices, not measured Apple timings.

The large Continue Watching layout targets two landscape posters across the aligned content
width. It uses a smaller top inset while retaining focus breathing room and a height cap for
large text. Poster captions use medium weight at 60% white; the section heading uses semibold
at 60% white. The compact continuation layout retains its previous typography and geometry.

### Home navigation follow-up — 2026-10-23

Overview is an explicit header control. Its compact section map includes only available
continuation, favorites, folders and catalog destinations, with authentic item counts.
Select reveals that section directly; each app section retains its last surviving logical
selection. Back returns to Overview. The map temporarily compresses Home, and returning
expands it without changing grid geometry. Reduced motion skips those transforms.

Focusing a folder reveals a small preview strip. Held Select or Menu opens its quick actions;
Peek explicitly enters the member strip, Select launches, and Back restores the folder tile.
Open full folder and Folder options retain the existing browsing/management destinations.
Normal Select still opens the full folder, and ordinary Down continues through Home.
Empty folders retain an available full-folder destination. A static bottom cue indicates
additional catalog rows without introducing a permanent animation.

Continuation artwork uses bounded row insets and a larger gap in the large layout, reserving
room for both neighboring focus lifts. The heading and first resting card share an inset.
Hero sizing accounts for caption/font scale and vertical focus clearance. Profiles adds a
progressive side material with a quiet blue tint; opaque/contrast paths retain their palette.
Settings entry keeps the settled two-pane layout and replaces full-screen scaling with a
short fade/translation. Its isolated glass sample loads its image asynchronously at a bounded
1280×720 size and renders optical work only while visible.

Settings Appearance uses stable-key Foundation lazy rows. Offscreen controls are created as
needed; the resting dark Settings fixture is byte-identical before/after this change. Category
return reveals the saved logical item before waiting for its requester, including deep controls
such as Defaults. First entry uses the outer panel transition without a redundant section fade.
The covered Home wallpaper stops recording an unused glass source while Settings is shown;
the independent visible preview retains its original optical treatment. Wallpaper pixels,
rendering quality, stored settings and accepted-write ownership are unchanged.

Opening Settings dispatches immediately from Control Center, with a one-shot disabled state
for duplicate activation. The Settings entry transition supplies the movement. Home remains
retained but hidden and unfocusable beneath it, avoiding destruction of the whole card/focus
tree during the same frame as Settings creation.
Settings entry stages composition after its initial pane is placed: the category pane and content heading
render first, then the independent control list joins the existing transition. Category input
is available during entry; entering content waits for its real controls to exist rather than
for an animation to finish. The settled layout, labels, values and preview remain unchanged.

Content readiness survives recreation so restored lazy scroll/focus state is measured against
the complete saved section, rather than being clamped to a temporary heading-only list.

## Single-column Settings and visual setup

Settings renders only its current level, in one centered column: categories, category content,
or a choice page. Back returns a level; Left is not required. Nested Settings pages cover the
parent content. The visual setup screen places a five-stop intensity rail above a bounded live
preview and an explicit Finish/Apply action. Browsing the rail does not save preferences.
Folder hover shows miniature app artwork without the redundant instruction. Continue Watching
extends to both screen edges while the first resting card and heading share the 80 dp inset.

Card rendering retains static pixels at the full enlarged focus resolution while drawing
reflection and focus transforms live. This avoids repeatedly issuing the static image,
rounded shape, edge and shade operations during movement. Input bounds and image request
quality remain unchanged; oversized cache requests use the original live drawing path.
