# Content capabilities

App-only Home works without provider or photo permissions. Content integrations are optional and use public Android APIs; they do not expose a universal catalog or global viewing history.

## Sources

Watch Next and Preview Programs have independent enablement and source-specific loading, empty, denied, unsupported and error states. Watch Next wallpaper and feed share the same bounded source snapshot. Preview-only mode must not query Watch Next. Filter by the selected package before wallpaper/presentation limits; changing focus does not start another source query.

Only foreground subscriptions register observers. Invalidation bursts conflate, obsolete reads cancel through CancellationSignal, and failed refreshes retain cached entries with explicit source status. Returning to foreground refreshes missed changes. Do not add polling or keepalive work.

## Photos and external destinations

Local search can use already accessible photo names without requesting access. The gallery requests image access only when opened, with the system photo picker as an alternative. Import validates/bounds image decoding; unavailable images fall back locally. A rejected permission must not block the launcher.

Public content destinations, system search and input viewers remain external Android launches. Validate URI schemes and component resolution, catch missing/security failures and retain the launcher return target. Provider discovery is separate from actual playback/viewer success on a firmware.

Diagnostics report observed availability. They must not imply access to another app's private database, account, playback progress or active audio route. See [Android compatibility](ANDROID_COMPATIBILITY.md) and [Testing](TEST_PLAN.md).
