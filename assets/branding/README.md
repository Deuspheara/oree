# Oree brand assets

The TV banner uses the website's lowercase `oree.` wordmark on graphite. The square launcher icon uses the website favicon's tilted orbit on deep green.

- `wordmark.svg`: transparent, outlined wordmark master, with a circular terminal dot. The lettering was outlined from Helvetica Neue Bold; no font file is included or needed at runtime.
- `orbit.svg`: original website favicon geometry and colors. Its rounded square is a website treatment; Android supplies its own adaptive mask.
- Android resources live in `core/designsystem/src/main/res/`. The banner composes a background color and a transparent wordmark vector at 160 × 90dp (320 × 180px at xhdpi). The brand name is identical in English and French.
- `mipmap-anydpi/launcher_icon.xml`: adaptive icon with separate opaque background and transparent foreground, each 108 × 108dp. The 54dp orbit, including its stroke, fits inside the centered 66dp safe zone. The background has no baked-in corner mask or shadow.
- `mipmap-anydpi-v33/launcher_icon.xml`: adds a white-on-transparent monochrome orbit for supported themed launchers. Android TV itself does not support themed icons; this resource does not add phone support to the TV application.
- `drawable/launcher_icon.xml`: flat vector fallback. The manifest and screensaver service both reference the adaptive mipmap resource. Since the app requires API 26, its base icon is already adaptive; a redundant `v26` directory is unnecessary.

The SVG masters are editable artwork. Keep geometry and color changes aligned with the corresponding Android vectors. No personal screenshot is used as a runtime asset.

Platform references: [TV icons and banners](https://developer.android.com/design/ui/tv/guides/system/tv-app-icon-guidelines) and [adaptive icon layers](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive).
