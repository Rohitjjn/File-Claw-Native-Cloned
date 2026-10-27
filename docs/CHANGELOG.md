# Changelog

This file records verified changes in the current working task. It does not reconstruct unreliable historical release notes from old prompts or logs. The app's Gradle version defaults remain `1.3.0` / version code `3`; this work does not bump the app version.

## Unreleased — 2026-10-27 — Office preview Android parser compatibility fix

### Root cause fixed

- **Every valid DOCX/XLSX/PPTX failed on Android with "This Office package is damaged or unsupported."** The shared `OoxmlPackageInspector.parseSecureXml()` configured `DocumentBuilderFactory` with desktop-JVM-only calls: `isXIncludeAware = false` (the Android base class always throws `UnsupportedOperationException` here) and `setFeature(FEATURE_SECURE_PROCESSING, ...)` plus four Xerces feature URLs (Android's bundled Harmony parser supports only the `namespaces` and `validation` features and throws `ParserConfigurationException` for every other feature URL; verified against AOSP `libcore`). The first unsupported call threw, and the broad `catch (Throwable)` in `inspect()` converted it into the generic damaged-package message. All desktop-JVM tests and CI stayed green because the desktop Xerces parser supports every one of those calls — the new `kotlin-inspector-jvm --simulate-android-xml` harness mode reproduces the device failure on the unfixed code.

### Changes

- Added `formats/ooxml/OfficeXmlHardening.kt`: one Android-compatible secure XML configuration for the inspector's metadata parts. Hardening features are applied best-effort where the platform supports them (and honestly reported), `setXIncludeAware` refusal is ignored (XInclude is off by default everywhere), and a mandatory full-bytes `<!DOCTYPE`/`<!ENTITY` scan of every parsed part keeps the XXE rejection platform independent — including DOCTYPEs hidden behind >64 KB comments that the previous 64 KB prefix scan could miss on Android.
- `OoxmlPackageInspector` now classifies unexpected failures: `InvalidPackageException` paths are unchanged, and hidden Throwables are reported through a privacy-safe `failureSink` (stage + exception class name only; no paths, file names, content or credentials). `FileClawApp` installs the sink as a warning log tagged `OfficePreview`.
- Added `OoxmlPackageInspectorAndroidCompatTest` with an `AndroidLikeDocumentBuilderFactory` test double that reproduces Android's factory semantics inside the Gradle unit-test JVM, so this failure class can never again pass CI while breaking on devices.

### Verification

- `OoxmlPackageInspectorTest` + `OoxmlPackageInspectorAndroidCompatTest`: 20/20 pass on the desktop JVM with the real Apache Commons Compress 1.26.1.
- `tools/office-preview-tests/kotlin-inspector-jvm/run.sh`: 27/27 fixture cases pass in desktop mode and 27/27 in the new `SIMULATE_ANDROID_XML=1` mode (real Commons Compress, no ZIP stub).
- Realistic LibreOffice-validated corpus (rich DOCX/XLSX/PPTX with Hindi/Devanagari and Arabic RTL text, tables, image, formulas, multiple sheets; macro-enabled variant; encrypted OLE2; truncated; misleading extension; path traversal; 70 KB-comment-hidden DOCTYPE; 9.6 MB zero-fill ZIP bomb): 11/11 expected outcomes in both parser modes.
- Bundled renderer regression (`npm test`, headless Chromium): 18/18 pass; `npm run syntax`: pass. Bundled JavaScript was not modified.

## Unreleased — 2026-10-27 — Office preview repair, expansion and renderer verification

### Root causes fixed

- **DOCX blank preview:** the previous fix relied on a DOM `ResizeObserver`, but Android parent-layout changes do not always produce a DOM resize event. The Compose `ViewTreeObserver` now reports every real non-zero WebView size to `notifyViewport()`, and `loadDocx()` waits for that signal before rendering. Visible-content confirmation and print mode remain. Chromium reproduced the original late-layout race and confirmed a visible page even with `ResizeObserver` removed.
- **XLSX crash:** the remaining crash path was the viewer transport, not just the parser. It injected a whole-sheet Base64 string into one `evaluateJavascript()` call and loaded scripts via `file:///android_asset/` with file access disabled. The shared bounded virtual grid now serves a JSON payload through `WebViewAssetLoader`, renders only visible cells, and saves atomically.
- **PPTX unreliable opening:** replaced the fixed all-slide column with a layout-aware slide viewer, local-only streamed bytes, navigation, cross-slide search, zoom/pan, dark mode, text fallback and dedicated all-slide print mode.

### Format and UX changes

- Added shared OOXML package validation/variant detection for DOCX/DOCM/DOTX/DOTM, XLSX/XLSM/XLTX/XLTM and PPTX/PPTM/PPSX/POTX/POTM. Macro and template variants are clearly read-only; macros never run.
- Added bounded legacy `.ppt` Unicode text recovery, improved legacy `.doc` Unicode recovery, and an explicit encrypted-OLE2 error state. `ole2` metadata is now Partial rather than Identify only.
- Added hardened Office WebView origin/settings, asset-path traversal rejection, XXE/DOCTYPE rejection, ZIP-bomb/ratio/entry limits and external-relationship blocking/counting. The effective ZIP-bomb ratio cap is 1,000:1 (DEFLATE cannot exceed roughly 1,032:1), not an unreachable 2,000:1.
- Fixed legacy OLE2 text recovery for Indic/Arabic combining marks, emoji surrogate pairs and zero-width joiners; the UTF-16 quality score now outranks a mis-decoded single-byte view of the same bytes.
- Added spreadsheet/presentation bottom-tray controls consistent with the existing PDF/DOCX pill: search, zoom, navigation, copy, print and share. The tray scrolls horizontally on narrow screens; non-functional controls are hidden in text-recovery states.
- Added dedicated PPTX and spreadsheet print/export paths and corrected Office MIME types used by sharing.

### Verification

- Added JVM unit tests for OOXML inspection, spreadsheet JSON/save bounds, presentation zoom, legacy Unicode/PPT record extraction and OLE2 support metadata.
- Added `tools/office-preview-tests/` with original CC0 and Apache-2.0 POI fixtures, a SHA-256 manifest, an independent Python OOXML validator, JavaScript syntax checks, 18 headless Chromium renderer tests and a plain-JVM `OoxmlPackageInspector` harness.
- Local results: `npm run syntax` 15/15 scripts, `validate_ooxml.py` 27/27 cases, `npm test` 18/18 cases, `kotlin-inspector-jvm/run.sh` 27/27 cases, a one-off pure-Kotlin JVM compile/23-assertion smoke test with 0 failures, Kotlin 1.9.22 PSI parse of 181 source files with 0 syntax errors, and `git diff --check`.
- CI on PR #8: `Android Build Verification` PASS (debug + minified release APKs, R8/lintVital, JVM unit tests); `Office Renderer Tests` renderer/validator and `kotlin-inspector` jobs PASS. Two earlier CI iterations exposed three Kotlin compile errors and four JVM unit-test failures; both were diagnosed and fixed without weakening tests. `lintDebug`, `detekt`, `jacocoCoreVerify` and on-device tests remain unrun.
- Added `.github/workflows/office-renderer-tests.yml` with renderer/validator and `kotlin-inspector` jobs; existing Android CI is unchanged.

## Unreleased — 2026-10-26 — DOCX blank preview, XLSX crash + light edit, PPTX rewrite

### Fixed / changed

- **Issue 1 — DOCX preview was a blank white page (Print showed the real content):**
  root cause is a layout-timing race, not a rendering bug: `docx_editor.html`'s
  fit-to-width zoom is computed from `window.innerWidth`, which only reflects the
  WebView's true on-screen size once Jetpack Compose finishes laying it out -- and
  Compose can finish that layout pass *after* the page has already rendered (the
  on-screen WebView starts loading as soon as it is created, before Compose
  necessarily knows its final size). Unlike an OS window resize, a WebView being
  resized by its parent's Compose layout does not reliably fire a DOM `resize` event,
  so the old code (which only recalculated zoom on `resize`) could get stuck showing a
  page sized/positioned for a stale, sometimes near-invisible size, with no further
  signal to correct it. The print WebView in `FilePrintHelper` is given its size
  explicitly before it loads, so it never hit this -- which is exactly why Print always
  showed the document correctly. Added a `ResizeObserver` on the document root that
  reacts to the WebView's actual rendered size changing for any reason, Compose layout
  included, and reuses the same anchored-relayout logic the old `resize` handler had.
  As a second line of defense, after rendering finishes the page now explicitly checks
  that the first page has a non-trivial visible size before reporting success; if not,
  it retries the relayout once, and only then reports a clear, retryable error instead
  of leaving the user looking at nothing (`docx_editor.html`).
- **Issue 2 — Excel (XLSX) files crashed the app on open:** `XlsxFormatHandler.open()`
  let any failure from the third-party reader propagate uncaught. The view-model's
  surrounding try/catch only catches `Exception` and `OutOfMemoryError` specifically;
  other crash classes a parsing library can realistically throw on a real device
  (`LinkageError`/`NoClassDefFoundError`, `StackOverflowError`, a plain
  `RuntimeException` on a malformed workbook) are not `Exception` subtypes and were
  never caught anywhere -- crashing the whole app instead of showing an error screen.
  Every failure is now caught there and turned into a normal, retryable
  `FileContentState.Error`; a handful of cell types that can throw out of the library's
  own text conversion now fall back to the raw stored value instead of failing the
  whole file. Separately, `MAX_VIEW_ROWS`/`MAX_VIEW_COLUMNS` (50,000 / 256 -- up to
  12.8M cells) were lowered to 20,000 / 100: that worst case, carried through a JSON +
  Base64 string and then a second copy inside the WebView for the x-spreadsheet grid,
  could reach several hundred MB and is a very plausible cause of a crash on any
  reasonably large, dense real-world spreadsheet; files beyond the new caps are
  truncated with a visible notice instead, the same mechanism already used for CSV.
- **"Light edit mode" for XLSX (new):** single-sheet workbooks can now be edited in the
  existing spreadsheet grid UI (the same `CsvPreview`/x-spreadsheet surface CSV already
  uses) and saved back as a real `.xlsx` file, via a new `fastexcel-writer`-based
  `XlsxFormatHandler.save()`. This writes plain cell **values** only -- no formulas,
  styling, column widths or merged cells -- matching the tier of editing CSV already
  has, not a full spreadsheet editor. Multi-sheet workbooks deliberately stay
  view-only: only the first sheet is ever shown, and lightly editing it could silently
  drop the other sheets on save, so editing is only offered when there is exactly one
  sheet to begin with (`CsvSuccess.spreadsheetEditable`, `FormatSaveRequest.Spreadsheet`,
  `XlsxWorkbookLoader.write`, `MainViewModel.saveSpreadsheetFile`).
- **Issue 3 — PPTX files did not open properly:** the PPTX reader loaded its editor page
  through the legacy `file:///android_asset/` scheme (PDF and DOCX both already use the
  modern `WebViewAssetLoader`/`https://appassets.androidplatform.net/` origin) and
  pushed the *entire* presentation into the page as one Base64 string embedded in a
  single `evaluateJavascript()` call -- the same memory-multiplying, reliability-risking
  pattern already identified and replaced for DOCX in the previous round, and more
  likely to be hit here since PPTX files (embedded images) tend to be larger.
  `PptxPreviewWebView.kt` is rewritten to mirror the PDF/DOCX readers: the editor page
  and the presentation's bytes are both served through `WebViewAssetLoader`, and the
  file is streamed in directly as an `ArrayBuffer` (`fetch('/pptx-src/presentation.pptx')`)
  instead of being Base64-encoded anywhere (`OoxmlDocumentHandler.kt` no longer
  Base64-encodes PPTX either, for the same reason it stopped for DOCX, and the 25MB
  archive-view fallback no longer applies to either format). The initial slide layout
  is now deferred two animation frames so it measures the WebView's real, settled size
  rather than a possibly-stale one (same class of issue as Issue 1, applied
  proactively here). Separately, testing surfaced that the bundled `pptx-preview.min.js`
  (a pure client-side renderer) can throw on slide structures it does not fully handle
  even for a structurally valid PPTX (observed: a theme/background lookup assumes a
  shape some files don't have) -- a known limitation of this category of library, not
  specific to this change. Rather than leave the user with nothing when that happens,
  `pptx_editor.html` now falls back to extracting each slide's plain text directly from
  the OOXML with JSZip (already loaded for the renderer itself) and shows that, the
  same "degrade to text, never to blank" approach already used for DOCX
  (`DocxTextRecovery.kt`).
- **Issue 4 — general MS Office format hardening:** covered by the fixes above for
  DOCX/XLSX/PPTX. Legacy binary `.doc`/`.xls` already have a basic, crash-guarded,
  read-only text/table extraction (`LegacyOfficeFormatHandler.kt`, pre-existing,
  verified during this work); legacy `.ppt` and building new parsers for formats with
  no existing support at all were out of scope for this pass.

### Verification

- Headless Chromium (Playwright) harness, as in the previous round, plus:
  - `docx_editor.html`: loaded at a 1x1 viewport (simulating the WebView's size before
    Compose has finished laying it out) with the page's own `resize` listener registration
    actively suppressed, then grown to 390x780 -- confirmed the `ResizeObserver` path
    alone (with no `resize` event at all) still corrects the zoom and reports a normal,
    correctly-sized page; repeated with `resize` left intact to confirm no regression
    for the ordinary case. Both produce an identical, correctly fitted result.
  - `pptx_editor.html`: an 8-slide generated PPTX streamed end-to-end through the new
    `/pptx-src/` route and `fetch()` pipeline. On this generated file the bundled
    renderer threw its known theme/background error; confirmed the new plain-text
    fallback engaged automatically, extracted and displayed all 8 slides' text (with a
    visible "could not be fully formatted" notice), and reported a normal
    `onDocumentLoaded` rather than an error.
  - Full prior-round PDF (open/scrub/rotate/resize, 400-page and 126MB cases) and DOCX
    print-mode suites re-run unchanged to confirm no regression from this round's edits.
- Kotlin changes checked with `kotlinc` against the Android SDK stub jar for parse
  errors, and each changed file's error list diffed line-for-line against the same
  check run on the unmodified file: identical categories/counts in every case (the
  `androidx.compose`/`androidx.webkit` AAR classes and the `fastexcel`/`dhatim` reader
  classes are not on this offline stub classpath, which is what those diagnostics are
  about), confirming no new parse errors were introduced anywhere. The
  `org.dhatim.fastexcel.**` Proguard keep rule already covers the newly added writer
  package (same top-level namespace as the reader). No Gradle/JVM toolchain was
  available to run a full build, so `fastexcel-writer`'s exact API surface
  (`Workbook`/`Worksheet`/`.value(row, col, value)`/`.finish()`) was additionally
  confirmed against the library's published Javadoc/README for version 0.15.3 before
  use.

### Not verifiable off-device (please check on a phone / emulator)

1. The DOCX blank-preview fix on an actual device/WebView build, ideally against
   whatever real-world file originally reproduced it -- the layout-timing race this
   fixes is specific to Compose + Android WebView and could not be reproduced directly
   outside Android; the harness instead proves the `ResizeObserver` recovery mechanism
   itself works correctly when `resize` is unavailable.
2. Opening and lightly editing a real single-sheet XLSX end-to-end (edit a cell, Save,
   reopen, confirm the new value persisted and the file still opens in Excel/Sheets);
   opening a large, real-world, multi-sheet XLSX to confirm it no longer crashes and
   correctly shows as read-only with the sheet-count notice.
3. Opening a selection of real, PowerPoint-authored PPTX files -- the bundled renderer
   may well handle real files it was actually tested against better than the
   synthetically generated ones used here; worth checking whether the full-fidelity
   view or the text-only fallback is what most real files hit.
4. `./gradlew testDebugUnitTest` / `assembleDebug`, including confirming the
   `fastexcel-writer` dependency resolves and the app's `minSdk` is compatible with it
   (no JVM/Gradle toolchain was available in this environment).

## Unreleased — 2026-10-24 — PDF page scroller, rounded loader, DOCX print & large-file fixes

### Fixed / changed

- **PDF side page scroller (new):** a right-edge page-number handle that tracks the
  current reading position (0 = first page, 1 = last page) and lets the user drag to
  jump to any page, following the position live while dragging. Implemented with a new
  `onPdfScrollProgress` bridge event and `scrubToPosition()` in `search.html`, and a new
  `PdfPageScrubber` composable in `PdfPreview.kt`.
- **PDF loading indicator replaced:** the old page-shaped shimmer skeleton is replaced
  with a rounded, Claude-accent spinning-arc indicator (`PdfRoundedLoading` in
  `PdfPreview.kt`, plus a lighter CSS version for the reader's own per-page placeholders
  in `search.html`), with a load-percentage readout once the reader reports transfer
  progress.
- **PDF open reliability:** `search.html` now tries three opening strategies in order —
  real worker + HTTP Range requests, main-thread "fake worker" + Range requests, then a
  whole-file fetch — each guarded by a stall timer, so a WebView that cannot spawn a
  Worker or whose server ignores Range headers still opens the file instead of hanging.
  Added `pdf.compat.mjs` (small, guarded polyfills for `Promise.withResolvers`,
  `Object.hasOwn`, `Array#at/findLast/findLastIndex/toSorted/toReversed`,
  `String#replaceAll`, `AbortSignal.any`, `structuredClone`) and `pdf.worker.shim.mjs`,
  which load the polyfills before the bundled PDF.js 4.10 worker/module code so older
  WebView builds that lack these APIs fail gracefully instead of silently never loading.
  A boot guard reports a clear error if the module/worker scripts fail to load at all
  (previously an endless loading state). `PdfPreview.kt` adds a 45s open watchdog and
  surfaces `WebViewClient.onReceivedError`/`onReceivedHttpError` for the reader page
  itself (previously only page-internal errors were ever reported).
- **PDF rotation / resize:** page relaypout now keeps existing bitmaps on screen while
  recomputing geometry (no blank flash), anchors on the page at the **top** of the
  viewport (not the page nearest the centre, which used to jump to a different page),
  and long page jumps (`gotoPdfPage`) scroll instantly instead of animating through
  every intermediate page.
- **Issue 3 — DOCX print no longer loses formatting ("prints like plain text"):** found
  two compounding bugs. (1) The in-app Print menu printed the **live, on-screen** DOCX
  WebView whenever it was available (`docxPreviewReady && docxPreviewWebView != null`),
  which carries the reader's current CSS `zoom` (often well under 100% to fit the
  screen) and is not re-fit to the printable page. It now always prints through the
  dedicated `?print=1` renderer instead (`FilePreviewScreen.kt`, both Print menu call
  sites). (2) In `FilePrintHelper.printDocx()`, the `onDocumentLoaded` bridge callback
  submitted the print job itself ~300ms after layout, racing ahead of `onPrintReady` —
  the signal that actually waits for fonts/images to settle and fits every section to
  the printable A4 width. `onDocumentLoaded` no longer submits a print job; only
  `onPrintReady` does. The 12s fixed safety timeout is replaced with a size-aware one
  that also extends once if rendering has genuinely started but print-layout
  preparation is still finishing, so large/image-heavy DOCX files are not punished into
  the plain-text fallback for taking longer.
