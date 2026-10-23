# Changelog

## 2026-10-22 — unversioned repository preparation (migration blocked)

- Extracted and verified all 366 source archive files at repository root; removed the transport ZIP.
- Restored executable launcher/verification script permissions and added local-output/secret ignores.
- Replaced the unrelated AI Studio README with native Android setup and accurate migration status.
- Added current/proposed architecture, production inventory, format ownership, development and test plans.
- Removed only byte-identical legacy documentation duplicates and historical build.log.
  Exact removal list is in FILE_MIGRATION_MAP.md. Non-identical history is retained for further audit.
- Corrected the build guide's missing workflow and stale fixed test-count claims.
- No application behavior, parser, renderer, build dependency, package, schema or asset changed.
- Baseline cannot start without Java; Android SDK/Kotlin tooling are also not configured.
  This is **not** a completed architecture migration or verified application release.

Earlier development history remains in ../changelogs.md and the retained legacy documents.
