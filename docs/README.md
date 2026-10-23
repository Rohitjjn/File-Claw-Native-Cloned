# Project documentation index

Start with [`../README.md`](../README.md) for a product overview and quick start, and [`../AGENTS.md`](../AGENTS.md) before making code changes.

## Source maps

- [Architecture](ARCHITECTURE.md) — package ownership, route flow, and test/assets locations.
- [Preview pipelines](PREVIEW_PIPELINES.md) — current PDF/DOCX viewer paths, local WebView assets, and open runtime checks.
- [Supported formats](FILE_FORMATS.md) — source-derived registry IDs, extension aliases, and support metadata.
- [Database and user data](DATABASE.md) — Room schema version, migration, and app backup behavior.
- [Security](SECURITY.md) — permissions, archive limits, WebView boundaries, and data-safety rules.
- [Performance and resource limits](../PERFORMANCE.md) — source-defined caps, scaling caveats, and a measurement checklist; not a benchmark report.
- [Design source map](DESIGN.md) — theme and glass tokens from the current Kotlin source.

## Build and work status

- [Build guide](../BUILD_GUIDE.md) — toolchain requirements and exact Gradle tasks.
- [Testing](TESTING.md) — test inventory and verification expectations.
- [Current status](STATUS.md) — exact checks run, blockers, and remaining acceptance checks.
- [TODO](TODO.md) and [work sequence](ROADMAP.md) — active gates only; no new preview support is implied.
- [Manual sanity checklist](SANITY_CHECKLIST.md) — device regression cases.
- [Changelog](CHANGELOG.md) — changes made in the current documentation/preview task.
- [Contributing](CONTRIBUTING.md) — source ownership, preservation, and truthful verification guidelines.
- [Repository checkpoint](../backup-manager.md) — pre-edit archive facts; the ZIP is workspace-only (not in Git/PR) and is not a user-data backup.

`THIRD_PARTY_NOTICES.md` is intentionally retained at the repository root. It is a legal/attribution record and must not be removed as stale project prose.
