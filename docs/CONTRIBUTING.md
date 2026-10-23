# Contributing

## Branching
`main` is always releasable. Work on `feature/<short-name>`, `fix/<short-name>`, `docs/<short-name>`. Rebase before opening a PR; squash-merge.

## Commits (Conventional Commits)
`feat:` · `fix:` · `perf:` · `docs:` · `test:` · `refactor:` · `build:` · `chore:`. Body explains *why*; reference the TODO item.

## Definition of done
1. Logic that doesn't need Android goes in `com.example.core` (no `android.*` imports) with JUnit tests. Core line coverage must stay ≥ 80 % (`./gradlew jacocoCoreVerify`).
2. Every write goes through `AtomicFileWriter`; every read of user data is size-bounded; every archive read honours `ArchiveLimits`.
3. Truncated / converted / view-only content is a **flag** on the state, never text injected into the content.
4. New format = a `FormatRegistry` entry + (if it needs a viewer) a `FileKind` route + tests; then regenerate `docs/FILE_FORMATS.md`.
5. No `printStackTrace()`, no swallowed exceptions that hide a failure from the user, no placeholder code without a TODO.md ticket.
6. The app stays 100 % offline: no `INTERNET` permission, no remote assets.
7. Update `docs/CHANGELOG.md` with measured numbers (or say "not measured").

## Local checks
```
./gradlew testDebugUnitTest jacocoCoreVerify lintDebug detekt assembleDebug
```
