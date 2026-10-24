# Rendering exploration — 24 October 2026

## Recommendation

The strongest theoretical candidate is a shared prepared wallpaper field sampled directly by
each glass control, followed by a cheap neutral-interior path and an optical edge path.
Prioritize removing intermediate targets and redundant pixel work over recording arithmetic.
Bounded artwork retention addresses re-entry rather than all scrolling. A native scene renderer
offers more control if a production architecture change is eventually justified. These are
theoretical recommendations, not established speedups.

The direct prepared-field implementation was subsequently withdrawn: it produced opaque
black wallpaper-backed glass on the physical TV. The repair retains live mapped/Haze sources
and the independent neutral-interior, opacity, palette, blur, bounds and lookup changes. The
research below is rationale and alternatives, not evidence that the rejected snapshot design
works. The user has now authorized physical before/after testing; see [Performance](PERFORMANCE.md).

The objective remains the existing physical-device budgets in [Performance](PERFORMANCE.md):
smooth navigation, bounded memory, preserved appearance and responsive input. No quality,
frame-rate, cache or device-safety policy changes are proposed as an implicit shortcut.

## Evidence available now

The latest mapped-glass comparison uses the physical API 34/Mali-G52 TV, Balanced quality,
a 1920×1080 application surface and rapid vertical navigation. The matched B1/B2/A2 subset
reports CPU-frame p95 71.72 → 39.69 ms and application deadline misses 35.65% → 5.83%.
Actual app-start-to-presentation p95 is still 81.92 ms. Neither statistic measures physical
button-to-first-changed-pixel latency. The scene mismatch excludes A1 from the primary
comparison; these are not four matched ABBA blocks. See the optical-renderer timing section
of [Performance](PERFORMANCE.md) for identities, compilation and workload limitations.

Offline inspection of the six existing candidate iteration summaries finds 69 artwork
command captures per approximately seven-second burst, totaling 14.88–17.00 ms of inclusive
CPU-side recording time per burst. Mapped optical recording totals 24.98–27.40 ms per burst.
These traced sections exclude subsequent GPU execution and do not account for all framework
work. They cannot establish shader, upload or texture-cache cost. They do make a rewrite of
only those recording calculations a weak first candidate. Raw derived summaries remain private.

The current implementation already has:

- Foundation lazy layouts with stable keys/content types and 320 dp ahead/behind preparation.
- Independent offscreen card artwork at maximum focus scale; live reflection and transforms.
- A retained wallpaper group and separately prepared sharp/frost glass inputs.
- Geometry-baked optics generated on Default, with live source sampling through Haze.
- Optimized builds and packaged baseline profiles in the measured workload.

Larger lazy windows were already rejected on physical evidence. Warm horizontal artwork
recording was previously absent. Those findings must not be generalized to vertical re-entry
or to the current Balanced optical renderer's remaining GPU work.

## Ranked options

| Priority | Option | Work it could remove | Main uncertainty |
| --- | --- | --- | --- |
| 1 | Sample prepared wallpaper directly in AGSL | Per-control source capture and intermediate composition | Fidelity, transforms and preparation cost |
| 2 | Retain a bounded set of static card layers beyond lazy disposal | Re-entry allocation, recording and potentially raster preparation | Actual GPU residency and eviction behavior |
| 3 | Group static catalog visuals into bounded strips | Per-card submission and static draw commands | Focus extraction, invalidation and texture bandwidth |
| 4 | A native GPU scene behind the existing interaction layer | Parts of HWUI submission and render-target topology | Surface composition, driver support and duplicated ownership |
| 5 | C/Rust CPU kernels | Measured CPU arithmetic in preparation tasks | No demonstrated dominant native-kernel candidate today |

### Direct wallpaper sampling

`MappedGlassRenderer.draw()` currently records `drawInput()` into a graphics layer with a
runtime RenderEffect. `SplitWallpaperGlassSources` also retains separate filtered wallpaper
inputs. This is correct live capture, but it leaves an opportunity to remove an intermediate
stage when the source is fully known wallpaper.

Prototype a composition-owned prepared backdrop resource, with sharp/frost inputs and their
existing shading. A runtime shader samples those resources in wallpaper coordinates, blends
the current diffusion, then applies the existing optical map and rim. Scrolling updates the
local-to-wallpaper transform; it does not capture the wallpaper again for each control.
Use the same existing image/decode bounds and full-resolution optical output.

