# Architecture

The namespace and application ID are `fr.deuspheara.oree`.

## Modules

| Module | Owns | Production dependencies |
|---|---|---|
| `app` | Application/Activity/Dream entry points, DI assembly, navigation, Android results/effects | Core and feature APIs |
| `core:model` | Android-free profiles, preferences, app/content identities and pure validation | Kotlin/JDK |
| `core:domain` | Repository interfaces, source ports, dispatchers/clock and use cases | Model, coroutines |
| `core:data` | Repository implementations, DataStore transactions, codecs, migration and backup | Model/domain |
| `core:platform` | PackageManager/providers, destination validation and Android image adapters | Model/domain |
| `core:designsystem` | Shared resources, theme, controls, focus, artwork and glass | Model/domain |
| `feature:home` | Catalog, favorites, folders, reorder and app actions | Allowed core APIs |
| `feature:settings` | Settings, commands, diagnostics, backup and ambient presentation | Allowed core APIs |
| `feature:profiles` | Profile editor, avatars, wallpaper and gallery | Allowed core APIs |
| `feature:search` | Local ranking and search presentation | Allowed core APIs |
| `benchmark` | Optimized measurements and baseline-profile journeys | Separate instrumentation process |

Features cannot depend on data implementations or one another. Their platform dependencies are presentation APIs, not source/implementation packages. `build-logic` shares JVM, Android-library and Compose conventions. `verifyArchitecture` enforces module direction, package/path layout and portable ViewModel/domain boundaries.

## Interfaces and ownership

Each repository interface lives in `core:domain/repository`; low-level adapter contracts live in `core:domain/source`. For example, `ProfileRepository` and `OrganizationRepository` are atomically implemented by `DataStoreProfileRepository`, while `AppCatalogDataSource` is implemented by `AndroidAppCatalogSource` and consumed by `DefaultAppRepository`.

Data separates `repository`, `datastore`, `serialization` and `migration`. Platform separates `datasource`, `image/impl`, `image/processing`, `launch` and `status`. Features separate presentation ViewModels, routes, screens, components and local state as needed. Avoid empty pass-through layers.

Koin 4.2.2 is BOM-pinned with classic/constructor DSL and no compiler/KSP plugin. `LauncherApplication` owns an isolated container. Repositories, DataStore and the 12 MiB Coil loader have application lifetime; external launch guards are Activity-owned. Resolve at entry points/routes and inject domain ports into ViewModels. Graph verification and construction tests cover every ViewModel.

## State and concurrency

Feature ViewModels expose immutable StateFlow state and typed operations. Routes collect with `collectAsStateWithLifecycle`; screens receive state/callbacks. Focus, press, tilt, scroll, Haze objects, captured frames and bitmaps remain presentation-owned. Navigation uses a saveable holder for nested panels/return targets. Only settled app selection reaches content coordination after 160 ms.

Package/provider observers use subscription ownership with zero stop timeout. Feed and wallpaper share one bounded snapshot per enabled source; preview-only mode does not activate Watch Next. Package filtering precedes presentation limits. Invalidations conflate, obsolete reads cancel, Android query cancellation reaches CancellationSignal, and refresh failures preserve cached content with explicit availability.

Inject IO and Default dispatchers for blocking work and ranking/pixel calculations. Serialize writes; a later setting must not cancel an accepted transaction. Initialization, Home projection, wallpaper import/cleanup, profile deletion/cleanup and backup restore are use cases. Simple typed mutations stay repository operations.

## Presentation contracts

- Home and Settings dispatch feature-scoped `HomeAction` / `SettingsAction` intentions. Their
  control sets describe available affordances independently from event delivery. Reusable cards,
  form fields and controls keep small local callbacks. App handles cross-feature navigation and
  Android results; features never resolve another feature's ViewModel.
- `HomeUiState` owns catalog loading/cached failure. Organization and launcher writes expose
  separate profile-tagged operation state, with mutually exclusive saving/completed/failed
  statuses. Routes may disappear while accepted writes finish; new waiters must not cancel them.
  Mutexes serialize accepted writes and failures remain observable without the original waiter.
- Settings keeps preferences, backup preparation and restore progress separate. Backup commands
  capture document strings, survive route recreation, reject overlapping work, flush preferences
  and read the committed profile snapshot for review. Export completion is acknowledged after its
  route displays feedback. Restore still uses the atomic reviewed-snapshot/undo contract.
- `ProfilesRoute` observes/acknowledges edit completion; `ProfilePanel` owns its saveable captured
  editor ID, name and avatar draft. Profile edit, restore and wallpaper import statuses cannot
  describe saving and completion simultaneously.
- Search results carry the request that produced them. `SearchRoute` only presents a result set
  matching its current request; replay from another query/catalog cannot restore stale focus.
  Ranking stays on injected Default and stops with zero subscribers.
- `HomeFocusState` owns requesters, lazy states, directional motion and the current restoration
  job. Saveable selected IDs/indices remain local to Home. Job identity is not rendered state.
  Home, folders and Search wait at most one second for lazy requester registration, preserve
  caller cancellation and fall back to a surviving header/form/query control when absent. This
  timeout is a liveness bound, not an input-latency budget or performance measurement.
- Settings renders category sections with their own inputs. Content, backup and ambient data
  have independent small contracts rather than an application-wide state bag. `AmbientRoute`
  owns optional lifecycle-bound photo IO; its screen owns slideshow and input presentation.

Do not introduce base ViewModels, reducers or application-wide generic operation runners. Use cases remain for
business orchestration already shared across callers; simple mutations stay repository methods.
Only deliberately accepted writes should use a ViewModel-owned `async` with an awaited result:
this preserves commit ownership and delivers failure to a waiting route. Reads normally retain
caller cancellation. `verifyArchitecture` rejects Compose and Koin imports in ViewModel sources.

