# Development tools

Run tools from the repository root unless their help says otherwise. Python tools use the standard library except SBOM validation, which needs `jsonschema`; avatar export additionally needs libwebp's `cwebp`. Keep raw reports, traces, APKs, local paths and device identities ignored under `.private/` or outside the project.

## Assets

`python3 scripts/export-avatars.py --check` verifies checked-in avatar masters, runtime derivatives and their manifest without changing files. Run without `--check` to export updated masters and regenerate the manifest. See [avatar sources](../assets/avatars/README.md).

## Benchmarks and traces

`benchmark/` contains shared startup, catalog, favorites and secondary-screen journeys, profile comparison and full-compilation diagnostics. Use optimized non-debuggable APKs; see [Performance measurement](../docs/development/PERFORMANCE.md) and [Testing](../docs/TEST_PLAN.md).

- `measure-tv.py`: explicit-package process-cold/legacy 60-key/idle diagnostics. Requires `TV_DEVICE`, ADB and an installed optimized target; `--label` keeps runs distinct. Its host-driven tour is not the reference warm protocol.
- `capture-tv-trace.py`, `trace-config.pbtxt`, `analyze-trace.sql`: source-scoped legacy Perfetto diagnostic and analysis. Pass `--trace-processor` pointing to an existing local processor binary. The collector archives advertised data sources and the exact configuration, then fails if trace-quality prerequisites are absent. It does not measure input cadence/destinations or input-to-presentation latency.
- `trace_quality.py`: offline completeness gate for existing traces. It reports nonzero stats, setup errors/loss, application slices/scheduling and actual/expected FrameTimeline rows. Missing tables or query failures fail closed. This is a prerequisite check, not performance acceptance or proof of complete workload coverage. Use a direct cached `trace_processor_shell` executable; auto-download script wrappers are refused.
- `summarize-benchmark.py`: summarizes AndroidX benchmark JSON output.

Do not collect profiles or benchmarks on configured user data without an approved preservation/recovery workflow. BaselineProfileRule on API 33 may reinstall its target. Do not change default HOME, display, accessibility or power settings to obtain a result.

```sh
python3 -m unittest discover -s scripts -p test_trace_quality.py
python3 scripts/trace_quality.py .private/perfetto/example/tour.pftrace \
  --processor "$LOCAL_TRACE_PROCESSOR" --output .private/perfetto/example/quality.json
TV_DEVICE="$TV_DEVICE" python3 scripts/capture-tv-trace.py --label example \
  --trace-processor "$LOCAL_TRACE_PROCESSOR"
```

Exit status 2 means incomplete evidence; retain the report and trace for limited diagnosis.
Existing quality reports are never overwritten. The collector still uses the legacy host-driven
tour, not the reference warm workload. A 64 MiB ring buffer does not guarantee no data loss.
Review quality counters and required track coverage on every capture before attribution.

`--config scripts/trace-config-cpu.pbtxt` selects the separately labelled CPU/input path.
Its physical-TV preflight has scheduler, app and actual/expected FrameTimeline rows without
reported setup/loss errors. It omits the explicit gfx category and vendor GPU probes; use the original
graphics configuration separately and retain its unsupported-event report. Neither path alone
establishes input-to-first-presented-response association.

## Baseline profiles

`profile_rules.py` hashes production modules/convention builds and reads semantic human-readable profile rules. `summarize-profiles.py` checks current-path presence and startup inclusion. `merge-profile-captures.py` accepts three distinct successful, converged startup/catalog/secondary captures from the same source and verified target APK. Supply its local capture manifest and `--summary` path; use `--help` for arguments.

Generated baseline/startup profiles under `app/src/main/generated/baselineProfiles/` are intentional build inputs. Raw collection outputs and source/APK/convergence manifests stay local. Rule presence is not installed AOT coverage or a speedup claim.

## Dependency inventory

`runtime-inventory.init.gradle` exports the resolved runtime graph with artifact hashes. `fetch-runtime-poms.py` collects primary POMs with an offline cache; `generate-runtime-sbom.py` builds a deterministic CycloneDX inventory. [Dependency documentation](../docs/dependencies/README.md) gives the generation commands.

`validate-runtime-sbom.py` validates against explicitly supplied local official schema copies, without implicit network schema fetching. `verify-runtime-sbom-tools.py` checks determinism, malformed-input refusal and preservation on failure. Run each tool with `--help` for required inputs. POM/schema checks do not complete notice/security review; no stale generated audit is checked in as current evidence.


The standalone trace configs include task creation/rename events and poll process metadata
every second so an Android pre-spawned process can be identified after becoming the app. Archive the exact config and
validate the result: a trace may contain marker slices while its process still has a
truncated kernel name, causing the package-specific prerequisite check to reject it.
Discovery polling is diagnostic overhead. The warm Macrobenchmark asset starts tracing only
after target setup and retains its separately archived configuration.

## Product website

`build-site.py` generates the dependency-free GitHub Pages site from an explicit public-file allowlist. `check-site.py` checks links, exported files, image labels and the six source-backed preset controls. See [website setup](../docs/WEBSITE.md) for local preview and deployment.
