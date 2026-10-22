# Motion and remote input

Motion is local, interruptible and subordinate to input. Retarget current values rather than queueing animations. Dispose effects when their owner leaves composition and stop work below STARTED. Respect system animator scale, reduced motion and economy settings while retaining clear focus.

## Input contract

| Context | Directions | Select / long Select | Back |
|---|---|---|---|
| Home | Spatial catalog; row edges stop laterally | Launch once / app actions | Stay on Home or exit edit mode |
| Folder | Children with logical/index restoration | Launch / app actions | Close and restore folder |
| App menu | Contained actions | Execute once | Close and restore trigger |
| Reorder | Preview neighboring positions | Commit | Cancel original arrangement |
| Search | Query and results | Select; Android owns IME | Dismiss IME, then Search |
| Settings | Categories and values | Save explicit setting changes | Return without undoing saved values |
| Command controls | Contained controls; explicit move/resize mode | Execute or commit edit | Cancel edit, then close |
| Ambient | Full fresh key gesture | Exit without activating underlying UI | Exit |

HOME, POWER, volume and assistant remain system-managed. Consume the opening held-key repeats/release at modal boundaries; they must not trigger a short action in the new window. External-launch guards clear on resume. Restore native dialog focus only after window focus and placement are ready.

## Focus and navigation

Keep focus/press/tilt in per-card presentation state and apply graphics transforms over fixed native bounds. Home installed-app cards use immediate selected lift/release; press and directional tilt retain their existing local springs. Other shared artwork keeps its own motion contract. Reduced motion snaps and preserves a visible ring.

Selection settles for 160 ms before content/wallpaper coordination, cancelling obsolete selections. Catalog/content Up returns to the surviving dock item and reveals only what is necessary. A subsequent fresh Up enters the header and starts the interruptible Home return. Header Down cancels that return and restores the last surviving app. Initial placement and logical restoration do not replay the return animation.

Reorder and control move/resize animate only their explicit candidate geometry. Back or lifecycle stop cancels uncommitted organization edits. Normal card focus never resizes layout. Ambient photos advance at the existing 30-second interval with a short crossfade, snapping under reduced motion; Home has no permanent ambient loop.

Native input instrumentation and real-device captures are required for interaction claims. Controlled animation clocks do not measure physical remote-to-photon latency.
