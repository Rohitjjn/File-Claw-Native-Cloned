# Current project and verification status

**Source review date:** 2026-10-27. **Working branch:** this pass runs on a focused fix branch created from `main` (`e3a199e`). The selected checkout is `Rohitjjn/File-Claw-Native-Cloned`. The fix is delivered as a pull request targeting the default branch; it is not merged automatically.

This status distinguishes source/browser verification from Android compilation and on-device testing. A green Chromium test is not an Android WebView test. A green desktop-JVM test is not an Android parser test — which is exactly how the failure documented below reached a released state.

## Latest pass — Office preview Android parser compatibility fix

### Confirmed root cause

Every valid DOCX, XLSX and PPTX failed on real Android devices with the shared error
`"This Office package is damaged or unsupported."`:

- `formats/ooxml/OoxmlPackageInspector.kt` `parseSecureXml()` configured
  `DocumentBuilderFactory` unconditionally with desktop-JVM-only calls. On Android the
  first of them, `isXIncludeAware = false`, always throws
  `java.lang.UnsupportedOperationException` (the Android base class does not override
  it; verified in AOSP `libcore/luni/src/main/java/javax/xml/parsers/DocumentBuilderFactory.java`,
  line ~459). Had execution continued, `setFeature(FEATURE_SECURE_PROCESSING, true)` and
  the four Xerces feature URLs would each throw `javax.xml.parsers.ParserConfigurationException`
  (Android's `org.apache.harmony.xml.parsers.DocumentBuilderFactoryImpl.setFeature`
  supports only the `namespaces` and `validation` features; verified in AOSP).
- `inspect()`'s `catch (Throwable)` swallowed the real exception and produced the generic
  message. Both `OoxmlDocumentHandler.open()` (DOCX/PPTX) and `XlsxFormatHandler.open()`
  (XLSX) call `inspect()` before rendering, so all Office previews failed.
- Every desktop-JVM check (Gradle `testDebugUnitTest`, the JVM inspector harness) stayed
  green because the desktop Xerces `DocumentBuilderFactoryImpl` supports all of those
  calls. The JVM harness even compiled the inspector against a Commons Compress ZIP
  shim, so no check ever exercised Android parser semantics.

Reproduction evidence: the committed harness run with `SIMULATE_ANDROID_XML=1` rejects
all 21 accept-expected fixture cases on the unfixed code and accepts them on the fixed
code; a statement-level probe shows `isXIncludeAware = false` throwing first.

### Changes in this pass

- **New** `formats/ooxml/OfficeXmlHardening.kt` — one platform-safe secure XML
  configuration: base settings always applied; hardening features applied per-feature,
  best-effort, with an honest applied/unsupported report; `setXIncludeAware` refusal
  ignored (XInclude is off by default on every platform); **mandatory full-bytes
  `<!DOCTYPE`/`<!ENTITY` scan of every part parsed**, keeping XXE rejection platform
  independent and closing the >64 KB prolog gap the prefix scan could not cover on
  Android.
- `OoxmlPackageInspector.kt` — `parseSecureXml()` delegates to `OfficeXmlHardening`;
  unexpected Throwables are now classified (failing stage + exception class) and
  reported through `failureSink`; all user-facing rejection messages are unchanged;
  validation, limits and rejection behavior are unchanged.
- `FileClawApp.kt` — installs the sink as a warning log (tag `OfficePreview`,
  stage + exception class + fixed rejection reason only; never paths, file names,
  content or credentials).
- **New** tests: `OoxmlPackageInspectorAndroidCompatTest` +
  `AndroidLikeDocumentBuilderFactory` (an in-JVM reproduction of Android's parser
  configuration semantics) so this failure class is covered by Gradle CI unit tests.
- Harness: `kotlin-inspector-jvm` compiles `OfficeXmlHardening.kt` and gains
  `SIMULATE_ANDROID_XML=1` (`AndroidLikeXmlFactory`); both result JSONs are committed
  under `tools/office-preview-tests/results/`.
- Docs: this file, `docs/CHANGELOG.md`, `docs/PREVIEW_PIPELINES.md`,
  `tools/office-preview-tests/kotlin-inspector-jvm/README.md`.

### Checks actually run locally

