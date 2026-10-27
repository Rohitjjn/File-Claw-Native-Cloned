# Performance and resource limits

**Source review date:** 2026-10-23. This is a source-based guide to current safeguards and known scaling behavior. It is **not** a benchmark report: no device timings, memory profiles, frame-rate measurements, or comparative performance tests were collected for this worktree. Limits in source are not guarantees of smooth operation on every Android device.

For build/test state, see [`docs/STATUS.md`](docs/STATUS.md). For reader-specific routes, see [`docs/PREVIEW_PIPELINES.md`](docs/PREVIEW_PIPELINES.md). The format-level limits are summarized in [`docs/FILE_FORMATS.md`](docs/FILE_FORMATS.md), and archive/WebView safety notes are in [`docs/SECURITY.md`](docs/SECURITY.md).

## Plain-language summary

File Claw uses bounded readers and viewer-specific limits to avoid loading or drawing an entire large file at full resolution. PDF.js releases offscreen page canvases and adjusts its render budget using Android memory signals. Text, table, archive, and 3D paths also have source-defined limits. These controls reduce risk; they do not establish measured performance or make every file safe to open.

## Current source safeguards

### PDF rendering

The Kotlin policy is `app/src/main/java/com/example/formats/pdf/PdfRenderBudgetPolicy.kt`; the matching implementation is in `app/src/main/assets/pdfjs/search.html`.

| Device signal | Canvas pixel budget per resident page | Approximate RGBA backing bytes for the 3-page target window |
|---|---:|---:|
| Android reports a low-RAM device | 6 million pixels | 72 MB |
| Memory class is unavailable | 12 million pixels | 144 MB |
| Memory class is at least 256 MB | 16 million pixels | 192 MB |
| Memory class is 128–255 MB | 12 million pixels | 144 MB |
| Memory class is below 128 MB | 9 million pixels | 108 MB |

The byte column is a **calculation from the policy**, not a measured process-memory limit: pixel count × 4 RGBA bytes × 3 resident pages, using decimal MB. `ActivityManager.memoryClass` is the per-app Java heap class, not the device's physical RAM; the canvas is rendered by WebView/Chromium, so this is a sizing signal rather than a direct memory measurement. Renderer, GPU, PDF.js, DOM, and temporary render allocations add overhead.

Additional PDF controls in source:

- `PdfRenderBudgetPolicy.RESIDENT_PAGE_WINDOW` targets three rendered pages. The reader releases older offscreen canvases first and does not evict visible pages; if more than three pages are simultaneously visible, the actual count can exceed the target.
- PDF.js uses `IntersectionObserver` with a 900 CSS-pixel vertical root margin to request/render pages near the viewport. It drops page proxies/canvases when records are released. It still creates a section, canvas element, and highlight layer for every page, and yields to the event loop every 32 records; there is no fixed page-count ceiling in this path.
- `PdfCanvasDimensionPolicy` and the matching JavaScript cap a canvas dimension at 8,192 backing pixels, clamp render scale to at most 8, and allow downsampling below 1× (floor 0.05) for unusually large pages. Kotlin and JavaScript constants must remain aligned.
- The PDF endpoint uses `PdfRangeResponder` for byte-range requests, so PDF.js can request parts of a local file. Range serving does not remove the cost of rendering page canvases.

Do not raise canvas, dimension, or resident-page budgets to address a blank/blurred page without profiling on low-memory and high-density devices. The dimension and area caps prevent oversized allocations and Chromium canvas failures.

### DOCX and PPTX WebView handoff

`formats/ooxml/OoxmlDocumentHandler.kt` sends files up to **25 MiB** through a base64 WebView state; larger files use the existing archive route. Base64 text alone is roughly four-thirds the input byte size. The source also reads the file and creates WebView/JavaScript copies, so actual transient memory use can be higher. The 25 MiB threshold is a route guard, not a promise that every document below it renders within a particular time or memory budget.

Keep the WebView HTML, JS libraries, document URI, and MIME responses local and bounded. Do not raise the threshold or add parallel whole-file copies without measurements and an explicit product decision.

### Text, search, and tables

