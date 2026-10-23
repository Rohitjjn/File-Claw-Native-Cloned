# Contributing safely

File Claw has existing users, file formats, local data, and renderer-specific limits. Prefer a small evidence-led change to an existing owner over a broad move or feature expansion.

## Before editing

1. Read the repository [`AGENTS.md`](../AGENTS.md) and the current [`STATUS.md`](STATUS.md).
2. Inspect relevant production source, tests, assets, manifest/resources, and license notices. Follow the full route from input through handler to viewer.
3. Check `git status` and `git diff` so existing work is preserved. Work only on the branch supplied for the session; do not change a managed branch name.
4. If the task is about a format, work in its existing `formats/<family>/` owner. If the behavior is shared, use its existing `core/`, `data/`, `platform/`, `feature/`, or `ui/` owner as appropriate.

## Implementation rules

- Preserve the app ID, namespace, SDK levels, module structure, dependencies, signing, database schema/migrations, and user-visible behavior unless the user approves a documented reason to change them.
- Keep parsing out of UI screens. Keep shared URI/storage, bounded I/O, atomic writes, archive safety, search, Room/preferences, navigation, printing/sharing, WebView security, errors, and design infrastructure centralized.
- Do not add new format/preview support during a repair or architecture-only task. Do not claim partial extraction is a full preview.
- Add focused tests for pure policies and parsers. WebView JavaScript syntax tests do not replace a WebView runtime test.
- Keep bundled third-party assets and their notices/licenses intact. Never treat a license file as disposable documentation.
- Keep generated files, APKs, caches, and large datasets out of source control unless the repository explicitly requires them.

## Verification and handoff

Run the relevant Gradle compile, unit-test, assemble, lint, and coverage tasks listed in [`BUILD_GUIDE.md`](../BUILD_GUIDE.md). For web assets, run `node --check` on changed scripts and verify local paths. For Android configuration, parse XML and run the app on a device/emulator for behavior changes. Run `git diff --check` and inspect the complete diff.

In the handoff, list exact commands and outcomes. Mark unrun work as `NOT RUN` with the concrete blocker; do not borrow results from `build.log`, an earlier commit, a historical prompt, or another environment. Update [`STATUS.md`](STATUS.md) and the relevant source map when facts change.