- **Issue 4 — large DOCX files:** `OoxmlDocumentHandler.open()` read the entire DOCX
  file into memory and Base64-encoded it (~2.7x the file's heap footprint) on every
  open, and sent any file over 25 MB to the archive-tree view instead of a preview —
  even though `DocxPreviewWebView`'s actual renderer never uses that encoded string (it
  always opens with `loadDocx(null, ...)` and streams the file itself through
  `WebViewAssetLoader`/`/docx-src/`). DOCX opens now skip the base64 encode entirely and
  are no longer size-capped here; PPTX (whose renderer does need the base64 string) is
  unchanged. Large-PDF loading is covered by the Range-request fallback chain above.
- **Issue 5 — related bug fixes:** `PdfPreview.kt`'s WebViewClient now reports main-frame
  load/HTTP failures instead of leaving the reader stuck on its loading state; the PDF
  rotation/resize anchor and instant-long-jump fixes above; the DOCX print safety-timeout
  race above.

### Verification

- `node --check` passed for `pdf.compat.mjs`, `pdf.worker.shim.mjs`, and the `<script
  type="module">` body of `search.html`.
- Headless Chromium (Playwright) harness against the real, unmodified
  `assets/pdfjs/search.html` and `assets/editor/docx_editor.html`, served through a local
  HTTP server with real byte-Range support:
  - 40-page and 400-page generated PDFs: open, drag-scrub to 0/0.25/0.5/0.75/1.0 (page
    and position match exactly), long jump, rotate, and resize all verified — reading
    position is preserved and no canvas is left blank after rotate/resize.
  - A 126 MB / 400-page PDF (8 embedded 15 MB streams) opens in under 1s, transferring
    only ~1 MB total (`bytes=0-524287` + a trailing range), not the full file; dragging
    the scrubber to 0.75 issues one more small range request and reports the correct
    page.
  - Simulated failure modes: worker script 404 (falls back to main-thread+Range, then
    whole-file, and still opens), Range requests rejected by the server after the
    worker also fails to load (falls through to whole-file fetch and opens, ~19s total
    against an artificial 2-request-then-fail Range block — exercises, not blocks, the
    stall-timeout path).
  - DOCX `?print=1` render: `onDocumentLoaded` then `onPrintReady` both fire (confirming
    the dedicated print layout path completes) before any print job would be submitted.
  - Basic DOCX fit-width render (`loadDocx(null, false)`) still produces one page sized
    to the container (794px page, zoom computed to fit a 390px viewport).