Android's [RuntimeShader](https://developer.android.com/reference/android/graphics/RuntimeShader)
accepts child shaders through `setInputShader`; numeric optical-map channels continue to use
`setInputBuffer`. This can be prototyped in Kotlin/AGSL with no JNI dependency. AGSL executes
inside Android's graphics pipeline; moving its host code to Rust does not change its shader.

The shallow 1.25 dp filter must be reproduced at the existing screen-space scale. Sampling the
original image directly would lose that filter; filtering at source resolution or substituting
a different kernel can change the result. Preparation may require a bounded GPU render target;
avoid per-frame bitmap readback or new duplicate CPU image caches. An immutable input resource
must not be mutated while any frame still consumes it.

Eligibility stays limited to known opaque wallpaper. Preserve Haze for unsupported inputs,
wallpaper crossfades, arbitrary live source content and failures. Validate Crop matrices,
Overview scaling, movement, edge padding, gradient/veil order, color space, premultiplied alpha,
replacement, resize, trim and reattachment. This removes capture topology only if the prototype
actually bypasses it; fewer Kotlin calls alone are insufficient evidence.

### Static card retention and strips

Keep Foundation responsible for layout, native focus, input and semantics. First try a
presentation-owned, byte-bounded set of retained card resources surviving nearby item disposal.
Own resources in the Home composition's GraphicsContext, release on eviction/trim/disposal,
and keep their lifetime independent of the logical app repository and ViewModels.

Key by artwork identity/version, raster size, theme, shape and static decoration. For cached
labels, also include text, locale, font scale, layout direction and typography. Invalidate on
artwork completion/error or changed content. A graphics-layer reference is not a promise that
the driver keeps its raster pixels resident. Count recordings and resource churn, then measure
frame and memory effects separately. Do not turn the 320/320 dp preparation policy into an
unbounded catalog cache.

Only screen row strips if individual resource retention still leaves substantial submission
cost. A strip must omit a focused card's base before drawing its independently transformed
artwork, or it exposes a duplicate underneath the focus lift. Editing, asynchronous artwork,
large text and changed column counts can invalidate strips. These costs can outweigh batching.

For scale, one 1920×256 RGBA8 strip is 1.875 MiB; three are 5.625 MiB before alignment, extra
targets and driver bookkeeping. One 1920×1080 RGBA8 target is 7.91 MiB. These are nominal
allocation calculations, not measured bandwidth or a proposed approved memory allowance.
The existing 12 MiB Coil cache is separate and remains unchanged.

### Native GPU renderer

The substantial native option is a different renderer: persistent textures/atlases, batched
quads, clipped visible regions, bounded uploads and explicit render targets. C++ or Rust
can host it; the potential gain comes from the rendering plan. A library that still submits
the same Canvas/RenderNode workload does not remove that workload.

