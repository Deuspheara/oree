# Development tools

Run tools from the repository root unless their help says otherwise. Python tools use the standard library except SBOM validation, which needs `jsonschema`; avatar export additionally needs libwebp's `cwebp`. Keep raw reports, traces, APKs, local paths and device identities ignored under `.private/` or outside the project.

## Assets

`python3 scripts/export-avatars.py --check` verifies checked-in avatar masters, runtime derivatives and their manifest without changing files. Run without `--check` to export updated masters and regenerate the manifest. See [avatar sources](../assets/avatars/README.md).

## Benchmarks and traces

`benchmark/` contains shared startup, catalog, favorites and secondary-screen journeys, profile comparison and full-compilation diagnostics. Use optimized non-debuggable APKs; see [Performance](../docs/PERFORMANCE.md) and [Testing](../docs/TEST_PLAN.md).

- `measure-tv.py`: explicit-package process-cold/legacy 60-key/idle diagnostics. Requires `TV_DEVICE`, ADB and an installed optimized target; `--label` keeps runs distinct. Its host-driven tour is not the reference warm protocol.
- `capture-tv-trace.py`, `trace-config.pbtxt`, `analyze-trace.sql`: source-scoped Perfetto capture and analysis. Review device/tool prerequisites before use.
- `summarize-benchmark.py`: summarizes AndroidX benchmark JSON output.

Do not collect profiles or benchmarks on configured user data without an approved preservation/recovery workflow. BaselineProfileRule on API 33 may reinstall its target. Do not change default HOME, display, accessibility or power settings to obtain a result.

## Baseline profiles

`profile_rules.py` hashes production modules/convention builds and reads semantic human-readable profile rules. `summarize-profiles.py` checks current-path presence and startup inclusion. `merge-profile-captures.py` accepts three distinct successful, converged startup/catalog/secondary captures from the same source and verified target APK. Supply its local capture manifest and `--summary` path; use `--help` for arguments.

Generated baseline/startup profiles under `app/src/main/generated/baselineProfiles/` are intentional build inputs. Raw collection outputs and source/APK/convergence manifests stay local. Rule presence is not installed AOT coverage or a speedup claim.

## Dependency inventory

`runtime-inventory.init.gradle` exports the resolved runtime graph with artifact hashes. `fetch-runtime-poms.py` collects primary POMs with an offline cache; `generate-runtime-sbom.py` builds a deterministic CycloneDX inventory. [Dependency documentation](../docs/dependencies/README.md) gives the generation commands.

`validate-runtime-sbom.py` validates against explicitly supplied local official schema copies, without implicit network schema fetching. `verify-runtime-sbom-tools.py` checks determinism, malformed-input refusal and preservation on failure. Run each tool with `--help` for required inputs. POM/schema checks do not complete notice/security review; no stale generated audit is checked in as current evidence.
