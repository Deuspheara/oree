# Documentation screenshots

These curated images are captured from the production Compose screens rendered by `PublicScreenshotsTest` on a physical Android TV, API 34, at 1920 × 1080. The UI follows the TV's French locale. They use the original bundled landscape/avatar artwork, synthetic app names and an in-memory “Sample” profile. No configured profile, provider catalog, viewing history or personal wallpaper is used.

The fixture runs in the debug instrumentation build with default compiler/cache state; it performs no benchmark compilation reset. Home/profile navigation uses reduced motion for a settled capture. Live optics are enabled. These images document appearance, not optimized-build performance or broad device compatibility.

To reproduce after replacement-installing the debug and test APKs:

```sh
adb -s "$TV_DEVICE" shell am instrument -w -r \
  -e class fr.deuspheara.oree.PublicScreenshotsTest \
  fr.deuspheara.oree.test/fr.deuspheara.oree.LauncherTestRunner
adb -s "$TV_DEVICE" exec-out run-as fr.deuspheara.oree \
  cat cache/public-home.png > /tmp/oree-home.png
```

Repeat retrieval for `public-profiles.png` and `public-live-glass.png`. Review the images before copying them here. Keep raw evidence and device-specific metadata private. The fixture does not write launcher preferences or change default HOME.
