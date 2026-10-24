# Continue Watching, content and photos

Oree works as an app launcher without optional content or photo access.

## Continue Watching and app previews

Enable the sources you want in Home settings. Watch Next and app previews are separate choices. Oree displays what installed apps share through Android TV's public providers; some apps or TVs may supply no entries.

Home shows one continuation per publisher. Available unfinished/Continue entries take priority. Progress bars and episode details appear only when a provider supplies valid information. Selecting an entry opens its external app; that app controls playback, account access and resume behavior.

Artwork may download over HTTPS when content sources are enabled. If loading fails, Oree uses available app artwork or a static fallback. Turning content off stops content-artwork requests.

## Local photos and wallpapers

Opening the gallery can request image access. The system photo picker is an alternative. Denying access leaves other launcher features available.

Search can use photo names that are already accessible. In Photo Viewer, Left/Right browses photos, Select opens controls and Back returns. It does not automatically advance photos.

Selecting an image in the wallpaper gallery applies it as a wallpaper. Missing or unavailable images fall back to built-in scenery. Configuration backups store image references, not the images themselves.

## Missing content

An empty row can mean the source is disabled, denied, unsupported, unavailable or simply has nothing to share. It is not proof that an external app has no viewing history. Oree does not read private streaming-app databases or provide a universal catalog.

## Contributor notes

Keep source-specific loading/error states and cached data on refresh failure. Watch Next wallpaper/feed share a bounded snapshot; preview-only mode must not query Watch Next. Filter by selected package before presentation limits. Conflate invalidations, cancel obsolete reads and stop observers with the last foreground subscriber. Do not add polling.

Validate external destinations and optional provider columns, with fallback for unsupported projections. Artwork keeps bounded requests, the shared 12 MiB memory cache, no disk cache/cookies and cancellation. Reject HTTP and embedded credentials. Photo permission/URI validation and decode bounds remain required. See [Testing](TEST_PLAN.md) and [Compatibility](ANDROID_COMPATIBILITY.md).
