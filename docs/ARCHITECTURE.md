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
