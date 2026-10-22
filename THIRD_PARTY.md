# Third-party dependencies and assets

The repository's original work is under [Apache-2.0](LICENSE). Retain upstream notices for redistributed material. This inventory describes the current source; it is not a completed release compliance or security audit.

## Runtime and tooling

| Family | Declared license / role |
|---|---|
| Kotlin and kotlinx.coroutines | Apache-2.0; language/runtime and concurrency |
| AndroidX, Compose for TV, DataStore, ProfileInstaller | Apache-2.0; Android UI, storage and profiles |
| DataStore external protobuf | BSD-3-Clause; storage implementation dependency |
| Koin | Apache-2.0; dependency injection |
| Coil | Apache-2.0; bounded local image loading |
| Haze, haze-glass, haze-utils | Apache-2.0; glass rendering |
| JUnit, AndroidX Test/Benchmark, UiAutomator, Gradle/Android/Kotlin plugins, Spotless/ktlint | Build/test tooling; review the resolved tool distribution and notices before release |

Versions are pinned in `gradle/libs.versions.toml` and the Gradle wrapper. Runtime graphs span all project modules and are shrunk for optimized builds. Use [the inventory recipe](docs/dependencies/README.md) to record exact resolved components/POM declarations. Review embedded/shaded notices and build/test tools separately. A POM license declaration alone is not a complete notice audit.

## Bundled icon vectors

- Selected [Lucide](https://github.com/lucide-icons/lucide) 0.468.0 vectors, upstream object `f12b0de177fbc2a6795e99be065887e72b237123`: [ISC notice](docs/dependencies/LUCIDE_LICENSE.txt).
- Lucide glyphs derived from Feather v4.29.2: [MIT notice](docs/dependencies/FEATHER_LICENSE.txt).
- `Tick02Icon` from `@hugeicons/core-free-icons` 4.3.5, converted to an Android vector: [MIT notice](docs/dependencies/HUGEICONS_LICENSE.txt). [Pinned source](https://unpkg.com/@hugeicons/core-free-icons@4.3.5/dist/esm/Tick02Icon.js).

Only selected vectors are bundled; there is no icon font or runtime fetch.

## Project artwork and installed resources

`core/platform/src/main/res/drawable-nodpi/landscape_forest.jpg` and the eight curated avatar masters/derivatives are original project images generated with OpenAI image generation. Avatar sources, export instructions and hashes are under [assets/avatars](assets/avatars/README.md). Launcher vectors are project-authored.

Installed application icons/banners are referenced at runtime rather than redistributed in this repository. System fonts come from Android. No third-party movie posters, proprietary platform artwork or streaming-app account content is bundled.
