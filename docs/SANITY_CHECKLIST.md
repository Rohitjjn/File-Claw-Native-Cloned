# Manual regression checklist

Use this checklist on an emulator or physical Android device after building the current source. Mark each item `PASS`, `FAIL`, or `NOT RUN` and record the device/Android/WebView version. Do not mark a source inspection as a device pass.

## Developer checks before the device pass

- [x] `npm run syntax`, `validate_ooxml.py`, `npm test` and `kotlin-inspector-jvm/run.sh` pass locally and in CI.
- [x] `git diff --check` and a Kotlin PSI syntax parse pass locally.
- [x] Observe the `Office Renderer Tests` and `Android Build Verification` CI results for the final commit — both PASS on PR #8.

## PDF viewer

- [ ] Open an ordinary PDF from the app's file list and confirm real pages render (not a loading skeleton left indefinitely).
- [ ] Open a PDF from an external file intent and from an archive entry.
- [ ] Confirm the PDF.js module, worker, CMaps/fonts when needed, and source PDF are loaded from local endpoints; JavaScript modules use `text/javascript`.
- [ ] If available, open a password-protected PDF; verify password retry and error messaging.
- [ ] Check page count, scrolling, search plus next/previous result navigation, jump-to-page, bookmark/resume, and scrubber.
- [ ] Rotate a page using the existing control, then rotate the device; confirm page position/search do not unexpectedly reset.
- [ ] Zoom in/out and pan. On large PDFs, confirm existing canvas budgets and page residency keep the renderer responsive.
- [ ] Check print/share and corrupt-file/renderer-retry behavior.
- [ ] Under Android dark theme, confirm PDF remains in its current light rendering mode.

## DOCX viewer

- [ ] Open DOCX from app storage, an external intent, and a ZIP entry. Confirm the editor HTML and its two local JS assets load.
- [ ] Confirm the `/docx-src/document.docx` local endpoint returns the source bytes and real formatted content appears.
- [ ] Check tables/lists/headings/images when the test document contains them; do not claim Word-perfect compatibility from one fixture.
- [ ] Check page progress, in-document search and match navigation, zoom controls/pinch, and the current dark-mode behavior.
- [ ] Check existing print output.
- [ ] Open a malformed file or deliberately exercise recovery if a safe fixture exists. Confirm the fallback is labeled as recovered text and retry is available.

## XLSX / spreadsheet viewer

- [ ] Open XLSX from app storage, an external intent and a ZIP entry. Confirm `grid.html`, `grid.js` and `/spreadsheet-src/data.json` are served from the local asset origin; there should be no `file://` requests.
- [ ] Confirm values, Unicode text, formulas-as-text and frozen headers are readable. Search for a value and navigate next/previous.
- [ ] Zoom with +/- and double-tap; pan horizontally/vertically; use Go to Cell and Copy selected cell.
- [ ] Edit one cell in a single-sheet workbook, save, reopen and confirm the original XLSX remains valid and the change persists. Confirm source-URI/imported-copy messaging.
- [ ] Open a multi-sheet, macro-enabled, template and oversized workbook; confirm read-only notices and that no save control is active.
- [ ] Print/export a bounded table and confirm the original file is unchanged.

## PPTX / presentation viewer

- [ ] Open PPTX from app storage, an external intent and a ZIP entry. Confirm `pptx_editor.html`, both local scripts and `/pptx-src/presentation.pptx` load.
- [ ] Check previous/next/jump slide navigation, slide count, image and table rendering, dark mode, pinch/double-tap zoom and pan.
- [ ] Search for text on a later slide; confirm the matching slide is selected and the result is highlighted.
- [ ] Print/export all slides as PDF; confirm the app prints from the dedicated print-mode WebView rather than the current on-screen slide only.
- [ ] Open a presentation that the bundled renderer cannot fully handle; confirm the text-only fallback is labeled and retryable.

## Legacy and variant Office

- [ ] Open DOC, XLS and PPT samples; confirm bounded read-only text/grid extraction and clear unsupported-layout notices.
- [ ] Open DOCM/DOTX, XLSM/XLTX and PPTM/POTX variants; confirm read-only preview notices and that macros are not executed.
- [ ] Open an encrypted/password-protected OOXML/OLE2 file; confirm an explicit unsupported error.
- [ ] Open files with misleading extensions, truncated/corrupt ZIPs, path traversal, XXE and external relationships; confirm validation errors or blocked requests, never a crash.

## Shared app behavior

- [ ] Open files through the app and the registered external `VIEW` intent path.
- [ ] Open and extract from a normal archive; confirm unsafe paths are not extracted and limits are honored.
- [ ] Exercise text/delimited preview and save without overwriting a truncated text preview.
- [ ] Check recent-file state, favorites/search settings, cache cleanup, theme changes, back navigation, print, and share.
- [ ] Confirm no remote content is required for PDF, DOCX, XLSX or PPTX preview and that the app does not request internet access.
- [ ] Run the Room migration test on a preserved version-3 database before considering any schema change.
