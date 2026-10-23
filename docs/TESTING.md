# Tests and verification

## What the repository contains

- Local JVM tests live under `app/src/test/java/` and cover core detection, I/O, archives, security, Room migration helpers, and format-family parsers/policies.
- At the 2026-10-23 documentation pass, there were **36 Kotlin test files** and **228 `@Test` annotations**. This is a source inventory count, not a test result.
- No files were present under `app/src/androidTest/`; there is no current Compose/instrumented Android test suite in this checkout.
- WebView renderers use local HTML/JavaScript assets. `node --check` can check JavaScript syntax, but does not check MIME responses, Android asset interception, WebView bridge calls, or actual rendering.

## Standard checks

Use JDK 17 and Android SDK platform 34. From the repository root:

```bash
./gradlew :app:compileDebugKotlin --no-daemon --max-workers=1
./gradlew :app:testDebugUnitTest --no-daemon --max-workers=1
./gradlew :app:assembleDebug --no-daemon --max-workers=1
./gradlew :app:lintDebug :app:detekt --no-daemon --max-workers=1
./gradlew :app:jacocoCoreVerify --no-daemon --max-workers=1
```

For changed web code, run `node --check` on each standalone or extracted script. For manifest/resource changes, parse the XML. Always run `git diff --check` and inspect the complete diff. Android/WebView behavior changes also need an emulator/device pass.

## Current execution record

Baseline and post-change Gradle attempts are recorded in [`STATUS.md`](STATUS.md). All stopped before Gradle startup because Java was unavailable and `JAVA_HOME` was unset. Kotlin compilation, unit tests, APK assembly, lint/detekt, coverage, and Room migration tests therefore have no passing result for this worktree.

After the MIME source change, `node --check` passed for seven PDF/DOCX page and library scripts; the expected local PDF/DOCX assets exist; all 14 manifest/resource XML files parsed; local links in the 20 current Markdown files resolved; no obsolete documentation references were found; and `git diff --check` passed. These source-only checks are not substitutes for Gradle or Android WebView runtime tests. The Kotlin documentation generator was updated but not compiled or run.