- Text preview handlers cap a displayed preview at **250,000 characters** and mark truncation separately so a partial view is not saved as the full original. The general `TextLoader` default is **2,000,000 characters**; `TextOperationReader` uses that default for larger operations.
- `TextFileSearchEngine` streams through a **64 KiB** default buffer, keeps up to **10,000** hit offsets by default, and rejects queries longer than **4,096 characters**. It still scans the file to count matches; it does not load the complete file into one string.
- Device filename indexing runs on `Dispatchers.IO` and is capped at depth **8** and **60,000** files. Name search is debounced by **150 ms**, ranks a bounded candidate set, and displays up to **150** results. The index is cached. If the index is empty, a background rebuild is started, but the current search takes its snapshot without waiting for that rebuild to finish.
- CSV/TSV/PSV parsing keeps its existing **50,000 rows × 256 columns** loader cap. XLSX parsing and the shared grid cap display/save at **20,000 rows × 100 columns**, **4,000 characters/cell** and **8,000,000 total characters**. XLSX reads the first sheet. Those are independent upper bounds, not a memory target; a dense table close to both limits can still be large, so the grid is virtualized.
- The legacy XLS parser reads a workbook stream capped by the OLE helper at **30,000,000 bytes** and limits its displayed grid to **5,000 rows × 100 columns**. The generic binary viewer reads at most **4 KiB**.

### Archives and other large-format paths

- Archive tree construction stops after **200,000 entries** and flags unsafe/deep paths (configured depth **64**). `ArchiveTreeBuilder` uses a path index and iterative sorting; the source describes its construction as O(n log n).
- Extraction is capped at **2 GiB per entry**. The **2,000:1** compression-ratio check above **4 MiB** is only possible on paths that provide compressed-size metadata (the ZIP/Zip4j path). Other extraction paths still apply the per-entry streamed-byte cap.
- `ArchiveLimits.maxTotalBytes` is declared as **16 GiB** but is not enforced. `ArchiveTree.totalSize` only records declared sizes. Do not rely on an aggregate archive-output cap until that source gap is addressed and tested. Archive listing also does not apply the per-entry extraction-size cap.
- OpenDocument text extraction caps XML input at **64 MiB** and output at **5,000,000 characters**. EPUB processing caps a chapter input at **16 MiB** and generated preview text at about **3,000,000 characters**.
- STL/OBJ parsing accepts up to **300,000 triangles**. The 3D viewer samples triangles for drawing using a stride based on a 25,000-triangle target; it is a rendering sample, not a model-file size limit.

## What has and has not been measured

Source inspection confirms the limits and paths above. Headless Chromium 153 on the local x86-64 sandbox provided these isolated renderer measurements (not Android app heap measurements):

- Basic DOCX visible render: ~1.3 s including local page/library/document requests.
- DOCX late 1×1 → 390×844 layout with `ResizeObserver` removed: ~1.8 s, visible 366×474 page.
- Rich DOCX (5 rendered pages, 96 search matches, zoom to 200%): ~1.3 s, ~8.6 MiB Chromium JS heap.
- Rich PPTX print mode (8 slides, one image, one table): `onPrintReady` reached, ~7 MiB JS heap.
- 5,001-row spreadsheet payload: ~0.1 s to ready, 125 visible DOM cells, ~2.6 MiB JS heap; 31-row basic payload also used 125 visible cells.
- Adversarial spreadsheet payload and corrupt/truncated Office files produced no browser page errors.

These are reproducible through `tools/office-preview-tests/`. They do **not** provide:

- Time to first PDF page, DOCX first render, archive listing, or filename-search response on Android.
- Peak app, WebView renderer, GPU, or native memory on any device.
- Scrolling/zoom frame rate, thermal impact, battery use, or performance under concurrent app load.
- Maximum reliable PDF page count or practical table size on supported devices.

Do not copy performance numbers from old notes, prompts, or logs as current measurements unless the device, Android/WebView version, file fixture, method, and result are recorded.

## Recommended performance checks

Before changing a resource limit or renderer policy, record Android/API and WebView versions, device memory class and low-RAM flag, file size/page or row count, source type, and the exact action being measured. Do not capture document contents or sensitive paths in logs.

On representative low-, mid-, and high-memory devices, measure:

1. Cold and warm open; time to the first usable page/table/text, not just Activity launch.
2. PDF first page, rapid scroll, page jump, password flow, zoom settle, mixed page sizes, and a high-page-count document. Watch both app and WebView renderer memory and renderer-crash logs.
3. DOCX/PPTX at increasing package sizes up to the 96 MiB compressed inspector cap; confirm render, retry, explicit rejection and memory behavior. There is no longer a flat 25 MiB archive-route fallback.
4. Large text preview/search, dense CSV/XLSX near their row/column caps, and archive listing/extraction with many entries and high expansion ratios.
5. Repeated open/close cycles and return-from-background behavior to detect retained WebViews, canvases, or stale cache data.

Run the relevant unit tests and Gradle gates from [`BUILD_GUIDE.md`](BUILD_GUIDE.md), then update [`docs/STATUS.md`](docs/STATUS.md) with exact outcomes. Android/WebView measurements require a built app on a device or emulator; static source checks alone are not performance evidence.
