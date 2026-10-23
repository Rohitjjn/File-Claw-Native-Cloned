# Existing PDF and DOCX preview pipelines

This page documents the current implementation and the focused WebView asset fix in this worktree. It does not add a preview format or claim an Android device test has passed.

## Shared route into a viewer

`MainViewModel.openFile()` → `FormatDetector.detect(file, file.name)` → the existing `core.format.FormatRegistry` handler route → a family handler's `FileContentState` → `feature/preview/FilePreviewScreen.kt` → the family UI. PDF and DOCX both reach their success-state branches in source. That proves the routing branch exists; it does not prove the WebView rendered the file.

## PDF

1. `formats/pdf/PdfFormatHandler.kt` returns `FileContentState.PdfSuccess` for a detected PDF.
2. `feature/preview/FilePreviewScreen.kt` composes `formats/pdf/ui/PdfPreview.kt`.
3. `PdfPreview` loads the bundled page `app/src/main/assets/pdfjs/search.html` from the HTTPS-style `WebViewAssetLoader` origin at `/assets/pdfjs/search.html`.
4. The page imports the local PDF.js ES modules and worker from the same packaged asset tree. The exact PDF bytes are requested through `/search-pdf/document.pdf`; `formats/pdf/PdfRangeResponder.kt` serves full or byte-range responses from the file. No network PDF service is involved.
5. The page reports readiness, page changes, search, password prompts, and failures through the existing JavaScript bridge. Page rendering uses the existing PDF render-budget and page-residency policies; do not remove their caps as a preview workaround.

### PDF asset change

The previous custom `.mjs` asset response used `application/javascript`. Module scripts are checked strictly by browsers; the standard `text/javascript` MIME is the recommended response. The app now shares `platform/webview/MimeAwareAssetsPathHandler.kt`, which overrides `.js` and `.mjs` assets to `text/javascript` while letting AndroidX serve all other files normally. `LocalWebViewMimeTypesTest` contains tests for the pure mapping, but those tests have not run because Gradle could not start; see [`STATUS.md`](STATUS.md).

This is a concrete source-level loader correction, but it is not a reproduction of the reported Android WebView failure. The preview still needs a runtime smoke test on an emulator or device, including the PDF.js module, worker, PDF bytes, and a real PDF. See [`STATUS.md`](STATUS.md).

## DOCX

1. The existing format metadata maps the Word OOXML extensions to the DOCX route. `formats/ooxml/OoxmlDocumentHandler.kt` handles DOCX/PPTX state creation; for DOCX it retains a base64 fallback string and sends files larger than 25 MiB to the existing archive path.
2. `feature/preview/FilePreviewScreen.kt` composes `formats/docx/ui/DocxPreviewWebView.kt` for `FileContentState.DocxSuccess`.
3. The page is loaded from `app/src/main/assets/editor/docx_editor.html` through the `/assets/` `WebViewAssetLoader` path. Its `jszip.min.js` and `docx-preview.min.js` scripts are local assets.
4. The page requests the source at `/docx-src/document.docx`, which is served from the file path by a dedicated local asset-loader handler. If that fetch is unavailable, the existing bridge can supply the base64 copy.
5. The WebView reports a successful render through the Android bridge. On a main-page, render, or WebView-process failure, the current UI switches to an explicitly labeled text-recovery state when extraction succeeds and offers retry. Recovered text is not a formatted DOCX preview.

The DOCX viewer and the DOCX print WebView now use the same MIME-aware `/assets/` handler as PDF, so local `.js` assets get the explicit standard JavaScript MIME type. Existing print behavior and its text-recovery path remain in `platform/print/FilePrintHelper.kt`.

## Failure diagnosis checklist

If a preview still fails on a built app:

1. Record the file source (local path, external content intent, or archive entry), extension, byte size, and Android/WebView version. Do not share document contents or sensitive file paths in logs.
2. For DOCX, note the visible recovery reason and inspect `WebViewDocx` logs for main-frame/resource errors. Confirm the WebView requests `/assets/editor/docx_editor.html`, both local renderer scripts, and `/docx-src/document.docx`.
3. For PDF, inspect `PdfJsReader` logs and the JavaScript console for module/worker errors. Confirm `.mjs` responses are `text/javascript`, the worker is found, and the PDF endpoint receives expected ranges.
4. Check the visible error state and retry behavior. Do not hide errors or silently present recovered text as formatted rendering.
5. Re-test password-protected, malformed, large, and ordinary files only within existing capability and safety limits.

The current tool environment lacks Java and Android runtime tooling, so these runtime checks are still open. See [`SANITY_CHECKLIST.md`](SANITY_CHECKLIST.md) for the full existing-behavior matrix.
