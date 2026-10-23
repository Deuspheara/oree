# Motion and remote input

Motion is local, interruptible and subordinate to input. Retarget current values rather than queueing animations. Dispose effects when their owner leaves composition and stop work below STARTED. Respect system animator scale, reduced motion and economy settings while retaining clear focus.

## Input contract

| Context | Directions | Select / long Select | Back |
|---|---|---|---|
| Home | Spatial catalog; row edges stop laterally | Launch once / app actions | Stay on Home or exit edit mode |
| Folder | Children with logical/index restoration | Launch / app actions; title renames | Close options/confirmation, then restore folder |
| App menu | Contained actions | Execute once | Return from confirmation to its row, then restore trigger |
| Reorder | Preview neighboring positions | Commit | Cancel original arrangement |
| Search | Query and results | Select; Android owns IME | Dismiss IME, then Search |
| Settings | Categories, then options/explicit choices | Enter, toggle or accept selected choice | Choice → option → category → origin; accepted values stay applied |
| Command controls | Contained controls; explicit move/resize mode | Execute or commit edit | Cancel edit, then close |
| Photo Viewer | Left/Right browse; Up/Down stay | Show controls | Hide controls, then restore origin |
| Ambient | Full fresh key gesture | Exit without activating underlying UI | Exit |

HOME, POWER, volume and assistant remain system-managed. Consume the opening held-key repeats/release at modal boundaries; they must not trigger a short action in the new window. External-launch guards clear on resume. Restore native dialog focus only after window focus and placement are ready.

## Focus and navigation

Keep focus/press/tilt in per-card presentation state and apply graphics transforms over fixed native bounds. Home app and folder cards use the shared short, retargetable spring for selected lift/release; press and directional tilt retain their existing local springs. Other shared artwork keeps its own motion contract. Reduced motion snaps and preserves a visible ring.

Selection settles for 160 ms before content/wallpaper coordination, cancelling obsolete selections. Catalog/content Up returns to the surviving dock item and reveals only what is necessary. The next Up enters the header and starts the interruptible Home return; held directional input can continue through the dock. Header Down cancels that return and restores the last surviving app. Initial placement and logical restoration do not replay the return animation.

Reorder and control move/resize animate only their explicit candidate geometry. Back or lifecycle stop cancels uncommitted organization edits. Normal card focus never resizes layout. Ambient photos advance at the existing 30-second interval with a short crossfade, snapping under reduced motion; Home has no permanent ambient loop.

Native input instrumentation and real-device captures are required for interaction claims. Controlled animation clocks do not measure physical remote-to-photon latency.

Wallpaper depth follows the Home scroll state, not individual card focus. Passing the hero
threshold crossfades the precomputed frost layer and dark veil in 280 ms; reversing scroll
retargets the current alpha. Reduced motion/economy snap. Folder opening uses the existing
interruptible panel transition across the safe screen area. Wallpaper-gallery Select applies
the image directly; photo-viewing directions remain confined to the separate Search viewer.

Folder first-row Up explicitly reaches the editable title. Package changes preserve a
surviving header/app target; removal restores a logical member neighbor instead of accepting
native temporary title focus. Held folder entry consumes its opening release. Removal
confirmation confines navigation to Cancel/Remove and restores Options after recomposition.
Create-name Back follows Android IME dismissal, then returns to destinations before closing.

Home reserves the header area in layout rather than clipping cards across a drawing-only
boundary. Focus reveal includes the dock inset and artwork breathing room. User-directed
section transitions animate when motion is enabled; initial logical restoration stays immediate.
Settled app-preview identity stays visible while the next app settles, preventing a transient
empty content row on every D-pad step. Search fades over stationary glass sampling bounds;
its retained background must not be invalidated by a scale transform during exit.

## Home card and dock follow-up

App, folder and continuation cards share a retargetable focus spring (damping 0.95, stiffness
400). Remote tilt is reduced to 0.30; release settles after 120 ms. These are qualitative
choices informed by the reference, not measurements of Apple's implementation. Reduced motion
retains its immediate path. The moving dock uses live Haze clear optics, avoiding a frozen capture followed by a
settled-image replacement.

## Control Center audio and focus correction