- Kotlin changes were checked with `kotlinc` against the Android SDK stub jar for parse
  errors; the only diagnostics are pre-existing "overrides nothing" / "not enough
  information to infer type variable" errors caused by the `androidx.compose`/
  `androidx.webkit` AAR classes not being on this offline classpath (confirmed by
  running the same check against the unmodified file, which produces the same category
  of errors at different line numbers). No Gradle/JVM toolchain was available to run a
  full build or instrumented test in this environment — build this on a dev machine or
  CI before shipping, and confirm the device-only items in the list below.

### Not verifiable off-device (please check on a phone / emulator)

1. The PDF page scroller's touch drag feel (handle size, "Page N of M" bubble
   placement) on an actual touchscreen.
2. The rounded PDF loading indicator's look in both light conditions and on a slow
   connection (load-percentage readout).
3. Printing a real DOCX file via "Print / Export as PDF" — confirm the output is a
   formatted, paginated PDF (not plain text) for both a small and a large/image-heavy
   document, and that opening a > 25 MB DOCX now shows a live formatted preview instead
   of the archive/file-tree view.
4. Opening a very large real-world PDF (100+ MB) over a slow/flaky connection or from
   local storage, to confirm the Range-request path and its whole-file fallback both
   work end-to-end on-device.
5. `./gradlew testDebugUnitTest` / `assembleDebug` (no JVM/Gradle toolchain was
   available in this environment).

