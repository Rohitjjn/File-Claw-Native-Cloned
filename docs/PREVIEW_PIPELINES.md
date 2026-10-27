# Document preview pipelines

This page records the current source flow for PDF and Microsoft Office previews,
the root causes of the three reported Office failures, and the evidence that is
still required on a real Android device. Browser-based checks are real rendering
checks of the bundled HTML/JavaScript assets; they are not Android `WebView` or
`PrintManager` tests.

## Shared route into a viewer

`MainActivity` / incoming intent → `MainViewModel.openFile()` →
`FormatDetector.detect(file, file.name)` → the single `core.format.FormatRegistry`
handler route → a family handler's `FileContentState` →
`feature/preview/FilePreviewScreen.kt` → family viewer.

For OOXML, `FormatDetector` still refines ZIP packages by their main part
(`word/document.xml`, `xl/workbook.xml`, `ppt/presentation.xml`). The new
`formats/ooxml/OoxmlPackageInspector.kt` is a validation layer called by those
existing handlers; it does not add another registry or change route IDs.

## PDF

1. `formats/pdf/PdfFormatHandler.kt` returns `FileContentState.PdfSuccess`.
2. `FilePreviewScreen` composes `formats/pdf/ui/PdfPreview.kt`.
3. The bundled `pdfjs/search.html` page is served through `WebViewAssetLoader`.
   `MimeAwareAssetsPathHandler` gives `.js`/`.mjs` files the standard
   `text/javascript` MIME type.
4. PDF bytes are served by `PdfRangeResponder`; no network service is involved.
5. Readiness, page changes, search, password prompts and failures flow through
   the existing JavaScript bridge. Existing render-budget and residency caps
   remain in force.

## DOCX — blank white preview

### Root cause

The print pipeline and the on-screen pipeline use the same `docx_editor.html`
renderer, but only the on-screen pipeline runs the fit-to-width layout engine.
Compose can finish laying out an `AndroidView` **after** the page's
`onPageFinished()` callback. A DOM `resize` event is not reliably emitted when a
parent ViewGroup changes a WebView's bounds. The preview could therefore compute
its zoom and first-page geometry from a stale or zero viewport and leave the
rendered page off-screen; print mode skipped that layout engine entirely, which
is why Print showed the document.

### Current pipeline and fix

1. `OoxmlDocumentHandler` validates the OOXML package and returns
   `DocxSuccess` with a variant notice; no Base64 copy is kept in state.
2. `DocxPreviewWebView` serves the page, JSZip, docx-preview and the source bytes
   from fixed `WebViewAssetLoader` routes. `OfficeWebViewSecurity` blocks all
   non-local navigation/requests.
3. A `ViewTreeObserver.OnGlobalLayoutListener` reports every real non-zero
   Compose layout to `notifyViewport(width, height)`. `onPageFinished` seeds the
   same signal.
4. `loadDocx()` waits for `waitForViewport()` before fetching/rendering.
   `ResizeObserver`, `pageshow` and `visibilitychange` remain defensive relayout
   paths. After rendering, the page verifies that the first `.docx` section has
   visible dimensions before calling `onDocumentLoaded`.
5. Search, page tracking, pinch/double-tap zoom, dark mode, print, retry and the
   labeled text-recovery state remain on the existing shared UI/bridge.

Evidence: Chromium 153 renders a visible page from a 1×1 viewport that later
changes to 390×844 even with `ResizeObserver` deleted; print mode still calls
`onPrintReady`. See `tools/office-preview-tests/results/renderer-tests.json`.

## XLSX — crash on open

### Root cause

The previous `CsvPreview` generated an inline HTML page whose scripts were
loaded from `file:///android_asset/` while the WebView disabled file access. It
then passed the entire worksheet as a Base64 string inside one
`evaluateJavascript("loadData('...')")` call. A dense workbook could exceed
WebView's script-string limit or exhaust memory before a single cell appeared;
the parser's own failures were already caught, but the rendering transport was
not.

### Current pipeline and fix

1. `XlsxFormatHandler` validates the package, then loads a bounded first-sheet
   values model (20,000 rows, 100 columns, 4,000 characters/cell, 8,000,000
   total characters).
