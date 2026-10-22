# Contributor rules

Build an ordinary Android TV HOME application. Read `docs/PRODUCT.md`, `docs/DESIGN.md`, `docs/MOTION.md`, `docs/ARCHITECTURE.md` and `docs/TEST_PLAN.md` before changing behavior. Keep code and technical documentation in English; maintain English and French UI strings.

## Scope and device safety

Do not uninstall other launchers, clear user data, change default HOME, enable accessibility services, or alter system display/power settings without explicit authorization. Install and explicitly start this app for tests. Preserve a recovery route. Do not add network access, advertising, telemetry, mandatory accounts, root, overlays or privileged services. Do not commit credentials, signing keys, local SDK paths, device identifiers, personal captures or raw evidence. Public pushes and releases require explicit authorization.

## Architecture

Use `app`, `core:model/domain/data/platform/designsystem`, `feature:home/settings/profiles/search` and the separate `benchmark` module. Keep model/domain Android-free. Domain owns repository interfaces and source ports; data/platform own their implementations. Features do not depend on data implementations or one another. Only app assembles the isolated Koin container. Resolve dependencies at entry points/routes; inject interfaces into ViewModels, never Activity, Context or bitmap objects. Keep these rules enforced by `verifyArchitecture`.

State flows down and typed events flow up. Routes own lifecycle and Android effects; screens render supplied state/callbacks. Focus, press, tilt, scroll, captured frames and glass belong to presentation. Avoid a generic MVI framework or global mutable service locator. Use injected IO for blocking work and Default for ranking/pixel work. UI collects at STARTED; subscription-owned observers stop with zero timeout. Conflate invalidations, cancel obsolete reads and serialize accepted writes. Do not cancel an accepted transaction because another setting changed.

Preference scheduling allows one in-flight transaction and one latest pending snapshot per profile. Capture profile IDs, acknowledge revisions, retain failures for retry and flush before export/restore. Ordered workflows read committed snapshots rather than shared UI replay. Preserve stored IDs, formats, legacy readers and atomic restore/undo. Validate imported data and external destinations. Handle loading, empty, cached failure, removal, permission denial and recreation.

## UI and performance

Use TV Material3 and standard Foundation lazy layouts with stable keys/content types. Preserve the 12 MiB shared Coil cache, decode limits, rendering quality, fixed input bounds and reduced-motion paths. Keep per-card focus out of global Home state. Animations retarget and stop offscreen. No largeHeap, explicit garbage collection, wake locks or permanent idle animation. HOME, POWER, volume and assistant keys remain system-managed. Never relax a performance budget without evidence and an explicit decision.

## Verification

Keep changes scoped and describe observable acceptance. No parallel agents unless explicitly requested. Run the commands in `docs/TEST_PLAN.md`, including Spotless, architecture checks, module tests, lint and optimized compilation. Keep mechanical formatting separately reviewable. Do not suppress lint broadly or introduce type-resolved Detekt without confirmed toolchain compatibility. Use injected schedulers for concurrency tests. Physical-device evidence is required for focus/lifecycle/performance claims; label emulator evidence and record source, APK, device, SDK, scenario, compilation and cache state. Keep raw results outside tracked source. Do not equate compilation with acceptance or claim broad compatibility, security or smoothness from a narrow test.
