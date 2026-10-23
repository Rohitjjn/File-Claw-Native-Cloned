# Implementation Plan: Loop 15 — PDF Scrubber, Gesture Resolution, DOCX Zoom/Print, Sensor Orientation & Settings Redesign

## 1. Problem Statement & User Requirements
The user requested resolving 9 interconnected defects and enhancements across document previewing, gestures, printing, orientation, performance, and UI styling:

1. **PDF Page Scroller (Scrubber)**: Replace the current rail with a smooth floating pill on the right edge showing `Page X of Y` that appears on touch, smoothly scrubs from page 1 at the top to the last page at the bottom, and retains page position upon release.
2. **Gesture Conflict Resolution (Single-Tap vs. Double-Tap)**: Resolve the conflict where single-tap (focus mode / toggle app bars) fights with double-tap (zoom in / zoom out). Implement a standard 250ms debounce window so double-taps reliably trigger zoom without toggling the system bars, while single-taps cleanly toggle focus mode.
3. **Rounded Loading Skeleton for PDF**: Replace plain spinners/cards with a modern, rounded shimmer skeleton matching document page geometry during document initial load and page render.
4. **Claude-Style Settings Screen Redesign**: Redesign `SettingsScreen.kt` with a premium, clean Claude aesthetic featuring warm paper backgrounds, dark charcoal ink, terracotta accents, grouped glass cards, streamlined essential options, and a storage manager with cache cleanup.
5. **DOCX Focal-Point Zoom Engine**: Fix the broken pinch-zoom behavior in `docx_editor.html`. Root cause: `setZoomDisplay()` applied `transform: scale()` on top of already-committed CSS `zoom` with a fixed `transform-origin: top center` and two competing scroll containers. Implement single-scroller viewport, fit-to-width baseline (100% = fits phone width), focal-point anchored pinch zoom, and double-tap toggle (fit ⇄ 200%).
6. **DOCX Print-to-PDF Fidelity**: Eliminate the plain-text fallback when printing DOCX files. Implement dedicated hidden print WebView rendering real Word layout, styles, tables, and vector text directly into Android's `PrintDocumentAdapter`.
7. **PDF Orientation & Sensor Landscape**: Fix orientation toggle causing reloads, black screen blinks, and one-sided landscape. Use `ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE` so phone rotation freely handles left and right landscape, with light window backgrounds and top-of-viewport scroll anchor preservation.
8. **Large File Streaming Optimization (PDF & DOCX)**: Support HTTP range requests for PDFs (`HttpRange.kt`, `PdfRangeResponder.kt`) avoiding whole-file 150MB byte reads upfront; stream DOCX without Base64 heap duplication.
9. **Engine Reliability & Sample Verification**: Audit and verify large DOCX, PDF, LOG, TXT, and MD files under high-load conditions.

---

## 2. Technical Architecture & Component Changes

### Component A: PDF Touch Scroller & Gesture Manager
- **File**: `app/src/main/java/com/example/ui/screens/FilePreviewScreen.kt`
- **Floating Scroller**:
  - Implement a sleek right-aligned draggable thumb with an attached floating page indicator bubble (`Page X of Y`).
  - Animates in on touch/scroll and auto-fades after 1.5s of inactivity.
  - Maps vertical drag fraction `(y / height)` to exact 1-based page index.
  - Dispatches `gotoPdfPage(page)` on drag and commits on release without redundant page-rendering churn.
- **Gesture Decoupling**:
  - Implement a combined gesture detector with a 250ms tap timeout window:
    - Tap 1 starts a 250ms timer.
    - If Tap 2 occurs within 250ms, cancel the timer and trigger double-tap zoom (zoom in/out).
    - If 250ms elapses without Tap 2, trigger single-tap (toggle focus mode & system bars).

### Component B: PDF Rounded Loading Skeleton & Sensor Orientation
- **Files**: `app/src/main/assets/pdfjs/search.html`, `app/src/main/java/com/example/MainActivity.kt`, `FilePreviewScreen.kt`
- **Loading Skeleton**:
  - In `search.html`, render page-shaped placeholder cards with `border-radius: 12px`, subtle border, and linear shimmer animation while document proxy loads.
- **Sensor Landscape & Anti-Blink**:
  - In `FilePreviewScreen.kt`, the orientation toggle sets `activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE` (allowing both left and right landscape based on accelerometer). Toggling back restores `ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT`.
  - On viewer exit, restore `ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED`.
  - Set window background to `#F0F0F0` during PDF viewing to eliminate black blink during configuration change.

