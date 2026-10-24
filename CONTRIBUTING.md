# Contributing

Oree is under active development. Keep changes focused and explain the problem, resulting behavior and verification. Discuss substantial feature or UI changes with the maintainer before implementing them.

## Development

Use the pinned Gradle wrapper, JDK 21 and Android SDK 37. Follow [Architecture](docs/ARCHITECTURE.md), [Design](docs/DESIGN.md), [Motion](docs/MOTION.md) and the contributor rules in [AGENTS.md](AGENTS.md). Keep technical prose/code in English and UI strings localized in English and French.

Repository contracts belong to `core:domain`; persistence, serialization and migration belong to `core:data`; Android sources belong to `core:platform`. Features expose state and callbacks without implementation dependencies. Add use cases for workflows spanning repositories rather than wrapping every getter. Preserve stored component/profile IDs, legacy readers and atomic persistence.

## Before review

```sh
./gradlew spotlessApply
./gradlew spotlessCheck verifyArchitecture test lintDebug \
  :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleBenchmarkRelease
./gradlew :app:assembleDebugAndroidTest :benchmark:assembleNonMinifiedRelease
python3 scripts/export-avatars.py --check
```

Keep formatting-only changes separate from behavioral changes. Add meaningful unit tests for business/concurrency changes and native D-pad/lifecycle tests for interaction changes. See [Testing](docs/TEST_PLAN.md). Report failures and limitations alongside passing checks. Performance claims need matched optimized measurements under [the performance protocol](docs/development/PERFORMANCE.md).

Keep captures, APKs, traces, local configuration and signing keys out of commits. Test this package explicitly without changing another launcher or system settings. Follow [Recovery](docs/RECOVERY.md) on configured TVs.

## Licenses and security

Contribute original or license-compatible work and retain required notices. Update [THIRD_PARTY.md](THIRD_PARTY.md) when adding dependencies or assets. Do not include proprietary artwork, private endpoints, accounts or keys. See [Security](SECURITY.md) for reporting guidance. Publishing and release signing are maintainer actions.