## Unreleased — 2026-10-23

### Fixed / changed

- Added a shared MIME-aware WebView asset path handler. PDF `.mjs` modules and DOCX `.js` renderer assets are explicitly served as `text/javascript` through the existing local asset-loader route; the DOCX print WebView uses the same handler.
- Added three unit tests for `.js`/`.mjs` MIME selection and pass-through of non-JavaScript assets.
- Moved the existing PDF byte-range responder into `formats/pdf/`; its response logic is unchanged.
- Replaced stale, contradictory repository project notes with source-based English documentation for architecture, file-format metadata, database state, security, preview pipelines, build/test commands, and current blockers.
- Added a root `PERFORMANCE.md` documenting source-defined resource limits, scaling caveats, known gaps, and a measurement checklist; it explicitly does not claim benchmark results.

### Verification

- After the MIME change, `node --check` passed for seven PDF/DOCX page and bundled scripts; local preview assets were present; all 14 manifest/resource XML files parsed; local links in 20 current Markdown files resolved; no stale documentation references were found; and `git diff --check` passed.
- Post-change Gradle commands were attempted but all stopped before Gradle startup because Java/`JAVA_HOME` were unavailable. No Kotlin compilation, unit test, APK assembly, lint, detekt, coverage, Room migration test, or Android WebView render has passed.
- See [`STATUS.md`](STATUS.md) for exact commands, output, and remaining device checks.

### Preserved scope

- No new preview format, dependency, app module, app ID, namespace, SDK, signing setup, database migration, or UI redesign was introduced.
- Existing bundled assets and third-party notices/licenses are retained.