## Persistence

DataStore owns bounded configuration; Room is deferred until relational queries or partial updates justify it. Keep DataStore names/keys, profile schema versions, backup format 1 and legacy readers compatible. The backup reader accepts the legacy exporter discriminator intentionally.

Preferences allow one in-flight transaction and one latest pending snapshot per captured profile ID. Revision-aware acknowledgements preserve newer drafts; failures remain available for retry. Export/restore wait for a flush barrier. Shared UI observations may lag a commit, so ordered workflows use `ProfileRepository.readSnapshot()`. A successful committed read restarts a failed observation. Restore blocks editing, compares the reviewed snapshot and clears optimistic state only after atomic commit. Undo is part of the same storage contract.

Changing Android application ID creates a separate sandbox; it does not transfer another package's private files. Configuration import is explicit and images remain references.

## Build quality

Spotless/ktlint and `.editorconfig` define formatting. Android Lint is mandatory. Compose compiler reports are generated per Compose module; use them before adding stability annotations. Type-resolved Detekt is deferred until compatibility with the pinned Kotlin toolchain is established. Preserve rendering, cache/decode limits, input bounds and reduced-motion behavior; DI/module extraction has no assumed performance benefit.

Settings Appearance's presentation-only lazy builder emits stable row keys. Focus memory
maps its saved row to that item order, reveals it and uses the shared bounded registration
wait before requesting focus. This changes no preference schema or write scheduling. Home's
navigation quiet-period job belongs to HomeFocusState; ordinary D-pad events do not increment
an observable screen-wide timer counter.

Settings retains the Home composition under its own screen instead of disposing/rebuilding
its focus graph on every visit. Covered Home is transparent, excluded from semantics and
focus, and suspends its clock, selection preview and restoration effects. Its Back handlers
are disabled until revealed. Closing Settings restores the captured Home destination. The
retained subtree keeps a stable material policy; Settings still owns its opaque panel policy.
Home also guards focus-memory callbacks with native window ownership. Covered-window
callbacks cannot replace the saved header/card destination, and restoration waits for the
parent window to regain focus. This applies to Control Center and folder/overview dialogs as
well as Settings return.
The opaque-panel composition local tracks its readers because navigation changes it. A static
local would invalidate the entire launcher scene when Settings opens. Focus callbacks read
live native-window ownership rather than retaining a window-focus Boolean from the previous
composition, so ownership changes between frames cannot rewrite Home focus memory.
Covered-state guards also use a retained current-state reference: focus modifier updates can
emit callbacks before child callbacks have been replaced during applyChanges. Continuation
position memory is separate from active row ownership. Captured header restoration takes
precedence over provider invalidations while revealing Home.

Visual setup is presentation-owned draft state with typed ExperienceAction events. The app
root gates welcome on committed profile preferences and submits completion through the
existing profile-scoped accepted PreferenceWriter. Optional setupComplete and motionIntensity
fields preserve schema/version compatibility: absent legacy fields decode to completed setup
and full motion. Imported values receive the same strict type/range validation as other
appearance fields. Settings category focus/viewport survive one-column navigation in the
existing saveable scopes; requester registration remains bounded and cancellable.

The app composition owns `SplitWallpaperGlass` beside its original Haze source. For bounded
opaque wallpapers, separate stationary sharp/frost filters feed a blended source for clear
controls; their refraction and lighting retain the current control geometry. Source identity,
opacity, sampling policy and the wallpaper transition govern eligibility. The original input
remains selected during wallpaper crossfades and for other materials. Haze/Compose own the
graphics layers and their disposal; no pixels enter ViewModels, saved state or a disk cache.

Eligible clear controls can replace Haze's optical stage through its public custom-effect API.
Composition owns a cancellable, geometry-keyed optical-map bake on Default; each effect node
owns its shader and bounded input layer. The bounded nine-patch map contains displacement and
fixed rim lighting, while Haze keeps live source positioning/invalidation. Pending, unsupported
or failed optical maps retain the ordinary Haze path; all bitmap state stays in presentation.

The optical map displaces samples inward. Materials whose minimum extent is at least twice
(maximum displacement + 2 pixels) use their exact drawing bounds for input/output, rather than
an unnecessary outside sampling border. Smaller materials retain the original padded path.
The bilinear map-sampling property test checks this invariant at maximum intensity; lower
intensities interpolate toward the already in-bounds original position. The map, live source,
optical shader, sampling resolution and original allocation eligibility guard remain unchanged.

The experimental immutable-field/direct-sampling path was withdrawn after a physical-TV
regression: wallpaper-backed dock/header controls rendered black in transparent mode. The
production path keeps Haze-owned live input and the existing split filters; it does not freeze
those hidden source trees into snapshots. The optical shader retains a conservative neutral
interior bypass, accounting for rounded corners and filtering support. Single-bitmap visible
frost applies opacity directly to its draw; multi-draw filtered sources retain group compositing.

Artwork delivery is independent of optional reflection metadata. The platform palette retains
its 64-entry metadata cache and serial scratch ownership, with at most eight queued/running
decoded-image references. Generation-tagged completion cannot repopulate a cleared cache.
Home observes optional completion through an Android-free suspending palette method while
STARTED; cancellation of an observation does not cancel another card's shared preparation.

`LauncherPreferences.liveGlass` is an optional profile/backup Boolean, defaulting to false for legacy data. The existing schema and IDs are preserved. The final visual preset enables it; lower presets disable it. App provides a presentation local consumed by glass and focus drawing; ViewModels remain independent of rendering APIs. Live mode bypasses precomputed optics, prepared sharp/frost inputs and retained menu images, while preserving opaque/accessibility fallbacks.