### Component C: DOCX Viewport Engine & Focal Zoom
- **File**: `app/src/main/assets/editor/docx_editor.html`
- **Engine Redesign**:
  - Eliminate the dual-container `#viewport-wrapper` bug. Consolidate into a single scroller `#document-container`.
  - Establish `fit-width` baseline: On load and on orientation change, calculate zoom so document width matches viewport width (100% = fit width).
  - Focal-point anchored pinch zoom: Track touch center `(centerX, centerY)` in JS `touchstart`/`touchmove`. Apply `transform: scale()` during pinch centered on the touch focal point.
  - On pinch settle (`touchend`), commit the CSS `zoom` and adjust `scrollLeft`/`scrollTop` by focal offset so content under the fingers remains completely stationary.
  - Double-tap gesture: Smoothly toggles between `fit-width` and `200%` centered at the tapped coordinate.

### Component D: DOCX Vector PDF Printing
- **Files**: `app/src/main/java/com/example/utils/FilePrintHelper.kt`, `app/src/main/assets/editor/docx_editor.html`
- **Print Pipeline**:
  - Support `?print=1` mode in `docx_editor.html` (disables interactive toolbar, enables `@media print` layout, sets light background, expands all sections).
  - In `FilePrintHelper.kt`, create an off-screen print `WebView`, load `https://appassets.androidplatform.net/assets/editor/docx_editor.html?print=1`, stream the document bytes, and print via `webView.createPrintDocumentAdapter("Document.pdf")`.
  - Fall back to text extraction only if WebView creation or asset loading throws an unrecoverable error.

### Component E: Large PDF Range Streaming
- **Files**: `app/src/main/java/com/example/core/io/HttpRange.kt`, `app/src/main/java/com/example/utils/PdfRangeResponder.kt`, `search.html`
- **HTTP Range Support**:
  - Implement `HttpRange.parse(rangeHeader, totalLength)` to support `bytes=start-end` requests.
  - In `PdfPreview`'s `WebViewAssetLoader`, intercept `/search-pdf/document.pdf` and serve `206 Partial Content` with `Content-Range: bytes start-end/total` and `Accept-Ranges: bytes`.
  - In `search.html`, configure pdf.js with `disableRange: false` and `disableStream: true` so it queries only required page tables and streams chunks on demand.

### Component F: Settings Screen Redesign (Claude Style)
- **File**: `app/src/main/java/com/example/ui/screens/SettingsScreen.kt`
- **Design System**:
  - Warm paper canvas (`#FAF9F5` in light, `#151514` in dark), terracotta brand accents (`#CC785C`), and M3 glassmorphism cards (`ClaudeCard`).
  - Organized into 4 cohesive groups:
    1. **Appearance**: Theme selection (Light, Dark, System) with visual radio cards; Accent tone indicator.
    2. **Reading & Editor**: Default text encoding, Word wrap toggle, Show line numbers, Font size selection.
    3. **Document Engine & Cache**: PDF quality tier, Auto-save changes on exit toggle, Storage breakdown & Clear Cache button with live byte counts.
    4. **About File Claw**: Version badge, 100% offline guarantee, open-source acknowledgments.

---

## 3. Step-by-Step Implementation Sequence

1. **Step 1: Core IO & Range Responders**
   - Create `com.example.core.io.HttpRange.kt` with unit tests `HttpRangeTest.kt`.
   - Update `PdfPreview`'s asset loader in `FilePreviewScreen.kt` to handle HTTP Range requests for partial PDF streaming.
2. **Step 2: PDF Touch Scroller & Gesture Debouncer**
   - Implement floating draggable pill scrubber with `Page X of Y` bubble in `FilePreviewScreen.kt`.
   - Implement 250ms tap-delay gesture detector handling single-tap (focus mode) vs. double-tap (zoom) across all viewers.
3. **Step 3: PDF Orientation & Loading Skeleton**
   - Update `search.html` with rounded shimmer page skeletons.
   - Update orientation toggle in `FilePreviewScreen.kt` to `SCREEN_ORIENTATION_SENSOR_LANDSCAPE` and fix window background.
4. **Step 4: DOCX Viewport Engine & Print-to-PDF**
   - Update `docx_editor.html` with focal-anchored pinch zoom, fit-width baseline, and `?print=1` mode.
   - Update `FilePrintHelper.kt` with off-screen WebView vector printing.
5. **Step 5: Claude-Style Settings Redesign**
   - Overhaul `SettingsScreen.kt` with grouped glass cards, clean options, and storage management.
6. **Step 6: Verification & Self-Replicating Loop Bump**
   - Run `compile_applet` and unit tests (`gradle :app:testDebugUnitTest`).
   - Update `docs/CHANGELOG.md` and `docs/TODO.md`.