Start with an isolated fixture containing the same wallpaper, moving dock, artwork sizes,
labels and focus effects. Keep native commands coarse: immutable scene updates plus camera
and focus parameters, with resources retained natively. Avoid per-card JNI calls per frame.
[Android's JNI guidance](https://developer.android.com/ndk/guides/jni-tips) calls out marshalling
cost and recommends a small interface. Rust can help express resource ownership; it is not
evidence of faster GPU execution than C++.

An embedded TextureView adds toolkit composition. A SurfaceView provides a separate composition
layer, but hardware-overlay use is device/composition dependent; clipping, transforms, translucent
Compose content and lifecycle still need verification. See [AOSP's surface comparison](https://source.android.com/docs/core/graphics/arch-tv).
Sampling an arbitrary Compose scene from a separate surface can reintroduce copies and
synchronization. Prefer a coherent renderer for the visual scene over several independently
animated surfaces that must agree every frame.

[Vulkan guidance](https://developer.android.com/games/develop/vulkan/native-engine-support)
identifies draw-call/driver overhead as an opportunity, and warns about older drivers and the
need for fallback coverage. Do not assume Vulkan is faster or reliable on this TV because the
GPU advertises it. Compare available OpenGL ES/Vulkan paths on the actual device before choosing.
[Frame pacing](https://developer.android.com/games/sdk/frame-pacing) is also necessary for a
custom render loop; faster submission can otherwise increase queueing and input latency.
Do not alter system display/power settings or add permanent idle rendering.

Replacing standard Foundation lazy rendering in production would require an explicit
architecture decision because the current contributor contract requires it. A native fixture
can test the performance ceiling without making that migration. Preserve Android focus,
semantics, large text, EN/FR, RTL, dialogs, recreation and external-return behavior in any
eventual hybrid design; do not count a noninteractive sprite demo as launcher acceptance.

### C/Rust CPU work

Use a native kernel only after identifying a dominant CPU-running function, such as a large
repeated image transform, and comparing an identical algorithm including JNI/upload cost.
The optical map already bakes asynchronously on geometry changes, not every scroll frame.
Native acceleration of that bake primarily targets preparation latency, not steady scrolling.
Rewriting repositories, coroutines, focus or preference handling has no current performance
justification. SIMD cannot remove GPU fill, offscreen targets or presentation queueing.

## Experiment sequence and decision gates

1. Preserve the current APK/source as the control. Use deterministic project-owned content for
   isolation, followed by the real configured scene. Keep motion, quality and material settings
   identical. Use diagnostic flat-glass/static-catalog fixtures separately to bound each
   subsystem's cost; they are not shippable quality reductions or additive time estimates.
2. Screen direct wallpaper sampling alone. Verify source-capture bypass, visual fidelity and
   resource bounds before timing. If it wins, independently confirm in alternating blocks with
   identical actual inputs, focus walks, scene and compilation/cache state.
3. Screen card-resource retention independently against that accepted renderer. Distinguish
   first entry, warm traversal, eviction/re-entry, reversal and dock arrival. Reject a cache
   that only moves the stall or grows memory across tours.
4. If render-thread/graphics cost still dominates, build the native scene fixture. Compare
   complete app-to-display timing and memory, not only its own draw-loop timer. It must offer
   enough demonstrated headroom to justify implementing the missing interaction contracts.

For every candidate, inspect CPU-frame p50/p95/p99, deadline overrun, application deadline
misses and presentation spacing. Measure input-to-first-changed-presented-frame separately.
Retain trace quality checks; inclusive thread slices are not isolated GPU timings. Sample PSS
and HWUI/renderer resources outside timed bursts, then background/resume and 10/30-minute
stress. Current hardware lacks isolated shader/bandwidth counters; keep that uncertainty.
Run all [build gates](TEST_PLAN.md) for implementation candidates, then physical-device focus,
lifecycle, fidelity and performance acceptance. Change one mechanism per comparison.

## Status at the research checkpoint

The initial exploration changed documentation only. It reused existing physical evidence and added
offline inspection; it does not install an APK, run a new benchmark, establish a speedup or
introduce a native dependency. The subsequent implementation is described in the handoff above;
the deployed application remains unchanged.

## Deeper theoretical audit

This section reviews the current source by subsystem. Source observations describe what the
code does; predicted benefits are conditional. Neither the exact remaining bottleneck nor a
particular frame-rate improvement can be established without new measurements, which are
outside the current authorization.

### 1. Frame cost and the limits of the scrolling-map analogy

Model steady throughput as the bottleneck among main-thread work, RenderThread/driver work
and GPU/compositor work, with synchronization and presentation deadlines. Input latency also
includes scheduling and queued frames. These costs overlap: adding thread durations or
subtracting CPU time from presentation duration does not isolate GPU execution.

A rolling catalog cache changes preparation frequency. It does not make the final frame
incremental: when the camera moves, nearly every viewport pixel changes. Already prepared
cards still require sampling, clipping and composition at their new positions. Copying the
previous framebuffer and drawing an entering strip also consumes bandwidth, complicates
fractional scrolling and interacts with shadows and translucent glass. It is not automatically
cheaper than drawing the prepared cards. Dirty rectangles are more attractive for stationary
focus-only changes than for a moving full viewport.

The best theoretical renderer minimizes both preparation work and the cost of drawing prepared
content. [Arm's render-pass guidance](https://documentation-service.arm.com/static/6560a5942c8b3557fee70a16)
explains why unnecessary intermediate targets and external-memory transfers matter on Mali.

One uncompressed full-surface RGBA8 write plus subsequent read at 1920×1080 is 16,588,800
bytes. Repeating that at 60 Hz is approximately 0.995 GB/s of nominal traffic. This is an
illustration, not this app's measured DRAM traffic: retention, compression, tile memory,
sampling and driver behavior change it substantially. A static retained target is not being
rewritten just because it is drawn again.

### 2. Wallpaper and depth blending

Source: `WallpaperBackground` in `core/designsystem/.../glass/GlassMaterial.kt` and
`AndroidWallpaperStore` in `core/platform/.../image/impl/AndroidWallpaperStore.kt`.

The visible wallpaper is already an offscreen group; its frost and veil animate independently.
The 192×108 broad frost is prepared on Default, outside drawing. Those decisions already avoid
repeated large CPU blur and retain expensive image/shading composition. Do not remove the root
offscreen group merely because offscreen rendering can be expensive: previous physical work
found it beneficial for this scene.

Theoretical improvement: describe wallpaper, frost, veil and gradient as one explicit field
with image identities and Crop transforms, rather than independently reconstructing equivalent
source trees for each consumer. Share immutable resources and animation parameters. During a
depth transition, a shader can compose the sources without a separate frost group-alpha target.
At stable endpoints, specialize away an unused source. Intermediate blur values such as .8
still require a blend; stable parameters can instead select a prepared stable composite if
its preparation and memory cost amortize. Avoid baking every animation fraction.

Do not casually move gradient application across blur: in general `blur(g * image)` differs
from `g * blur(image)` for a spatial gradient `g`. Preserve the existing source order, filter
kernel, density, color space and alpha convention. Wallpaper fusion is mainly a transition
and resource-sharing opportunity where the existing group already retains settled pixels.
[Skia runtime effects](https://skia.org/docs/user/sksl/) support composed child shaders; a
shader tree is not inherently one render pass per child.

### 3. Glass source acquisition: highest architectural opportunity

Source: `SplitWallpaperGlass.kt`, `MappedGlass.kt`, `MappedGlassRenderer.kt`.

The mapped renderer uses a padded source GraphicsLayer with a RuntimeShader RenderEffect;
its draw records current Haze input before drawing the result. Source capture preserves live
position and wallpaper changes. Its raster is control-sized plus padding, not necessarily
full-screen; the prepared sharp/frost source layers are root-sized. A recording marker does
not prove the GPU re-rasterizes every source instruction.

For eligible wallpaper-only controls, replace that indirection with a shader Paint drawing
the actual control shape into the destination. Its inputs are already prepared wallpaper
resources; uniforms carry the material-to-wallpaper transform, diffusion and intensity.
This can eliminate the per-control input RenderEffect target rather than only recording it
more quickly. Preparing the shallow filtered fields once is part of this design, not a new
per-frame CPU filter. Reuse across controls only when their filter/style keys match.

For source location `p`, the intended structure is `shade(field(T(p + bend(p))))`, with the
existing rim applied afterward. The local displacement must be transformed correctly by `T`;
adding a screen-space displacement to local coordinates is wrong under scale or rotation.
Refraction moves sample coordinates, so a cropped shared resource needs the union of all
possible sampled bounds, not merely visible control bounds. Include filter support and retain
original out-of-bounds sampling behavior.

Keep capture for unknown or live content. Only stop preparing the old path when no active
consumer needs it; the existence of a source modifier alone does not prove it performs wasted
GPU work. A direct path may trade a single blended source read for two sharp/frost reads at
intermediate diffusion. Account for that tradeoff rather than promising a free pass removal.
[RuntimeShader](https://developer.android.com/reference/android/graphics/RuntimeShader)
provides the required child-shader APIs without a native-language migration.

### 4. Optical shader: separate the neutral interior from the rim

Source: `GlassOpticalMap.kt` and `MAPPED_GLASS_SHADER`.

The bake drives displacement to zero beyond its edge band, and explicitly sets lighting to
zero there. The current shader nevertheless reads the numeric map and evaluates its optical
math for every covered fragment. Derive a geometrically guaranteed neutral region and use a
plain backdrop sample there. Reserve the optical-map path for the edge and rounded corners.

For a rectangular control of width `W`, height `H` and edge band `b`, the interior fraction
is approximately `max(W-2b,0) * max(H-2b,0) / (W*H)`, before rounded-corner adjustments.
For a hypothetical 1800×200 pixel control and 28 pixel band, it is 69.8%. That would exempt
roughly 70% of the area from map lookup and optical arithmetic, not from backdrop sampling,
and not remove 70% of the entire frame's work. Actual dimensions and density determine it.

A coherent shader branch is simpler; a geometric interior/rim split guarantees distinct
work coverage but adds draw calls and seam/antialiasing concerns. The compiler may flatten
a branch, and tiny controls may have no neutral interior. Preserve the sampled map's actual
zero region, including interpolation support, rather than assuming a center point is neutral.

Keep high precision for coordinates where needed; colors already use `half`. Compressing the
small map further or replacing multiplication with clever bit arithmetic has a weaker case.
Skia's [raw image shader guidance](https://skia.org/docs/user/sksl/#raw-image-shaders) also
supports preserving numeric map channels independently of color conversion.

### 5. Static card content and raster retention

Source: `FocusArtwork.kt`, `AppTile.kt`, `ContentCard.kt`.

The existing cache includes clip, surface, artwork, edge and shade at maximum focus scale.
Its outer layer isolates transforms, while reflection remains outside the static cache.
Additional row rasters would cache already cached content. They may reduce submissions but
also introduce another preparation pass and enlarge the sampled texture area with spacing.
Simple one-image cards can already be cheap to submit; complex multi-element cards have a
stronger flattening case.

A useful amortization model is: direct cost `N * D`; cached cost `K * P + N * Q + E`, where
`N` is displayed frames, `K` is rebuilds, `P` is preparation, `Q` is cached-quad drawing and
`E` is eviction/management cost. Caching wins only when `N*(D-Q) > K*P+E`. This scalar model
illustrates the tradeoff; actual CPU/GPU stages overlap. A larger cache can increase `Q` or
`E` through memory pressure, not only decrease `K`.

The targeted opportunity remains short-term re-entry retention with explicit byte accounting.
Use maximum-focus-resolution resources consistently to avoid quality switches or focus-entry
raster allocation. A strong layer reference does not guarantee raster residency; Android may
discard display lists no longer drawn. [RenderNode documentation](https://developer.android.com/reference/android/graphics/RenderNode)
describes that distinction. The existing 4 Mi-pixel per-card guard is not an aggregate cache
budget and must not become permission to retain many maximum-sized layers.

### 6. Focus, reflection and shadows

Source: `FocusArtwork.kt` and `AppTile.kt`.

Lift/press/tilt already read animation values in the graphics layer, and shader geometry is
cached by native size. Preserve those phase boundaries and the fixed input rectangle. A single
global focused-card overlay would need to preserve outgoing spring releases, z order,
clipping and coordinate transforms; it is not a free replacement for local focus state.

The shadow still varies through `shadowElevation`. A reusable nine-patch shadow outside the
artwork cache could substitute bounded textured drawing for native elevation work. However,
native shadows can already reuse masks, and a fixed texture does not reproduce every elevation,
tilt or lighting condition. It is a plausible appearance-changing option, not an equivalent
guaranteed improvement. Keep it below capture elimination in priority.

Reflections are radial gradients drawn only above a small lift threshold. A shared reflection
asset can replace gradient evaluation, but adds a texture read and must handle artwork-derived
color. Texture lookup is not automatically cheaper than this simple shader. Avoid a new
per-frame offscreen target merely to cache the dynamic reflection. Continue suppressing idle
loops and respecting reduced motion; do not shorten accepted animations as a hidden shortcut.

### 7. Artwork decoding, uploads and palette extraction

Source: `AppTileArtwork`, `ArtworkReflections`, shared loader in `LauncherModule.kt`.

Requests are bounded and shared; motion-enabled artwork requests CPU-readable pixels for
palette sampling. `ArtworkReflections.intercept()` waits for palette preparation under a
mutex before returning the successful image. Thus a small decorative result is on the image
delivery path even though its work runs on Default. The sampling loop makes 144 `getPixel`
calls per uncached palette. It is bounded, but unrelated requests can wait for that mutex.

Theoretical improvement: deliver the decoded image independently; compute the same palette
as metadata and publish a tagged optional result. Start with the existing neutral reflection,
then update only its color. Preserve generation checks, request identities, cancellation and
bounded scratch ownership. It changes the transient reflection timing, so it needs an explicit
appearance contract if implemented. Batching CPU pixel access could also reduce JNI overhead,
but copying an entire bitmap for 144 samples can cost more than those calls.

This does not justify blindly enabling hardware bitmaps: metadata extraction could then
require a costly readback/copy. Nor does it justify decoding a second full image. Existing
palette hits still need a consistent resource policy across image and metadata eviction.

[Bitmap.prepareToDraw](https://developer.android.com/reference/android/graphics/Bitmap#prepareToDraw())
can initiate upload before first use, but cannot promise permanent residency. Existing project
inspection already found Coil's upload preparation; adding another warming loop duplicates
work without demonstrating earlier useful availability. Precomposition, decode completion,
texture upload and final offscreen card rasterization are four different readiness states.

### 8. Lazy layout, geometry observation and scroll ownership

Source: `HomeFocusState.kt`, `HomeScreen.kt`, `AppTile.kt`.

Home already derives the raised-state Boolean and column count rather than observing the
whole changing scroll layout in composition. Catalog projection is remembered from catalog
and organization inputs. Foundation already reuses lazy items and places retained drawing
nodes as scrolling changes; a camera analogy alone does not establish extra recomposition.

The 320/320 dp cache is a bounded preparation window. Predicting useful work by direction,
velocity and estimated preparation time is theoretically better than enlarging both sides:
lookahead distance is approximately `speed * preparation latency + margin`. Rapid reversals
require a small protected reverse buffer and cancellation of obsolete reads. Accepted writes
remain serialized and cannot be cancelled by scrolling. This is a scheduling idea, not a
reason to replace Foundation's existing prefetch scheduler or flood RenderThread uploads.

Each AppTile currently observes global bounds during movement. Keep launch geometry live,
but separate it from the Boolean visibility needed only for focused loading feedback. Avoid
unnecessary snapshot-backed writes if a value is only read inside input callbacks. Do not
claim those writes currently recompose every card: observation scope determines that.
[onLayoutRectChanged](https://developer.android.com/reference/kotlin/androidx/compose/ui/layout/onLayoutRectChanged.modifier)
offers specialized tracking, but its default debounce can make launch bounds stale during
scroll. Exact geometry needs zero-delay delivery or resolution at activation time.

`focusApp()` also waits a frame before requester lookup to allow relocation. An identity-aware
fast path for an already correctly registered target could reduce some focus-restoration
latency. It would not reduce GPU frame cost and must preserve relocation, cancellation and
fallback behavior. Multiple scroll owners must still retarget rather than queue independent
motions; removing the existing handoff guards is not a performance optimization.

### 9. Catalog, data coordination and text

Source: `HomeViewModel.kt`, `ProjectHomeUseCase.kt`, `HomeContinuationRow.kt`, `HomeClock.kt`.

Repository invalidations already conflate and obsolete reads cancel. Scroll does not require
repository writes or catalog reloads. The remembered Home projection already uses a set for
occupied app IDs. Preserve these boundaries rather than introducing a native business layer.

Continuation cards search the app list by package during item composition. A remembered
package map would replace repeated linear scans with lookups. The row is capped and this is
primarily catalog-arrival/composition work, so theoretical gain is small. Similar immutable
indexes can aid restoration without making per-card focus global.

Text layout and glyph reuse belong to the toolkit. Labels outside the artwork cache are not
therefore rebuilt every scroll frame. An extra text atlas is most relevant to a native renderer;
in Compose it can duplicate existing caching and harm locale/font-scale/accessibility behavior.
The clock already wakes at minute boundaries only while STARTED. No clock optimization is
likely to explain continuous scroll cost. Baseline profiles and optimized builds already
address managed-code preparation; neither removes GPU sampling or composition work.

### 10. Native renderer: the highest-control architecture

If production were rebuilt for rendering control, use one coherent visual surface with
persistent artwork pages, shared backdrop fields, batched card geometry, bounded upload
commands, and camera/focus uniforms. Retain textures until their last GPU use completes.
Preserve visual ordering: sorting translucent objects by texture can produce incorrect pixels.
Atlas padding must prevent filtering into neighboring images; dynamic updates must not race
frames sampling the same page. Atlas memory is an explicit GPU cost, not free reuse of Coil.

[Arm's GPU guide](https://documentation-service.arm.com/static/67a62b17091bfc3e0a947695)
supports batching and minimizing state changes. Those mechanisms require a renderer; copying
Kotlin code into Rust without changing submissions does not obtain them. Using embedded Skia
also does not inherently remove HWUI-like render-target and shader costs.

Keep Android interaction and semantics authoritative. A parallel invisible full Compose scene
can duplicate layout work and create visual/focus drift; the interaction geometry must come
from the same scene description as the renderer. Text shaping, large text, RTL, localization,
dialogs and external-return behavior remain production requirements. This would be an explicit
architecture departure from the current standard-Foundation contract.

Prefer a single output surface for a coherent moving scene. Splitting wallpaper, cards and
focus into independent surfaces adds fences, composition layers and alignment constraints.
A SurfaceView does not guarantee hardware-overlay composition under translucent UI;
TextureView introduces toolkit composition. [AOSP describes those differences](https://source.android.com/docs/core/graphics/arch-tv).
Frame pacing needs to sample current input and avoid queue stuffing; a custom loop must stop
when idle/hidden. More buffered frames can increase latency despite good draw-loop throughput.
See [Android frame pacing](https://developer.android.com/games/sdk/frame-pacing).

Vulkan offers explicit submissions/attachments; OpenGL ES may have a more mature path on a
particular TV driver. Neither language choice nor advertised API support establishes a winner.
Use C++ for direct NDK ecosystem integration or Rust for a deliberately owned native resource
model; both require careful FFI, synchronization and lifecycle design. Do not invoke native
code separately for every card or pixel.

### 11. Preparation kernels, memory and lifecycle

The small CPU wallpaper blur uses repeated neighborhood sums. A sliding running sum can reduce
each pass from `O(width*height*radius)` to `O(width*height)`, retaining edge clamping and integer
rounding after each pass. That is a real algorithmic opportunity in Kotlin before SIMD/native
code. Here the image is only 192×108 and radius is two; it affects preparation, not every frame.
The optical map is likewise geometry-baked once. Neither deserves priority over repeated
source composition without a specific startup/preparation objective.

Treat resources as separate classes: decoded images, card rasters, backdrop fields, optical
maps, temporary targets and submitted frames. Count both steady residency and crossfade peaks.
Two simultaneously valid wallpaper generations need lifetimes until their last use, even
after logical cancellation. A cache must release on trim/hidden/disposal and restore lazily.
Do not replace a bounded cache with a permanent global atlas or force garbage collection.

For Kotlin-owned GPU preparation, [HardwareBufferRenderer](https://developer.android.com/reference/android/graphics/HardwareBufferRenderer)
is available from API 34, but shares the application's render thread/resources. Moving its
invocation to a worker does not create a separate GPU or guarantee it cannot delay UI frames.
Older supported APIs need an appropriate alternative/fallback. Asynchronous preparation must
publish readiness without a UI-thread fence wait; it cannot silently lower rendering quality.

### 12. Final theoretical ranking

| Candidate | Reason it ranks here | What remains unknown |
| --- | --- | --- |
| Shared backdrop fields + direct glass drawing | Removes a whole capture/filter-composition dependency for known inputs | Actual saved target work versus added texture reads |
| Neutral-interior optical bypass | Avoids map sampling/math for a provably unaffected region | GPU branching, added draw overhead and area on real controls |
| Bounded short-term card resource retention | Avoids work on genuine vertical re-entry | Driver eviction and aggregate memory cost |
| Independent artwork/palette delivery | Removes decorative metadata from image critical path | Practical latency gain and transient color behavior |
| Fused wallpaper depth composition | Avoids some transition intermediates and duplicate source structure | Existing retention already amortizes settled work |
| Bounds/visibility tracking and indexes | Reduces some CPU callbacks/lookups | Small share of the critical frame path |
| Batched native scene renderer | Maximum control over submissions, targets and pacing | Large integration cost; no device-specific speed guarantee |
| Shadow assets / strips / SIMD kernels | Conditional specialized savings | Fidelity, bandwidth and rebuild cost may dominate |

The theoretical end state is a small dependency graph: prepare immutable visual resources when
their content changes; update only camera, focus and depth parameters during motion; draw the
final visible scene with the fewest necessary targets and samples. Retain standard Foundation
interaction and the current renderer unless a separate architecture decision selects native
ownership. No percentage FPS improvement or 60 Hz attainment is asserted by this analysis.
