# Content capabilities

App-only Home works without provider or photo permissions. Content integrations are optional and use public Android APIs; they do not expose a universal catalog or global viewing history.

## Sources

Watch Next and Preview Programs have independent enablement and source-specific loading, empty, denied, unsupported and error states. Watch Next wallpaper and feed share the same bounded source snapshot. Preview-only mode must not query Watch Next. Filter by the selected package before wallpaper/presentation limits; changing focus does not start another source query.

Only foreground subscriptions register observers. Invalidation bursts conflate, obsolete reads cancel through CancellationSignal, and failed refreshes retain cached entries with explicit source status. Returning to foreground refreshes missed changes. Do not add polling or keepalive work.

## Photos and external destinations

Local search can use already accessible photo names without requesting access. The gallery requests image access only when opened, with the system photo picker as an alternative. Import validates/bounds image decoding; unavailable images fall back locally. A rejected permission must not block the launcher.

Public content destinations, system search and input viewers remain external Android launches. Validate URI schemes and component resolution, catch missing/security failures and retain the launcher return target. Provider discovery is separate from actual playback/viewer success on a firmware.

Diagnostics report observed availability. They must not imply access to another app's private database, account, playback progress or active audio route. See [Android compatibility](ANDROID_COMPATIBILITY.md) and [Testing](TEST_PLAN.md).

## Optional continuation metadata

Watch Next may supply episode title/display numbers, duration and last playback position.
These remain nullable. Show progress only for positive duration and a position within it;
never infer progress from engagement timestamps. Optional text is bounded/filtered. Vendors
rejecting optional columns get one fallback to the previous base projection, preserving the
bounded scan and CancellationSignal. Metadata does not establish playback entitlement,
exact seek/resume behavior or access to every publisher.

## Per-app continuation and artwork

Home shows one continuation per publisher. Genuine unfinished progress and Continue entries
precede upcoming entries, using provider engagement timestamps to break ties. Preview weight
order remains provider-owned; focused-app previews fill only publishers without Watch Next.
Search keeps the full source catalog. Provider poster artwork falls back to thumbnail artwork.

Enabled content sources can now fetch public HTTPS artwork, explicitly authorized for this
follow-up. HTTP and embedded credentials are rejected. Coil uses the existing 12 MiB shared
memory cache, bounded decode requests, no disk cache/cookies, short timeouts and cancellation.
A failed Home image falls back to app artwork. Content disabled means no content image requests.
No notification listener or private app data access is added. Network artwork is not imported
as a wallpaper; local wallpaper decoding retains its original contract.