2. `formats/spreadsheet/SpreadsheetGridPayload.kt` builds one JSON document with
   safe escaping; truncation disables editing.
3. `formats/spreadsheet/ui/SpreadsheetGridWebView.kt` serves the page, script and
   JSON through the local asset-loader origin. The page materialises only
   visible cells; a 5,001-row payload produces ~125 DOM cells in Chromium.
4. Edits are emitted as one-cell deltas to `CsvPreview`, which keeps the existing
   pending-edit/back-confirmation UI and calls
   `MainViewModel.saveSpreadsheetFile`. `XlsxWorkbookLoader.write` uses
   `AtomicFileWriter`; multi-sheet, truncated, macro and template workbooks are
   read-only.
5. `FilePrintHelper.printSpreadsheet` prints a bounded escaped HTML table.

Evidence: Chromium tests cover basic, 5,001-row, adversarial and empty payloads,
including search, zoom, copy, cell edit and HTML-injection resistance.

## PPTX — files did not open/render reliably

### Root cause

The old viewer used a `file:///android_asset/` origin with file access disabled
and pushed the whole presentation into one enormous Base64
`evaluateJavascript()` call. It rendered all slides into a fixed 800 px column,
had no reliable slide navigation/search/theme/zoom integration, and no explicit
renderer-crash recovery. Large or media-heavy packages were most likely to fail.

### Current pipeline and fix

1. `OoxmlDocumentHandler` validates the package and returns `PptxSuccess` with a
   variant notice.
2. `PptxPreviewWebView` streams bytes through `/pptx-src/presentation.pptx`,
   hardens the WebView with `OfficeWebViewSecurity`, and waits for a non-zero
   Compose layout before initialising pptx-preview.
3. `pptx_editor.html` uses the library's slide mode, hides its duplicate
   built-in controls, and exposes `nextSlide`, `prevSlide`, `goToSlide`,
   `searchWord`, `next/prevSearchMatch`, `setZoom`, `setDarkMode` and
   `setNotice`. Search uses a bounded OOXML text index, then highlights the
   current slide and jumps to the matching slide.
4. A failed formatted render degrades to a clearly labeled slide-text view; a
   renderer-process loss becomes a retryable error state.
5. `FilePrintHelper.printPptx` opens `?print=1` in list mode, waits for fonts and
   `onPrintReady`, then creates the Android print adapter. The live viewer's
   current-slide state is never used for print.

Evidence: Chromium tests cover basic/variant/rich presentations, all eight
slides in print mode with one image and one table, and corrupt-package errors.

## Legacy Office and variants

`LegacyOfficeFormatHandler` remains the single OLE2 route. `.doc` and `.ppt`
use bounded Unicode text recovery through `OleTextRecovery`; `.xls` uses
`Biff8XlsParser`. Encrypted `EncryptedPackage` OLE2 containers and `.xlsb` are
not decoded. Macro-enabled and template OOXML variants are previewed read-only;
macros are never executed. See [`FILE_FORMATS.md`](FILE_FORMATS.md) for the
complete support matrix.

## Failure diagnosis checklist

If a preview still fails on a built app:

1. Record file source, extension, byte size, Android/WebView version and the
   visible error/retry state. Do not log document contents or private paths.
2. Confirm the WebView requests only `/assets/...`, `/docx-src/...`,
   `/pptx-src/...` or `/spreadsheet-src/...`; there should be no `file://` or
   network requests.
3. For DOCX/PPTX, confirm `notifyViewport()` received a non-zero width/height
   and `onDocumentLoaded` arrived after visible content existed.
4. For XLSX, confirm `data.json` is served with `application/json`, the grid
   reports `onGridReady`, and a truncated payload is read-only.
5. Check print readiness callbacks (`onPrintReady`) and Android print output for
   DOCX, PPTX and spreadsheets.
6. Re-test password-protected, malformed, large and ordinary files within the
   documented limits.

The local environment has Chromium but no JDK/Android SDK/emulator. Android
runtime checks remain open and are listed in [`STATUS.md`](STATUS.md) and
[`SANITY_CHECKLIST.md`](SANITY_CHECKLIST.md).
