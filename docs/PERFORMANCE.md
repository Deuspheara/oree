# Visual quality and performance

The Visual experience slider changes how much animation, glass and background blur Oree draws. More effects ask more of your TV's graphics hardware.

Open **Settings → Appearance → Visual experience**. Press Left/Right to preview, then choose **Apply this experience**. In first-time setup, choose **Finish**. Previewing alone does not save a change.

## The six levels

| Level | Appearance and movement | Expected graphics workload |
| --- | --- | --- |
| **1. Simple** | Solid surfaces. No glass or background blur. Focus uses a visible outline instead of animated lift. | **Lowest**—the fewest visual effects. |
| **2. Light** | Gentle card movement with solid surfaces. Glass and background blur stay off. | **Low**—adds motion without glass effects. |
| **3. Balanced** | Softer card movement, lighter glass and some background blur. | **Moderate**—a starting point if you want both motion and glass. |
| **4. Rich** | Full card movement and reflections, stronger glass and more background blur. | **High**—more visual detail than Balanced. |
| **5. Full effect** | Full card movement with maximum glass and background-blur strength. Prepared effects are reused where supported. | **Higher**—the strongest effects on the usual rendering path. |
| **6. Live Liquid Glass** | Maximum-strength glass using Haze's Quality mode. Glass is rendered live, and card animations continue during fast, held-arrow navigation. | **Highest**—may make scrolling less smooth on a busy or less powerful TV. |

“Workload” describes the requested effects. It is not a measured speed score: FPS has not been compared for every level under matching conditions on the same TV. Catalog size, artwork loading and the TV itself also affect responsiveness.

## Which should I choose?

Start with **Balanced**, then scroll through your normal apps and Continue Watching row. If movement feels uneven, try **Light** or **Simple**. If you prefer stronger effects, try **Rich**, then **Full effect**. Choose **Live Liquid Glass** when you want the richest glass and continuous focus animation, accepting its higher graphics cost.

**Full effect versus Live Liquid Glass:** both use maximum effect strength. Full effect can reuse prepared rendering work and temporarily simplify card focus while you hold an arrow key. Live Liquid Glass uses the live Quality renderer and keeps those card animations running.

A higher level changes appearance; it does not make apps launch faster or improve video quality inside streaming apps.

## Other settings that affect the result

- **Reduced motion:** limits animation even at a high slider level. The system's reduced-motion setting also takes priority.
- **Reduced transparency or high contrast:** uses solid materials where needed for readability.
- **Glass disabled:** removes the glass effect independently of the slider.
- **Android version:** the current glass effect requires Android 13 / API 33 or later. Older supported Android versions use solid surfaces.

Settings are saved per local profile. Separate appearance controls can produce a custom combination, so the slider may show the nearest preset.

## What has been checked?

Focus, transparency and profile behavior have targeted physical-TV tests on API 34. Those checks do not establish a guaranteed frame rate. The project's warm-navigation performance target is still unmet; no preset has a certified 30 or 60 FPS rating.

For contributors: [measurement method and unchanged performance targets](development/PERFORMANCE.md).
