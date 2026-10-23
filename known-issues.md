# File Claw - Known Issues & Tech Debt
Last Updated: October 16, 2026
Document Version: 1.3
Next Review Date: January 15, 2027

## 1. Issue Classification System
- **Severity**: `CRITICAL` (Crashes/Data Loss), `HIGH` (Core Feature Broken), `MEDIUM` (Degraded Experience), `LOW` (Cosmetic).
- **Status**: `OPEN`, `IN_PROGRESS`, `CLOSED`, `BY_DESIGN`.
- **Area**: `UI`, `FILE_IO`, `PDF_ENGINE`, `DATABASE`, `PERFORMANCE`.

## 2. Active Issues (CRITICAL and HIGH)

| ID | Title | Severity | Status | Area | Description & Workaround |
|----|-------|----------|--------|------|--------------------------|
| BUG-001 | Large Text File UI Freeze | HIGH | OPEN | `FILE_IO` | Files > 10MB cause ANRs due to full read into memory on main thread string assignment. *Workaround*: App limits rendering blocks, but true streaming is missing. |
| BUG-002 | Search Indexing Latency | HIGH| OPEN | `PERFORMANCE`| Searching entire external storage triggers massive GC churn. |

## 3. Active Issues (MEDIUM and LOW)

| ID | Title | Severity | Status | Area | Description & Workaround |
|----|-------|----------|--------|------|--------------------------|
| BUG-003 | PDF Zoom Reset on Rotation | MEDIUM | CLOSED (Loop 14) | `UI` | Root cause fixed: `MainActivity` now handles orientation/screen/theme config changes itself, so the Activity (and its WebView + zoom state) survives device rotation. Device re-verification pending. |
| BUG-004 | CSV Table Horizontal Scroll | LOW| OPEN| `UI`| Extreme widths cause minor clipping on strict bounds. |
| BUG-005 | Password Field Obscuring | LOW| OPEN| `UI`| Keyboard overlaps the unlock dialog on small screens. |
| BUG-006 | PDF placeholder geometry estimated from page 1 | LOW | OPEN (Loop 12 watchlist) | `PDF_ENGINE` | Very large PDFs open instantly now, but page placeholders use page 1's size until each real page is fetched; mixed-orientation documents can shift layout slightly on first approach. Same tradeoff as the official pdf.js viewer. |
| BUG-007 | DOCX pinch preview can transiently overflow horizontally | LOW | OPEN (Loop 12 watchlist) | `UI` | During an active pinch the DOCX zoom preview uses `transform: scale()` (no layout), so wide documents may visually overflow the scroll area mid-gesture; the settle commit restores correct scrolling and scroll position. |
| BUG-008 | HTML preview has no next/prev match navigation | LOW | OPEN | `UI` | HTML files use `findAllAsync` only (highlight, no arrow navigation). Not part of the Loop 12 scope (PDF/DOCX/TXT/MD were audited); needs its own pipeline if wanted. |

### Loop 8 watchlist
- **PDF crisp-zoom memory**: SUPERSEDED by the Loop 12 residency model — the reader now receives a TOTAL RGBA budget across at most 3 resident canvases (per-page tiers unchanged); see the Loop 12 watchlist below.
- **PDF smart-dark classification threshold**: SUPERSEDED in Loop 12 by `PdfDarkModePolicy` (near-binary text detection); see the Loop 12 watchlist below.
- **DOCX pinch vs text selection**: 2-finger gestures are consumed at the Initial pass; long-press selection inside the rendered DOCX is unaffected, but exotic WebView widgets may need per-case validation.
- **Search ranking is name-only**: global search still matches file NAMES, not contents; content search is planned (see `docs/prompts/NEXT_LOOP_PROMPT.md`).

### Loop 10 watchlist (all items implemented; NOT yet device-verified)
- **PDF zoom budget now device-tiered** (`PdfRenderBudgetPolicy`, pushed via `setPdfQualityBudget`): 6M/9M/12M/16M pixels per page by heap tier. Watch `dumpsys meminfo` and logcat for renderer kills at 5× on the 16M tier; if a device class still struggles, the bounded strategy to investigate next is viewport/tile rendering, not removing caps.
- **DOCX pinch transition-skip**: the new `PinchGestureGate` discards pointer-count-change events; verify on hardware that starting a pinch feels immediate (one skipped event should not be perceptible) and that adding/removing a finger mid-pinch does not jump the zoom.
- **DOCX fallback controls hidden**: when the pure-Kotlin text fallback renders a DOCX, zoom in/out/chip are hidden by design; if users ask for zoom there, implement it in the fallback rather than re-enabling dead controls.
- **Edit tray vs IME**: the tray uses the `safeDrawing.bottom` union only while `isEditMode`; verify three-button navigation (navbar + IME must not double-pad) and that opening the keyboard never scrolls/loses the caret or exits edit mode.
- **Back closes find first**: deliberate Chrome-style behavior added in Loop 10; if any flow expected back to leave the document while find is open, revisit before promoting v1.3.1 to stable.
- **PDF blank-flash**: re-renders now swap an offscreen canvas on completion; confirm no flicker remains on slow devices where the settle re-render takes >300ms (the old bitmap stays visible meanwhile by design).

