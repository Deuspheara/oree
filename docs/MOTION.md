# Motion and remote input

Motion follows input immediately, can be interrupted and stays local to the control. Retarget the current animation instead of queuing another one. Stop offscreen/lifecycle work and respect system and explicit reduced motion.

## Remote controls

| Screen | Arrows | Select / hold Select | Back |
| --- | --- | --- | --- |
| Home | Browse apps and sections | Launch / open actions | Stay on Home or cancel editing |
| Folder | Browse members; Up from first row reaches the title | Launch / open actions | Close nested options, then return to its trigger |
| Reorder | Preview a new position | Commit | Cancel |
| Search | Browse grouped results; Down from query enters results | Open result; Android owns text entry | Hide keyboard, then close Search |
| Settings | Browse the current level | Enter or accept a choice | Return one level |
| Command controls | Browse controls or explicit move/resize positions | Activate or commit | Cancel editing, then close |
| Photo Viewer | Left/Right browse; Up/Down stay | Open controls | Close controls, then viewer |
| Ambient | A fresh complete key gesture exits | Exit | Exit |

HOME, POWER, volume and assistant remain system-managed. Suppress repeats/release from the key that opened a dialog. Reset external-launch guards on resume. Restore dialog focus only when the native window and target placement are ready.

## Focus and scrolling

- Keep focus, press, tilt and reflection in per-card presentation state over fixed input rectangles. Ordinary focus must not resize layout.
- App, folder and continuation artwork use retargetable focus motion. Reduced motion snaps and retains a visible outline.
- In the usual rendering modes, held-arrow navigation temporarily uses a focus ring instead of card lift/tilt/reflection. Release, or the bounded quiet-period fallback, restores the normal treatment.
- Live Liquid Glass keeps those card effects during held-arrow navigation. Reduced motion and high contrast still take priority. See the [slider guide](PERFORMANCE.md).
- App selection settles for 160 ms before preview/wallpaper coordination; cancel obsolete selections and keep the previous settled preview until the next one is ready.
- Catalog/content Up returns through the dock to the header. Header Down reverses the return and restores a surviving app. Rapid reversals cancel obsolete scroll and deferred focus work.
- Initial placement and logical restoration are immediate. User-directed page motion is independent of the effects-quality tier; explicit/system reduced motion and covered Home stay immediate.

Header, card and continuation focus memories are distinct. A native window's automatic focus must not overwrite the intended return target. Explicit section jumps reveal their lazy item and wait a bounded time for registration, with cancellation and a surviving fallback.

## Panels and backgrounds

Panel entry/exit and move/resize feedback are interruptible. Back or lifecycle stop cancels uncommitted organization edits. Search fades over stationary sampling bounds so retained glass does not sample a scaled exit scene.

Wallpaper depth follows Home scrolling, with a retargetable 280 ms frost/veil transition; reduced motion/economy snap it. Prepared glass inputs follow the visible wallpaper transition and keep their original-source fallback while it settles.

Static artwork can reuse a bounded retained GPU layer; reflection and transforms stay live. Do not save pixels across recreation or enlarge input bounds. Keep the shared cache/decode limits.

Photo Viewer has no automatic advance. Ambient photos advance at 30-second intervals with a short crossfade, snapping under reduced motion. Home has no permanent idle animation.

Native D-pad tests and physical captures establish covered interactions. Controlled animation clocks do not measure remote-to-photon latency; performance claims require the [measurement protocol](development/PERFORMANCE.md).
