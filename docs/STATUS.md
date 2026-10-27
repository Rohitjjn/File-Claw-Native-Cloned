# Current project and verification status

**Source review date:** 2026-10-27. **Working branch:** `arena/10dfe1b6-file-claw-native-cloned`. **Base:** `008bd1b47734b16138193120556699b1c4499f18` (`main`). The selected checkout is `Rohitjjn/File-Claw-Native-Cloned`; the prompt also named `Rohitjjn/File-Claw-App-Native-Cloned`, which does not resolve as a GitHub repository. The session branch is fixed by Arena, so the requested `fix/office-preview-engines` branch was not created; all work and the PR remain on the pinned branch.

This status distinguishes source/browser verification from Android compilation and on-device testing. A green Chromium test is not an Android WebView test.

## Implemented in this pass

- Root-cause DOCX layout fix: Compose reports real WebView bounds via `notifyViewport`, `loadDocx` waits for a non-zero viewport, and visible-content confirmation remains. Print mode is preserved as the working diagnostic renderer.
- Root-cause XLSX transport fix: removed whole-sheet Base64 `evaluateJavascript()` injection and `file:///android_asset/` script loading; added a bounded, virtualized, local-asset spreadsheet grid and atomic values-only save.
- Rebuilt PPTX preview with streamed bytes, layout readiness, slide navigation, search, zoom/pan, dark mode, images/tables, text fallback and an all-slide print path.
- Added shared OOXML validation/variant detection, hardened Office WebView settings, legacy PPT Unicode text recovery, Office MIME-aware sharing, and dedicated spreadsheet/presentation print paths.
- Added OOXML, spreadsheet, presentation, legacy text and save unit tests; added a reproducible Office fixture corpus, independent Python OOXML validator, JavaScript syntax checks, headless Chromium renderer tests and a plain-JVM inspector harness.
- Added an `Office Renderer Tests` GitHub Actions workflow with renderer/validator and `kotlin-inspector` jobs. Existing Android build/unit-test CI was left intact.

## Checks actually run locally

| Check | Command / tool | Result |
|---|---|---|
| JavaScript syntax | `npm run syntax` in `tools/office-preview-tests` | **PASS**: `node --check` passed for 15 standalone/inline bundled scripts. |
| Independent OOXML validation | `python3 validate_ooxml.py --fixtures fixtures --output results/ooxml-validation.json` | **PASS**: 27/27 expected outcomes; 0 failures. Path traversal, XXE, ZIP bomb, truncated/corrupt, misleading extension, encrypted OLE2 and external-rel fixtures behaved as expected. |
| Bundled renderer regression | `npm test` with pinned `@sparticuz/chromium` 153.0.0 + `puppeteer-core` 25.16.0 | **PASS**: 18/18 tests, 0 failures, ~17 s. See `tools/office-preview-tests/results/renderer-tests.json`. |
| Kotlin inspector JVM logic | `tools/office-preview-tests/kotlin-inspector-jvm/run.sh` with JDK 17.0.8 + Kotlin 1.9.22 | **PASS**: 27/27 cases, 0 failures. Compiles the real `OoxmlPackageInspector.kt` against a tiny ZIP metadata shim. See `tools/office-preview-tests/results/kotlin-inspector-jvm.json`. |
| Pure-Kotlin JVM logic smoke test | One-off Kotlin 1.9.22 compile of 9 dependency-free production files + 23 assertions | **PASS**: 0 failures. Covers spreadsheet bounds/JSON, zoom/search policies, HTML escaping, legacy text recovery, OLE2 stream reading and legacy DOC/PPT extractors. Local harness not committed; Gradle unit tests cover these areas. |
| Kotlin PSI syntax parse | Kotlin 1.9.22 PSI over all 181 Kotlin source files | **PASS**: 0 syntax-error nodes. Parser check only; not type checking or a Gradle build. |
| DOCX late-layout reproduction | Chromium: 1×1 viewport → 390×844 with `ResizeObserver` deleted | **PASS**: visible 366×474 first page; `onDocumentLoaded(1)`, no bridge errors. |
| DOCX print diagnostic | Chromium `?print=1` | **PASS**: `onPrintReady` fired, 793 px page, no errors. |
| PPTX rich print | Chromium `rich.pptx` `?print=1` | **PASS**: 8 slide wrappers, 1 image, 1 table, `onPrintReady`. |
| Spreadsheet virtualization | Chromium 5,001-row payload | **PASS**: 125 visible DOM cells, ~2.58 MiB JS heap in the isolated browser process; no crash. Basic/adversarial/empty payloads also passed. |
| Heavy/adversarial DOCX | Chromium matrix: rich, variants, XXE/traversal, external rel, truncated, corrupt, 50 MiB ZIP bomb, misleading extension | **PASS as browser asset behavior**: variants rendered; malformed/bomb packages produced explicit errors; no `/etc/passwd` expansion; no external requests. Production `OoxmlPackageInspector` rejects the hostile packages before the renderer. |
| `git diff --check` | local Git | **PASS** after final local edits. |
| Kotlin/Gradle compile, JVM unit tests, APK assembly, lint/detekt | `./gradlew ...` | **NOT RUN LOCALLY**: the Android SDK, Gradle distribution/dependencies, emulator and ADB are absent. A task-local JDK 17 was installed only for the JVM-inspector harness. Exact Gradle blocker without that JDK: `ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.` |
| Android WebView / PrintManager / device | emulator/device | **NOT RUN**: no Android runtime available in this environment. |

## CI status

The new `Office Renderer Tests` workflow (renderer/validator and
`kotlin-inspector` jobs) and existing `Android Build Verification` workflow are
configured to run on the pull request. Their observed results for the final
commit are recorded in the final report after the PR is created; no CI result is
claimed before it is observed.

## Remaining acceptance checks

- On a JDK 17 + Android SDK 34 machine, run:
  - `./gradlew :app:compileDebugKotlin --no-daemon --max-workers=1`
  - `./gradlew :app:testDebugUnitTest --no-daemon --max-workers=1`
  - `./gradlew :app:assembleDebug :app:assembleRelease --no-daemon --max-workers=1`
  - `./gradlew :app:lintDebug :app:detekt --no-daemon --max-workers=1`
- Install the debug APK on an emulator/device and open every supported format from local storage, an external content intent and a ZIP entry. Confirm `WebViewAssetLoader` requests, no `file://`/network requests, search/jump/zoom, dark/light themes, orientation, back/retry, print/export, share and safe save.
- Record Android System WebView version, file size/source category, visible error and relevant logs without logging document text or private paths.
- Measure on-device time-to-first-content and peak memory for the large fixtures; browser heap figures above are not Android app heap measurements.

See [`TESTING.md`](TESTING.md), [`PREVIEW_PIPELINES.md`](PREVIEW_PIPELINES.md), [`SECURITY.md`](SECURITY.md) and [`TODO.md`](TODO.md).
