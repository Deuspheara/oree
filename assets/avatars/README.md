# Profile avatars

The eight 1024 × 1024 PNG files in `masters/` are original project artwork generated with OpenAI image generation. They are authoring sources; only the 256 × 256 WebP derivatives in `core/designsystem/src/main/res/drawable-nodpi/` ship in the application.

`manifest.json` records stable IDs, source/runtime paths, sizes and SHA256 hashes. The Android catalog supplies localized names. Existing stored IDs must not change when artwork is added or reordered; legacy numeric values map through `AvatarIds`.

Verify the checked-in files with Python 3:

```sh
python3 scripts/export-avatars.py --check
```

To export changed masters, install libwebp's `cwebp`, then run:

```sh
python3 scripts/export-avatars.py
```

The recipe uses quality 90, method 6 and 256 × 256 output. Review actual circular crops, focus/selection, small sizes and contrast in the running app after an artwork change. The shared loader keeps existing decode/cache bounds. No runtime generation, download or remote image backend is used.
