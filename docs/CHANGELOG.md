# Changelog

This file records verified changes in the current working task. It does not reconstruct unreliable historical release notes from old prompts or logs. The app's Gradle version defaults remain `1.3.0` / version code `3`; this work does not bump the app version.

## Unreleased — 2026-10-23

### Fixed / changed

- Added a shared MIME-aware WebView asset path handler. PDF `.mjs` modules and DOCX `.js` renderer assets are explicitly served as `text/javascript` through the existing local asset-loader route; the DOCX print WebView uses the same handler.
- Added three unit tests for `.js`/`.mjs` MIME selection and pass-through of non-JavaScript assets.
- Moved the existing PDF byte-range responder into `formats/pdf/`; its response logic is unchanged.
- Replaced stale, contradictory repository project notes with source-based English documentation for architecture, file-format metadata, database state, security, preview pipelines, build/test commands, and current blockers.
- Added a root `PERFORMANCE.md` documenting source-defined resource limits, scaling caveats, known gaps, and a measurement checklist; it explicitly does not claim benchmark results.

### Verification

- After the MIME change, `node --check` passed for seven PDF/DOCX page and bundled scripts; local preview assets were present; all 14 manifest/resource XML files parsed; local links in 20 current Markdown files resolved; no stale documentation references were found; and `git diff --check` passed.
- Post-change Gradle commands were attempted but all stopped before Gradle startup because Java/`JAVA_HOME` were unavailable. No Kotlin compilation, unit test, APK assembly, lint, detekt, coverage, Room migration test, or Android WebView render has passed.
- See [`STATUS.md`](STATUS.md) for exact commands, output, and remaining device checks.

### Preserved scope

- No new preview format, dependency, app module, app ID, namespace, SDK, signing setup, database migration, or UI redesign was introduced.
- Existing bundled assets and third-party notices/licenses are retained.