| Check | Command / tool | Result |
|---|---|---|
| Inspector unit tests (existing + new Android-compat) | `kotlinc` 1.9.22 + JUnit 4.13.2 + real `commons-compress-1.26.1.jar`, `JUnitCore OoxmlPackageInspectorTest,OoxmlPackageInspectorAndroidCompatTest` | **PASS 20/20**, 0 failures |
| Fixture corpus, desktop parser mode | `kotlin-inspector-jvm` harness classes run directly against real Commons Compress 1.26.1 (stronger than the ZIP shim) | **PASS 27/27** |
| Fixture corpus, Android parser simulation | same harness, `--simulate-android-xml` | **PASS 27/27** |
| Realistic LibreOffice-validated corpus, both modes | python-docx/openpyxl/python-pptx corpus (Hindi/Devanagari + Arabic RTL, tables, image, formulas, 2 sheets; macro variant; encrypted OLE2; truncated ×2; misleading extension; path traversal; 70 KB-comment-hidden DOCTYPE; 9.6 MB zero-fill bomb); LibreOffice rendered the three rich files to PDF independently | **PASS 11/11 in both modes** |
| Bundled renderer regression | `npm test` (pinned `@sparticuz/chromium` 153.0.0 + `puppeteer-core`) | **PASS 18/18**, 0 failures, ~21 s |
| Bundled asset JS syntax | `npm run syntax` (`node --check` over bundled/inline scripts) | **PASS** |
| Pre-fix failure reproduction | unfixed `OoxmlPackageInspector.kt` (`git show HEAD`) + Android-simulation runner | **Reproduced**: all valid packages rejected with the generic message |
| Hidden-exception probe | direct call sequence under simulated Android factory | `isXIncludeAware = false` → `UnsupportedOperationException` (first throw) |
| `git diff --check` | local Git | **PASS** (run before commit) |
| Kotlin/Gradle compile, JVM unit tests, APK assembly, lint/detekt | `./gradlew ...` | **NOT RUN LOCALLY** — no Android SDK/Gradle in this environment; delegated to the repository's GitHub Actions workflows (see CI status below) |
| Android WebView / device / emulator | — | **NOT RUN**: no Android runtime available; on-device steps listed below |

### Remaining on-device acceptance checks

- Install the debug APK on an emulator/device and open valid DOCX, XLSX and PPTX files
  from local storage, an external content intent and a ZIP entry; confirm preview,
  search/jump, zoom, dark/light themes, orientation, back/retry, print/export, share
  and safe save.
- Capture `adb logcat -s OfficePreview` while opening a deliberately corrupt package:
  expect one `Office package rejected: stage=… type=…` line with no path or content.
- Record Android System WebView version and file size/source category with any visible
  error. Browser and desktop-JVM results above are not device results.

## Previous pass — Office preview repair, expansion and renderer verification

The prior pass (PR #8, branch `arena/10dfe1b6-file-claw-native-cloned`, base `008bd1b`) delivered the DOCX late-layout fix, the XLSX bounded local-asset grid, the rebuilt PPTX viewer, shared OOXML validation/variant detection, hardened Office WebView settings, legacy Office text recovery, the fixture corpus, the independent Python OOXML validator, the headless Chromium renderer tests and the `Office Renderer Tests` CI workflow. Its checks (27/27 Python OOXML validation, 18/18 Chromium renderer, 27/27 JVM inspector with the ZIP shim, CI-verified `assembleDebug`/`assembleRelease`/`testDebugUnitTest`) are recorded in PR #8 and in `docs/CHANGELOG.md`; the desktop-only blind spot that hid the parser bug is now covered by the Android-simulation mode described above.

## CI status

To be filled after the fix branch is pushed and the workflows have run on the final commit (see pull request).

## Remaining acceptance checks (all passes)

- On a JDK 17 + Android SDK 34 machine, run:
  - `./gradlew :app:compileDebugKotlin --no-daemon --max-workers=1`
  - `./gradlew :app:testDebugUnitTest --no-daemon --max-workers=1`
  - `./gradlew :app:assembleDebug :app:assembleRelease --no-daemon --max-workers=1`
  - `./gradlew :app:lintDebug :app:detekt --no-daemon --max-workers=1`
- The on-device steps listed in the latest pass above.
- Measure on-device time-to-first-content and peak memory for the large fixtures; browser heap figures are not Android app heap measurements.

See [`TESTING.md`](TESTING.md), [`PREVIEW_PIPELINES.md`](PREVIEW_PIPELINES.md), [`SECURITY.md`](SECURITY.md) and [`TODO.md`](TODO.md).
