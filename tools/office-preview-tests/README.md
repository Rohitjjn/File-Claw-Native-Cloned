# Office preview renderer tests

This directory contains reproducible, offline test fixtures, headless Chromium
regressions and a plain-JVM inspector harness for the app's bundled Office
preview assets. It is a developer test tool; nothing here is packaged into the
Android APK.

## What is tested

- `validate_ooxml.py` independently checks OOXML family/variant metadata and
  rejects path traversal, duplicate parts, unsafe compression ratios, encrypted
  ZIP entries, DOCTYPE/entity declarations, and unsafe external relationships.
- `syntax-check.mjs` runs `node --check` on bundled standalone scripts and on
  every inline script extracted from the preview HTML pages.
- `renderer.test.mjs` serves the real `app/src/main/assets/` pages from a local
  loopback server and drives them with `@sparticuz/chromium` / `puppeteer-core`.
  It verifies:
  - DOCX visible formatted output, including a late 1×1 → 390×844 layout with
    `ResizeObserver` removed;
  - DOCX print readiness and explicit errors for truncated packages;
  - DOCM/DOTX read-only previews and external-relationship isolation;
  - PPTX slide navigation, cross-slide search, zoom, rich images/tables, and
    all-slide print mode;
  - spreadsheet virtualization, search, cell editing, copy, zoom, dark mode,
    safe text escaping, and a 5,001-row payload;
  - corrupt/malformed files reach an error bridge rather than a blank page.
- `kotlin-inspector-jvm/` compiles the repository's real
  `OoxmlPackageInspector.kt` on a plain JVM and exercises 27 fixture cases
  against a tiny ZIP metadata shim. It checks family/variant/macro notices,
  safe external relationships and rejection of hostile or malformed packages.

The Android format handlers have their own JVM unit tests under
`app/src/test/java/com/example/formats/`. GitHub Actions runs the browser,
validator and JVM-inspector jobs in `office-renderer-tests.yml`; the existing
`android-build-verify.yml` runs Gradle compilation and JVM unit tests.

## Run locally

Node 20+ and Python 3.11+ are required. No Android SDK is needed.

```bash
cd tools/office-preview-tests
npm ci
npm run syntax
python3 validate_ooxml.py --fixtures fixtures --output results/ooxml-validation.json
npm test
```

Optional plain-JVM inspector check (JDK 17 and Kotlin compiler 1.9.22; see
[`kotlin-inspector-jvm/README.md`](kotlin-inspector-jvm/README.md)):

```bash
tools/office-preview-tests/kotlin-inspector-jvm/run.sh \
  tools/office-preview-tests/fixtures \
  tools/office-preview-tests/results/kotlin-inspector-jvm.json
```

The latest local results are committed in `results/` for review; CI regenerates
them. Chromium is supplied by the pinned `@sparticuz/chromium` npm package. The
test harness never contacts a remote document service.

## Regenerating fixtures

`fixtures/` contains the small committed corpus used by CI. To recreate the
original fixture files:

```bash
python3 -m pip install -r requirements.txt
python3 generate_fixtures.py --output /tmp/office-fixtures
```

`fixtures-manifest.json` records SHA-256 hashes and sizes for every committed
fixture/payload. The Apache POI files are copied from the upstream project's
`test-data/` tree and retain its Apache-2.0 license. See
[`fixtures/LICENSES.md`](fixtures/LICENSES.md).

## Known gaps

These tests exercise browser/WebView-like rendering and a plain-JVM source-level
inspector harness; they do not exercise Android's `WebView` implementation,
`WebViewAssetLoader`, `PrintManager`, Compose layout, Gradle/R8 or on-device
memory. The JVM-inspector harness uses a small Commons Compress shim, not the
real dependency. Those require an emulator/device and the Gradle CI job. The
tests also do not execute Office macros by design.
