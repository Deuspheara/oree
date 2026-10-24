# Rendering notes for contributors

For a plain-language explanation of the six slider levels, see [Visual quality and performance](PERFORMANCE.md). This page describes the current rendering constraints, rather than the history of experiments.

## Current paths

- **Usual glass:** bounded prepared wallpaper inputs and precomputed optical geometry can reuse rendering work. Unsupported sizes, translucent inputs and unavailable prepared data fall back to the original Haze path.
- **Live Liquid Glass:** uses Haze Quality, bypasses prepared optical maps and retained menu images, and keeps card motion during repeated navigation. Accessibility and Android-version fallbacks remain active.
- **Menus:** settled optical pixels may be retained in the usual modes. Foreground rows, input and focus remain live. Changes to source, geometry, density or material invalidate the retained image.
- **Profile circle:** the app root owns a bounded Activity-window copy while the native profile dialog is visible and STARTED. It includes foreground cards, excludes the separate dialog, and is cleared on close, stop or profile change. The copy feeds Haze without repainting the scene.
- **Artwork:** static card pixels can reuse a bounded GPU layer. Reflection and transforms remain independent, with stable native input bounds.

## Preserve these limits

Keep the shared 12 MiB Coil memory cache, existing request/decode bounds, filtering quality, cancellation and reduced-motion paths. Do not replace live/source-backed output with opaque or stale pixels to improve a timing number.

Optical-map sampling must stay inside the supported material bounds, including bilinear filtering and nine-patch stitching. Tiny/unsupported materials retain their fallback. `GlassOpticalMapTest` covers the numeric bounds; physical rendering fixtures cover source freshness and appearance.

A cheaper shader or fewer draw operations do not by themselves prove a faster complete frame. Keep only changes supported by matched optimized physical comparisons, with the same scene and input delivery. See [measurement rules and targets](development/PERFORMANCE.md) and [Testing](TEST_PLAN.md).

Older investigations, rejected renderers and detailed measurements remain in Git history. They are historical evidence, not ratings for today's slider presets.