### Loop 12 watchlist (all items implemented; NOT yet device-verified)
- **PDF lazy open + residency**: very large PDFs (100+ pages) should open in roughly constant time and stay bounded in memory (`dumpsys meminfo` before/after a 200-page scroll session; logcat for renderer kills). Page revisits re-fetch the page proxy — confirm scrolling back feels instant on a mid-range phone.
- **Resident window = 3**: if users report visible blank gaps while flipping pages quickly, raise `residentPageWindow` in `search.html` together with `PdfRenderBudgetPolicy.RESIDENT_PAGE_WINDOW` (they must stay in sync) and re-measure memory.
- **Search arrows**: verify with a 7-match document (the original report) in DOCX, TXT, MD and PDF — every press must move the counter 1→7 and wrap in both directions.
- **Dark mode decision**: text pages invert deep as before; photo pages/portraits/charts now stay natural and slightly dimmed. If the dim level feels wrong, tune `brightness()` in the `.pdf-page.natural` CSS rule only — never re-enable blanket inversion.
- **DOCX two-phase zoom**: pinch should track the fingers smoothly (transform preview) and settle crisply (one CSS zoom commit); watch for the settle commit feeling delayed on very slow devices (backstop is 260ms). The percentage chip updates live during the pinch.
- **DOCX streamed bytes**: open DOCX from storage AND from inside a zip; if `/docx-src/document.docx` ever fails, the page falls back to the base64 bridge automatically — confirm the fallback path still renders.
- **Renderer-crash recovery**: hard to trigger deliberately; if the PDF viewer ever shows "The PDF renderer stopped unexpectedly.", the Try Again button must rebuild it (and DOCX must fall back to Clean Document View).

### Loop 14 watchlist (all items implemented; NOT yet device-verified)
- **DOCX real rendering**: the `/assets/` WebViewAssetLoader registration must be smoke-tested with DOCX files from storage AND from inside a zip (the streamed `/docx-src/document.docx` path plus the base64 bridge fallback). If a DOCX ever still lands in the recovery view, the honest banner now shows the renderer's failure reason — report that text.
- **PDF rotate button**: rotate through 90/180/270/0 on a middle page of a large document; page, scroll anchor, search state and zoom must survive; password-protected files must NOT re-prompt. Toolbar order must read: Page indicator, Rotate, Search, Print, Share.
- **Page scrubber**: drag first→last→first on a 500+ page PDF; the bubble counts with the thumb; release jumps without rendering the pages passed over (`dumpsys meminfo` should stay flat during a fast drag).
- **PDF always light**: open a PDF under Android system dark mode — the viewer, its status-bar canvas and page colors must stay light; DOCX dark mode must still work.
- **8x zoom**: pinch to the max on a text page — text stays sharp (1:1 backing at ratio 8), panning works, memory stays within the resident budget; `dumpsys meminfo` before/after.
- **configChanges breadth**: verify no screen regressed from Activity-recreation removal (theme switch, locale change, split-screen resize) — Compose recomposes from configuration, but this is the biggest lifecycle change shipped in a while.
- **Loop 12 carry-overs still open**: BUG-006 (mixed-orientation placeholder shift), BUG-007 (DOCX pinch horizontal overflow), resident-window=3 blank-gap watch, dark-mode dim tuning (DOCX only now).

