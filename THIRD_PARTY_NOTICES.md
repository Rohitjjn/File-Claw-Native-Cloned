# Third-party notices

## Bundled in `app/src/main/assets/`
| Library | Version | License | Verified from |
|---|---|---|---|
| JSZip | 3.10.1 | MIT **or** GPLv3 (dual) | header of `docx/jszip.min.js` |
| docx-preview | not stated in file | upstream: Apache-2.0 (**verify**) | no license text shipped in the bundle |
| pptx-preview | not stated in file | upstream: MIT (**verify**) | no license text shipped in the bundle |
| x-spreadsheet | not stated in file | upstream: MIT (**verify**) | no license text shipped in the bundle; retained legacy asset, no longer referenced by the active spreadsheet viewer |
| PDF.js | 4.10.38 | Apache-2.0 | `app/src/main/assets/pdfjs/LICENSE` |
| Adobe CMaps | bundled with PDF.js 4.10.38 | BSD-style license in the bundle | `app/src/main/assets/pdfjs/cmaps/LICENSE` |
| PDFium/Foxit standard fonts | bundled with PDF.js 4.10.38 | BSD-style license in the bundle | `app/src/main/assets/pdfjs/standard_fonts/LICENSE_FOXIT` |
| Liberation Sans fonts | bundled with PDF.js 4.10.38 | SIL Open Font License 1.1 | `app/src/main/assets/pdfjs/standard_fonts/LICENSE_LIBERATION` |

## Gradle dependencies (see `gradle/libs.versions.toml`)
AndroidX / Jetpack Compose / Room / WorkManager / Navigation / WebKit `androidx.webkit:webkit:1.12.1` (Apache-2.0) · Coil (Apache-2.0) · telephoto (Apache-2.0) ·
Apache Commons Compress (Apache-2.0) · XZ for Java (public domain / 0BSD) · zip4j (Apache-2.0) · fastexcel (Apache-2.0) ·
JetBrains markdown (Apache-2.0) · AndroidPdfViewer / pdfium-android (Apache-2.0).
Verify each license against the resolved versions before a store release.

Removed in 1.1.0: pdfbox-android, kotlin-csv, kotlinx-serialization. The fastexcel writer is active again for bounded, values-only XLSX light editing.

## Test-only tools (not packaged in the APK)

- `@sparticuz/chromium` 153.0.0 — MIT — supplies a pinned Linux Chromium binary for `tools/office-preview-tests/`.
- `puppeteer-core` 25.16.0 — Apache-2.0 — drives headless Chromium in the renderer tests.
- `kotlin-compiler` 1.9.22 — Apache-2.0 — standalone JVM compiler for the `kotlin-inspector-jvm` harness and its CI job; not packaged in the APK.
- `javajre-linux-64` 17.0.8 — MIT npm wrapper around an Oracle JDK 17 archive — task-local JDK used only to run the JVM-inspector harness locally; CI uses Temurin JDK 17 via `actions/setup-java`.
- Python fixture generators: python-docx, openpyxl, python-pptx, xlwt and Pillow; versions/licenses are recorded in `tools/office-preview-tests/fixtures/LICENSES.md`.
- Apache POI `test-data` fixtures — Apache-2.0 — license text is copied to `tools/office-preview-tests/fixtures/poi/LICENSE.txt`.
