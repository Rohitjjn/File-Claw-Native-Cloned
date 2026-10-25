# Changelog

This file records verified changes in the current working task. It does not reconstruct unreliable historical release notes from old prompts or logs. The app's Gradle version defaults remain `1.3.0` / version code `3`; this work does not bump the app version.

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