The audio surface enters with a short 180 ms fade/scale from its upper-right anchor. The
horizontal volume fill retargets over 140 ms; rail/knob focus emphasis retargets over 120 ms.
Control tiles use a 0.92/500 focus spring for a bounded 3.5% lift and a 140 ms palette change.
Up/Down remains navigation in the Audio view; Left/Right adjusts the focused rail. These are
Oree implementation choices informed by Apple's Liquid Glass presentation and tvOS audio
guide, not recovered Apple animation constants. Reduced motion skips these transitions.

## Navigation pace follow-up — 2026-10-23

This supersedes the 0.95/400 Home-card focus tuning above: app, folder and continuation
artwork use a critically damped 1.0/260 spring for continuous entry, retargeting and release.
Composition-local reads stay unconditional across focus changes, preserving the remembered
animation state rather than recreating it at the new target. The controlled-clock regression
failed before this correction and passes with partial entry and continuous release.
Artwork keeps one automatic compositing path through rest and motion, preserving native
rendering instead of switching to an offscreen texture when focus settles.

Repeated directional key-downs suppress card lift, tilt and reflection, retaining a visible
focus ring. Key release, or a bounded 180 ms quiet period if release is missing, restores the
ordinary selected-card treatment. Vertical page navigation remains animated during repeats. The measured hero return uses a
420 ms ease, and row reveal uses a critically damped spring at stiffness 180; a new direction
cancels/retargets the current movement. Initial restoration and reduced motion remain immediate.
Input is delivered immediately; no card waits for its predecessor's release to finish.
System/explicit reduced motion retains its immediate path. Economy keeps static card focus
effects, while vertical page navigation remains animated unless reduced motion is requested.

Overview compresses Home over 220 ms and expands it on return. Native modal focus is restored
only after the parent window regains focus; explicit section jumps first reveal their lazy
item and then await its bounded requester. Peek consumes the opening held Select release,
requires a fresh explicit Peek activation, and keeps normal Home directions available.
The member strip unfolds from its start edge over 180 ms and folds with panel dismissal;
reduced motion enters immediately. Focus ownership is frozen across native automatic focus
changes until the captured member/header generation is restored.

## Navigation workload and visual setup

Default motion intensity is 1, preserving the existing card springs, tilt, lift, press and
reflection. Focus changes stay in local presentation state; settled app-preview work is
isolated from Home composition. Tilt axes share one offset animation with the same spring.
Settings focus memory does not invalidate its list when the selected row changes.

The visual setup rail previews Simple, Light, Balanced, Rich and Full. These are relative
workloads, not device capability ratings. Simple uses static focus feedback; other steps
scale card motion and glass/blur strength. System reduced motion still takes precedence.
The preview only animates when the selected step changes and has no idle animation.

Static card content, edge and shade use an independent retained GPU layer at the maximum
focus scale. Reflection is a separate live draw node; lift, press, tilt and shadow retain their
springs and fixed input rectangle. The layer belongs to its composed card, uses no bitmap
readback or saved-state pixels, and falls back to live drawing above 4,194,304 raster pixels.
The shared artwork decode limits and 12 MiB Coil memory cache are unchanged.

Rapid vertical reversals cancel the pending Home scroll and its deferred focus request before
the new direction reaches the current control. Header identity stays authoritative until a
card actually receives focus; Up can therefore reverse a pending header-to-content reveal
without allowing its old content target to regain focus afterward.

Home page motion is independent of the effects-quality tier. Economy retains its existing
solid panels and static card treatment, but uses the same soft vertical reveal and eased
return as standard motion. Explicit/system reduced motion and covered Home remain immediate.
Search already lets Foundation animate result scrolling independently of its reduced-effects
background policy. Do not infer frame-rate acceptance from this policy separation.

The current upward-handoff experiment keeps the 420 ms header-return duration and endpoint.
A presentation-owned observer samples page displacement only while the grid is scrolling.
A sample no older than 60 ms can carry same-direction upward velocity into a monotone Hermite
return with zero ending velocity. The starting slope is capped at 0.75 of average return speed to prevent an aggressive
handoff; stationary,
stale and opposite-direction cases keep the original easing. Reversals clear the sample, and
covered/reduced Home does not run the observer. Downward reveal, fixed input bounds, focus
routes and visual quality are unchanged. An earlier, faster handoff felt better on the physical remote but regressed frame p95.
The gentler cap passes its first matched current-quality regression screen; physical
acceptance and independent confirmation are pending. Mathematical bounds and functional passes
alone do not prove smoothness.
