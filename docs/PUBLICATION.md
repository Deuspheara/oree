# Preparing a public source repository

The repository includes original source/artwork, license notices, build instructions and curated sample-scene screenshots. Public source availability is separate from an APK release or production support.

## Keep private material out of Git

`local.properties`, SDK output, Gradle caches, `.env` files, signing stores, APKs and `.private/` are ignored. Keep device addresses/serials, configured profile exports, personal screenshots, raw traces and recovery backups there or outside the checkout. Ignored files remain on disk and must not be uploaded by copying the complete working directory.

Use Git to publish the reviewed branch, or create a source archive from a reviewed commit:

```sh
git status --short
git diff --check
git ls-files
# Review the exact archive before uploading it.
git archive --format=zip --output=/tmp/oree-source.zip HEAD
```

An archive excludes untracked/ignored files and Git metadata. A Git push includes commit identities and every file reachable through the refs being pushed. The maintainer intentionally retains the existing Git identity. Avoid pushing unrelated branches or private recovery refs.

Before publication, inspect both the current tree and all commits being shared for credentials, keys, local paths, private endpoints, personal data and binary metadata. A search for common secret patterns is useful but cannot prove their absence. Rotate any real credential that has ever been committed before rewriting or removing it.

## Documentation images

Only exports from `PublicScreenshotsTest` belong in `docs/screenshots/`. It renders production screens with project-owned wallpaper and in-memory sample apps/profiles. Inspect every image before staging it. Do not replace these with a configured TV's viewing history or custom wallpaper.

## Release work

Run the gates in [Testing](TEST_PLAN.md), retain third-party notices and review dependency/asset rights under [THIRD_PARTY.md](../THIRD_PARTY.md). Physical timing, accessibility and compatibility acceptance remain separate from compilation and sample screenshots. Publishing source does not establish a security audit or authorize signing/distributing a release.
