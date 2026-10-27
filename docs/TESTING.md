# Tests and verification

## Repository test layers

- Local JVM tests live under `app/src/test/java/` and cover core detection, I/O, archives, security, Room migration helpers, OOXML inspection, spreadsheet payloads/saves, presentation zoom, legacy Office text recovery and format-family parsers/policies.
- At the 2026-10-27 pass there are **42 Kotlin test files** and **256 `@Test` annotations**. This is a source inventory, not a passing result; Gradle could not start locally.
- No instrumentation-test source tree exists under `app/src/androidTest/`.
- `tools/office-preview-tests/` contains a committed Office fixture corpus, an independent Python OOXML validator, JavaScript syntax checks, headless Chromium regression tests for the bundled HTML/JavaScript viewers, and a plain-JVM harness that compiles the real `OoxmlPackageInspector.kt` against a small ZIP metadata shim.
- `.github/workflows/office-renderer-tests.yml` runs the browser/validator suite and the JVM-inspector harness; `.github/workflows/android-build-verify.yml` runs Gradle compilation, release shrinking and JVM unit tests.

## Standard local checks

### Browser/asset checks (runnable without Android SDK)

```bash
cd tools/office-preview-tests
npm ci
npm run syntax
python3 validate_ooxml.py --fixtures fixtures --output results/ooxml-validation.json
npm test
```

The pinned `@sparticuz/chromium` package supplies a Linux Chromium binary. The
test server binds to loopback only; no remote document service is used.

### Plain-JVM inspector logic check (JDK 17 + Kotlin compiler 1.9.22)

```bash
tools/office-preview-tests/kotlin-inspector-jvm/run.sh \
  tools/office-preview-tests/fixtures \
  tools/office-preview-tests/results/kotlin-inspector-jvm.json
```

This compiles the repository's actual `OoxmlPackageInspector.kt` with a small
source-compatible Commons Compress shim and runs 27 fixture cases. It is a logic
check, not a Gradle/Android build; see
[`tools/office-preview-tests/kotlin-inspector-jvm/README.md`](../tools/office-preview-tests/kotlin-inspector-jvm/README.md).

### Gradle checks (JDK 17 + Android SDK 34)

```bash
./gradlew :app:compileDebugKotlin --no-daemon --max-workers=1
./gradlew :app:testDebugUnitTest --no-daemon --max-workers=1
./gradlew :app:assembleDebug :app:assembleRelease --no-daemon --max-workers=1
./gradlew :app:lintDebug :app:detekt --no-daemon --max-workers=1
./gradlew :app:jacocoCoreVerify --no-daemon --max-workers=1
```

Also run `git diff --check`, `node --check` on edited JavaScript (or
`npm run syntax`), and parse manifest/resource XML when changed. Android/WebView
behavior changes still need an emulator/device pass.

## Current execution record (2026-10-27)

- `npm run syntax`: **PASS**, 15 standalone/inline bundled scripts.
- `validate_ooxml.py`: **PASS**, 27/27 expected outcomes, 0 failures.
- `npm test`: **PASS**, 18/18 Chromium tests, 0 failures.
- `kotlin-inspector-jvm/run.sh`: **PASS**, 27/27 cases with JDK 17.0.8 and Kotlin 1.9.22; see `tools/office-preview-tests/results/kotlin-inspector-jvm.json`.
- One-off Kotlin 1.9.22 JVM compile of 9 dependency-free production Kotlin files plus a 23-assertion harness: **PASS**, 0 failures. It exercised spreadsheet bounds/JSON, presentation/DOCX zoom, search navigation, HTML escaping, UTF-16/single-byte text recovery, OLE2 stream reading and legacy DOC/PPT extractors against committed fixtures. This local harness is not committed; the repository's JVM unit tests cover these areas under Gradle.
- Kotlin 1.9.22 PSI syntax parse of all 181 Kotlin source files: **PASS**, 0 syntax-error nodes. This is a parser check, not type checking or a Gradle build.
- Chromium visual inspection: DOCX basic, PPTX rich print and spreadsheet grid screenshots were inspected; the DOCX page is visible, PPTX print contains slide text/image/table and the grid keeps headers/cells readable on a 390 px viewport.
- `git diff --check`: **PASS** after the final local edits.
- Gradle/JVM unit tests/APK/lint/detekt: **NOT RUN LOCALLY**. A task-local JDK 17 was installed only for the JVM-inspector harness; the Android SDK, Gradle distribution/dependencies, emulator and ADB remain unavailable. GitHub CI is required for these results.
- Android device/emulator tests: **NOT RUN** in this environment.

## Fixture coverage

The committed corpus includes original CC0 fixtures and Apache-2.0 Apache POI
samples; see `tools/office-preview-tests/fixtures/LICENSES.md`. Coverage includes
simple/rich DOCX, XLSX and PPTX; DOCM/DOTX/XLSM/XLTX/PPTM/POTX variants; Hindi,
Arabic and emoji text; tables, images, charts, formulas, multiple sheets/slides
and a 5,001-row workbook; empty/truncated/corrupt files; misleading extensions;
ZIP-bomb-like expansion; path traversal; XXE DOCTYPE; external relationships;
and HTML/script-like spreadsheet cell values. The Python generator can recreate
the original fixtures; the manifest records SHA-256 hashes.

Do not treat a browser test or the JVM-inspector harness as proof of Android
`WebViewAssetLoader`, `PrintManager`, Compose lifecycle, Gradle/R8 or on-device
memory behavior. Those are listed as remaining checks in [`STATUS.md`](STATUS.md).
