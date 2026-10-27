# `OoxmlPackageInspector` JVM logic harness

This directory runs the repository's **real** `app/src/main/java/com/example/formats/ooxml/OoxmlPackageInspector.kt` on a plain JVM, without Android, Gradle, or Maven access. It is an independent source-level check of the inspector's package-validation decisions against the committed Office fixture corpus.

The app normally compiles this file against Apache Commons Compress. To keep this harness self-contained, `src/main/java/org/apache/commons/compress/archivers/zip/` contains a tiny, source-compatible shim that reads ZIP central-directory metadata (Unix mode and encryption flag) with the JDK. The shim is used **only by this test**; it is never bundled into the app, and it is not a substitute for the real Commons Compress dependency or for `./gradlew test`.

## What it checks

27 cases from `tools/office-preview-tests/fixtures/`:

- Standard, macro-enabled and template DOCX/XLSX/PPTX variants, plus Apache POI interoperability fixtures.
- Misleading file extensions (`misleading.pdf` is a Word package).
- A safe external relationship that must be counted and surfaced as a notice.
- Rejection of corrupt, truncated, empty, ZIP-bomb, XXE/traversal/adversarial, invalid-POI and encrypted/OLE2-magic packages.
- Family, variant, macro flag, notice text and external-relationship count for accepted packages.

The expected outcomes mirror `../validate_ooxml.py`, but the harness exercises the Kotlin implementation rather than a Python reimplementation.

## Prerequisites

- JDK 17 or newer (`java`, `javac`).
- Kotlin JVM compiler 1.9.22. Locally you can obtain one from npm without Maven:
  ```bash
  npm pack kotlin-compiler@1.9.22
  tar -xzf kotlin-compiler-1.9.22.tgz
  export JAVA_HOME=/path/to/jdk
  export KOTLINC=/path/to/package/bin/kotlinc
  ```
  `npm`, `tar`, `python3`, `node`, `unzip` and `zip` are already used by the rest of `tools/office-preview-tests`.

## Usage

From the repository root:

```bash
tools/office-preview-tests/kotlin-inspector-jvm/run.sh \
  tools/office-preview-tests/fixtures \
  tools/office-preview-tests/results/kotlin-inspector-jvm.json
```

The first argument defaults to the committed fixture directory; the second argument is optional and writes a JSON report. The script prints one `PASS`/`FAIL` line per case and exits non-zero on any failure.

## Limitations

- The Commons Compress shim implements only the API surface used by `OoxmlPackageInspector`; it does not validate Commons Compress itself.
- Android `WebView`, Compose UI, PrintManager, and the actual Gradle/R8 build are not exercised here.
- Full JVM/Compose unit tests still require the Android SDK and Gradle dependencies; see `docs/TESTING.md` and `docs/STATUS.md`.
