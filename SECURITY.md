# Security

Oree is a development project with no supported production release or completed security audit. Do not include credentials, personal TV data or private device endpoints in public issues or captures. Report sensitive findings privately to the maintainer; a public reporting contact will be added before distribution.

## Current boundaries

- Core app launching works offline. Optional content artwork can use HTTPS; the app declares Internet permission. There is no telemetry, required backend or silent updater.
- The HOME activity is exported for Android launcher integration. Incoming extras are not a command interface.
- The screensaver service is exported behind Android's `BIND_DREAM_SERVICE` permission.
- Local photo access and public TV-listing access are optional. Denial must preserve app-only operation.
- Configuration import validates size, encoding, schema, identifiers and typed values before atomic restore. External launch destinations are validated before dispatch.
- Debug-only document/image fixtures are unexported and excluded from optimized builds. Input diagnostics are debug-only and opt-in.

Keep signing material outside Git. Security, dependency/asset notices, import validation and device compatibility need review before any public release.
