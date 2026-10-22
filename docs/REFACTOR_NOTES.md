# Application architecture refactor

## Review and implementation scope

| Area | Result |
|---|---|
| App entry points, navigation and DI | Kept isolated app-owned Koin assembly and saveable nested navigation. Initialization retries share one in-flight job. Profile-tagged accepted writes serialize and retain results after a route waiter disappears. Android effects stay at entry points/routes. |
| Home, folders and organization | Replaced the callback-heavy screen API with scoped actions and available controls; extracted header rendering and transient focus ownership. Kept projection, lazy keys, fixed bounds, settled artwork selection and local card motion. Organization progress/failures live in the ViewModel. Removed unused screen inputs. |
| Settings, commands and diagnostics | Separated category rendering and small content/backup/ambient contracts. Backup preparation/export now belong to SettingsViewModel, with flush barriers, committed snapshot review, duplicate guards and retained failure. Restore has one explicit status. The selected category survives recreation. Command move/resize and diagnostic rendering remain local presentation responsibilities. |
| Profiles, avatars, wallpaper and gallery | Added ProfilesRoute for lifecycle-aware observation/completion acknowledgment and an explicit edit status. Kept captured saveable editor drafts and existing photo permission/gallery route boundaries. Wallpaper import has one status and preserves its captured profile identity. |
| Search | Kept injected Default ranking and zero-timeout subscription ownership. Results identify their producing request; the route rejects stale replay for another request. Input/results retain local focus and scroll ownership. |
| Domain, data and platform | Retained repository/source boundaries, meaningful orchestration use cases, committed reads, DataStore identifiers/codecs/legacy readers, atomic restore/undo, bounded preference scheduling and cancellation-aware providers. These contracts already had deterministic coverage and did not justify a persistence rewrite. |
| Shared UI and build rules | Added a cancellable, bounded lazy requester-registration helper with surviving-control fallbacks. Strengthened verifyArchitecture to reject Compose/Koin imports in ViewModels. Retained existing theme/glass/artwork/image pipelines and benchmark separation. |

The conventions are in [Architecture](ARCHITECTURE.md); regression coverage and mandatory commands
are in [Testing](TEST_PLAN.md). No new runtime dependencies, permissions, strings, storage formats
or visual design were introduced. English/French resources and persistence compatibility remain
unchanged.

## Observable corrections

- An absent lazy focus target no longer suspends a restoration coroutine indefinitely.
- Re-entering Search waits for the current request rather than restoring from another request's
  cached result set.
- Accepted organization/profile writes and backup work retain progress/failure when the original
  route disappears; overlapping accepted writes serialize.
- Repeated initialization retry does not start duplicate initialization reads.
- Disabling Ambient photos clears the route's previous photo set and stops its slideshow work.
- Settings recreation restores the selected category and its entry focus target.

## Verification and limits

- All mandatory commands in TEST_PLAN passed: Spotless, architecture, module/app unit tests,
  lint, debug and optimized compilation, instrumentation/benchmark APK compilation and avatar
  verification. A lint worker initially crashed internally; the successful rerun used one worker
  without suppressing checks.
- 59 distinct debug/JVM unit tests passed. The full Gradle test gate also exercised configured
  build variants.
- On a physical API 34 TV in French, 91 distinct native tests passed across Home, folders,
  favorites, app menus, Settings, Profiles, Search, commands, Ambient, photo permissions,
  backup validation/restore/undo and Activity recreation. One real-photo prerequisite case was
  skipped because user-authorized fixture photos were unavailable. The lifecycle test's
  hardcoded English query was corrected to use the localized action label, then passed.
- Existing HOME and accessibility launcher settings were preserved. The app package was absent
  before this session's explicit install. An Android CLI inspection server initially occupied
  UiAutomation; only that test helper was stopped before direct instrumentation.
- Compose compiler reports still mark HomeScreen, HomeHeader and SettingsScreen skippable; no
  stability annotations were added. This is compiler evidence, not a runtime improvement claim.

Raw logs, device information, APKs, source/APK hashes and captures stay outside tracked source.
The temporary emulator was stopped and its newly created disposable AVD removed; native results
above come from the real TV.

This is an architectural change, not measured performance acceptance. Cache/decode limits,
rendering quality, reduced-motion paths and physical budgets were not relaxed. Physical-device
focus, lifecycle, accessibility and remote-to-photon acceptance still require the protocol in
[Performance](PERFORMANCE.md). Generated profiles must be recollected after changed Compose
signatures; old rule presence alone cannot establish current-path AOT coverage. All three
source/APK-verified captures converged on the real TV: startup at iteration 14, catalog at
iteration 15, and secondary screens at iteration 8. An interrupted catalog attempt was retained
separately; restarting the local ADB server restored transport and the complete retry passed.
The repository's three-journey guard verified the successful results, convergence logs, source
and APK identity, and profile checksums before merging the generated profiles. Startup rules
come only from the startup capture. Required-class presence and startup inclusion checks passed.
These results establish executed-path profile inclusion, not latency or broad AOT coverage.
The optimized benchmarkRelease APK was rebuilt with these profiles, replace-installed without
clearing app data, and explicitly launched. Its installed checksum matched the build. The final
`LauncherJourneySmokeTests.secondaryJourneysRestoreTheirState` passed with compilation preserved
and existing caches/data; its single timing sample is not a performance acceptance result. The
TV's original HOME selection and accessibility service were verified unchanged afterward.

`LauncherRoot` still coordinates many Android effects and nested panels. Smaller permission and
external-launch routes are a possible later improvement once their device journeys are covered;
this pass avoids changing modal ordering or introducing a global navigation/action framework.