## 4. Recently Fixed Issues
- **DOCX preview looked like plain text extraction (user-reported, Loop 14)**: every DOCX open failed the main-frame load because the editor was registered under the WebViewAssetLoader prefix `/editor/` while the asset lives at `editor/docx_editor.html` — the loader strips the prefix, so the asset 404ed and the viewer silently fell back to extracted text (introduced by the Loop 12 streaming change; commit `7d11163`). *Fix*: register `/assets/` (documented pattern, same as the PDF reader) and load `/assets/editor/docx_editor.html`; verified against androidx.webkit source AND by rendering a real DOCX in headless Chromium. *Device verification pending.*
- **DOCX find never reported results (found by browser test, Loop 14)**: the Loop 12 rewrite of `docx_editor.html` dropped the `maxSearchHighlights` declaration while `searchWord()` still read it — every matching query threw `ReferenceError`. *Fix*: all five search-engine globals declared explicitly (cap 5000). *Device verification pending.*
- **Rotation re-prompted the PDF password / reset zoom (BUG-003, Loop 14)**: Activity recreation on device rotation destroyed the WebView and the authenticated PDF.js session. *Fix*: `configChanges` on `MainActivity`; plus the new Rotate button is pure viewer state that never touches the WebView lifecycle. *Device verification pending.*
- **PDF dark mode removed (user request, Loop 14)**: toggle, menu item, bridge command, dark CSS, per-page classifier and `PdfDarkModePolicy` deleted; PDFs always render light. Supersedes the Loop 12 "By Design" tradeoff for mixed text+photo pages (no longer relevant — no inversion at all). *Device verification pending.*
- **Search arrows stopped at 2–3 of N matches (user-reported, Loop 12)**: the DOCX and TXT/MD WebView search effects re-ran `searchWord()` on every arrow press, which resets the renderer's active match index to 0 before advancing one step — the counter could never move past 2 regardless of the real match count. *Fix*: pure `InViewerSearchCommandPolicy` plans the commands (query searched once, arrows issue only next/prev deltas, counter resets absorbed); 7 unit tests prove all N matches reachable both directions (`FilePreviewScreen.kt`, `DocumentPreviewSupport.kt`). *Device verification pending.*
- **Very large PDF pages rendered permanently blank (user-reported via "struggling with large PDFs", Loop 12)**: the render-ratio floor of 1.0 plus missing per-dimension cap let oversized canvases exceed Chromium's silent canvas limits. *Fix*: `PdfCanvasDimensionPolicy` — area budget AND 8192px per-dimension cap, 0.05 floor, downsampling instead of blanking; mirrored in `renderPixelRatio()`.
- **Heavy PDFs ballooned memory / opened slowly (user-reported, Loop 12)**: every page was fetched up front and retained forever; extracted search text duplicated the whole document; 3–5 canvases could be resident against a per-page-only budget. *Fix*: lazy placeholder open (page 1 only), `page.cleanup()` + proxy drop on release, resident-window LRU (max 3 canvases), total-RGBA budget across residents, 128-page text LRU (`search.html`, `PdfRenderBudgetPolicy.totalCanvasBytes`). *Device verification pending.*
- **Dark mode made human photos look like negatives (user-reported, Loop 12)**: whole-canvas inversion guarded only by a saturation heuristic ghosted grayscale/low-saturation portraits. *Fix*: invert ONLY confidently near-binary text pages; everything else keeps original colors slightly dimmed (`PdfDarkModePolicy` + per-render classifier). *Device verification pending.*
- **DOCX pinch zoom lagged then jumped on large documents (user-reported, Loop 12)**: every gesture frame applied CSS `zoom`, forcing a full style→layout→paint of the entire rendered document. *Fix*: compositor-only `transform: scale()` preview during the pinch (throttled ~30/s) + one authoritative CSS `zoom` commit on settle with scroll restore (`docx_editor.html` two-phase zoom).
- **Large DOCX open memory spikes (code-path evidence, Loop 12)**: the base64 bridge path peaked at ~5 simultaneous copies of the file. *Fix*: bytes stream through WebViewAssetLoader as one ArrayBuffer (`/docx-src/document.docx`); bridge path kept as in-page fallback.
- **Light-mode status bar icons invisible (user-reported, Loop 7)**: `enableEdgeToEdge()` set icon appearance from the system dark-mode flag once, while the canvas followed the in-app theme. *Fix*: `FilesClawTheme` now re-asserts `isAppearanceLightStatusBars/NavigationBars` on the host window whenever the in-app theme resolves (`ui/theme/Theme.kt`).
- **Preview top bar duplicated the bottom tray (user-reported, Loop 7)**: Search/Outline/Focus/Copy icons were stacked in the top bar. *Fix*: top bar reduced to back + file name + 3-dot menu.
- **Document Outline / Viewer Appearance menu items dead (user-reported, Loop 7)**: flags were set but the bottom sheets were never rendered. *Fix*: both sheets are now wired (outline jump + theme/font/zoom appearance).
- **In-document search appeared broken in MD/TXT (user-reported, Loop 7)**: pipeline was complete; the search field never received focus so users could not type. *Fix*: auto-focus + keyboard raise on open.
- **TextFileSearchEngine overlap double-count**: cross-chunk candidates starting inside a previously counted match were double-counted. *Fix*: byte-range guard + regression coverage.
- **Test suite did not compile**: pre-existing type error in `TextFileSearchEngineTest` (String vs ByteArray). *Fix*: corrected; suite is 134/134 green as of Loop 7.
- **PDF Password Crash**: `PdfRenderer` previously crashed wildly on encrypted files. *Fix*: Implemented `PDFBox-Android` as a robust fallback to decrypt safely into cache. Verified via local tests.
- **PDF zoom blurry at high scale (user-reported, Loop 8)**: pages were rasterized once at ≤2 device pixels per CSS pixel and pinch merely magnified the bitmap. *Fix*: `onScaleChanged` debounce feeds the settled scale to `setPdfRenderScale()` which re-rasterizes visible pages under a ~9M-pixel/page budget (`assets/pdfjs/search.html`, `FilePreviewScreen.kt`).
- **PDF dark mode turned photos ghost-like (user-reported, Loop 8)**: whole-canvas `invert(.88)` inverted faces into negatives. *Fix*: per-page classification (48px downsample, >4% saturated pixels ⇒ `photo` class) keeps photo pages natural with a slight dim; text pages get a full luminance inversion.
- **Focus-mode transition snapped/flashy (user-reported, Loop 8)**: content padding flipped binary at frame 0, status-bar inset flipped instantly, and PDF pages re-rendered (blank flash) per resize frame. *Fix*: scaffold insets always applied (animate frame-by-frame), unconditional `statusBarsPadding()`, matched 220–260ms tween specs, debounced JS resize + skip-if-unchanged geometry.
- **Global search listed deleted files (Loop 8)**: results are existence-filtered after relevance ranking (`FileSearchRanker`).
- **Search-term history corrupted by `|*|` in queries (Loop 8)**: storage migrated to a JSON array with legacy-format fallback read.

