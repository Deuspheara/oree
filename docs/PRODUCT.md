# Product scope

Oree is a local Android TV / Google TV launcher for everyday remote use. Priorities are reliable navigation, clear focus and an appearance the user can adjust to their TV.

## Included

- Installed-app browsing, favorites, folders, reorder, Overview and folder Peek.
- Local search for apps, folders, settings actions, enabled public content and accessible photo names.
- Local profiles with independent layouts, appearances and wallpapers.
- Six previewable [visual experience levels](PERFORMANCE.md), from Simple to Live Liquid Glass.
- Settings, command controls, optional local photos, manual ambient display and an Android screensaver.
- Reviewed configuration export/import, atomic restore and one-step undo. Backups reference images rather than including them.

New profiles start with a welcome screen. Keeping the current appearance or choosing Finish/Apply is explicit; browsing the slider does not save. Existing profiles retain their stored appearance. Settings shows one level at a time, and Back restores the previous logical focus and viewport.

## Optional content and privacy

Core app launching works offline without accounts or media permissions. Optional Android TV providers supply available Continue Watching and preview entries; enabled content can download public HTTPS artwork. Empty catalogs remain empty. Missing images use local fallbacks.

There are no ads, telemetry or mandatory accounts. Profiles configure Oree; they do not change accounts or playback in streaming apps. Android and external apps own playback, DRM, permissions, system keys and screensaver scheduling.

## Boundaries

Oree is an ordinary app, not a ROM or privileged launcher replacement. Do not add root, overlays, accessibility services or privileged HOME behavior. It is not a universal streaming catalog, global playback history or system parental control.

The app is a development preview. Public releases require explicit maintainer authorization, measured performance, device/accessibility coverage and dependency/asset review. [Builds and targeted tests](TEST_PLAN.md) do not establish release readiness.
