# Dependency inventory and notices

The adjacent license files cover selected redistributed icon vectors. [THIRD_PARTY.md](../../THIRD_PARTY.md) lists dependency and asset families. Dependency versions live in `gradle/libs.versions.toml`.

Generate a fresh source-scoped runtime inventory rather than relying on historical snapshots:

```sh
./gradlew --init-script scripts/runtime-inventory.init.gradle :app:exportRuntimeDependencyInventory
python3 scripts/fetch-runtime-poms.py \
  .private/dependency-audit/runtime-inventory.json .private/dependency-audit
source_sha=$(python3 -c 'import sys; sys.path.insert(0,"scripts"); from profile_rules import main_code_sha; print(main_code_sha())')
python3 scripts/generate-runtime-sbom.py \
  .private/dependency-audit/runtime-inventory.json \
  .private/dependency-audit/pom-provenance.json .private/dependency-audit/sbom \
  --source-sha256 "$source_sha"
```

The Gradle task hashes actual resolved Maven runtime inputs across project modules before R8. POM collection uses primary Maven metadata and supports an offline cache. SBOM output is deterministic CycloneDX 1.6 with dependency edges, artifact hashes and declared licenses. Use the validation/tool-verification scripts described in [scripts](../../scripts/README.md) with reviewed local schema copies.

Published declarations and schema checks do not replace embedded NOTICE, shaded/source copyright, build/test-tool or security review. Regenerate after dependency changes and complete those reviews before distribution. Keep raw POMs, local paths, tool environments and reports ignored.