## 5. Technical Debt Register
- **Architecture Debt**: `MainViewModel` is a God Class. It manages Home, Preview, Editor, and Navigation. MUST be split into scoped viewmodels. *(Loop 7 note: inline edit state was added minimally; a full split is planned behind the MD/TXT loops.)*
- **Testing Debt**: Automated test coverage was 0% historically; Loop 7 restored a green 134-test suite and added the first UI-flow helper tests. Keep expanding (target: 80% core coverage via `jacocoCoreVerify`).
- **I/O Debt**: `FileManager` utilizes older `java.io.File` APIs synchronously in many places instead of non-blocking `NIO` or strict Coroutine stream channels.
- **String Debt**: Hardcoded UI strings exist across all Compose screens.
- **Build Debt**: `docs/TODO.md` notes `gradlew` is tracked without the executable bit in some checkouts; run `chmod +x gradlew` after cloning (Loop 7 verified builds need this).

## 6. Platform Limitations
- **Android Scoped Storage**: Strict file limits prevent raw `java.io.File` modifications in standard `Documents` or `Downloads` directories on API 30+ without `MANAGE_EXTERNAL_STORAGE`.
- **Compose Lazy Lists**: Rapidly scrolling thousands of heavy items with complex thumbnail logic can drop frames on lower-end devices.

## 7. Third-Party Limitations
- **PDFBox-Android**: Extremely heavy dependency (~5MB). Updates are incredibly slow/orphan state, but required because native Android `PdfRenderer` explicitly lacks password support.

## 8. Won't Fix / By Design
- **DOCX Formatting Loss**: Custom parser strictly isolates text. Retaining word-perfect structural formatting (images, fonts, alignments) is `BY_DESIGN` ignored to keep the app lightweight and avoid bloated Apache POI imports.
- **No Cloud Sync**: App is marketed and designed as purely offline file manager. Network APIs are excluded intentionally.
- **PDF has no dark mode (Loop 14)**: removed by scope decision — PDF pages always display their original light colors; no classifier, no filters. DOCX/TXT/Markdown keep dark mode.
- **Tile rendering at extreme PDF zoom (Loop 12)**: a bounded viewport/tile design exists on paper but cannot be verified (tile coordinates, scroll/highlight/cleanup interplay) without a device; shipping a partial tile implementation is explicitly out of scope. Whole-page rendering with dimension caps remains the shipped behavior — see `docs/prompts/NEXT_LOOP_PROMPT.md` backlog.

## 9. Issue Triage Rules
- Users/QA submit items mapped to the BUG format.
- Maintainer assigns Severity.
- `CRITICAL` forces a hotfix branch. `HIGH` enters the next sprint.

## 10. Metrics and Trends
- Currently tracking high error rates on nested ZIP files (Unimplemented).
- Memory optimization is the highest trending issue area based on internal benchmarks.

---
**Update Log**
- 2026-06-15: v1.0 - Initial document creation.
- 2026-10-16: v1.3 - Loop 14: BUG-003 closed (configChanges), DOCX WebViewAssetLoader + find regressions recorded, Loop 14 watchlist, PDF dark-mode removal noted; bumped document version.
- 2026-10-16: v1.2 - Added Loop 10 watchlist (header find bar, IME-aware edit tray, DOCX pinch gate, device-tiered PDF budget, offscreen swap render); bumped document version/date.
