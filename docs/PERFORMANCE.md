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
