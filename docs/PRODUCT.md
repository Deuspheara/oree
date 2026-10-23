# Product

Oree is a local Android TV / Google TV HOME application for everyday remote use. Priorities are reliable navigation and return, responsiveness, visual consistency and optional integrations. It is an ordinary application, not a ROM, streaming service or privileged launcher replacement.

## Current scope

- Discover and launch installed TV apps; organize favorites and folders with stable identities.
- Jump between Home sections through Overview; explicitly Peek into a folder to launch a member without opening full management.
- Search local apps, folders, settings actions, enabled public content and accessible photo names.
- Store local profiles with independent appearance, organization and wallpaper settings.
- Offer settings, command controls, optional local photos, ambient display and an Android screensaver.
- Export/import validated configuration through local documents, with review and one-step undo.

The app works without accounts or media permissions. Enabled public content sources may fetch HTTPS artwork; core launcher functions remain offline. Sparse catalogs stay sparse; content must come from authentic enabled sources. Profiles affect launcher configuration, not accounts or playback inside streaming applications. Backups exclude image binaries; missing image references fall back to built-in scenery.

## Boundaries

No ads, telemetry, required backend, root, overlay, accessibility service or privileged HOME behavior. Android owns playback, DRM, external apps, permissions and system keys. Do not imply a universal streaming catalog, global playback history or system parental control.

Features and UI are still evolving. Public release requires device/accessibility coverage, measured performance, dependency/asset review and explicit maintainer approval.

## Visual setup and direct Settings navigation

New local profiles begin with a welcome screen. Users can keep the current appearance or
preview five relative visual workloads, from Simple to Full, using a stepped remote slider.
Only Finish saves the draft; setup remains visible while that profile's accepted write is
pending or failed. Existing stored profiles skip welcome and retain their appearance.
Appearance → Visual experience reopens the preview later.

Settings shows one column at a time. Select opens a category or choice; Back returns one
level and restores the previous logical focus and viewport. Folder hover keeps its icon
preview without an instructional sentence. Continue Watching uses the screen-wide viewport,
with its initial card aligned to the heading's inset.
