# Changelog

Format: [Keep a Changelog](https://keepachangelog.com). Numbers marked **(measured)** were produced by a real
run in the build sandbox; **(not verified)** means the change could not be compiled or run there (no Android SDK,
no Maven access). See `TODO.md` → KNOWN ISSUES.

## [Unreleased] Loop 15 (v4) — DOCX Pinch/Print, Sensor Orientation, Large File Streaming & Claude Settings

### Added
- **PDF Right-Edge Floating Scroller**:
  - Implemented floating pill handle with interactive page bubble on touch/drag showing `Page X of Y`.
  - Supports smooth continuous drag from page 1 to the end, accurately jumping to and anchoring on the released page.
- **Unified Single-Tap vs Double-Tap Gesture Resolution**:
  - Implemented 250ms debouncer window across all file viewers (DOCX, PDF, TXT, Markdown, Web), cleanly distinguishing one-tap focus mode (bars visibility toggle) from double-tap zoom without conflict.
- **PDF Rounded Shimmer Skeleton**:
  - Replaced sharp placeholder cards with 12px rounded-corner page cards and soft ambient shimmer animation.
- **Settings Screen Claude Redesign**:
  - Redesigned `SettingsScreen.kt` with warm glass cards (`ClaudeCard`), streamlined options into 4 clean sections (Appearance, Reading & Editor, Storage & Performance, About).
  - Added live preview cache manager with size readout and one-tap clear dialog.
- **DOCX Focal-Point Pinch Zoom & Fit-to-Width Baseline**:
  - Rewrote `docx_editor.html` viewport engine with a single scroller, fit-to-width baseline (100% = page fits viewport width, refitted on rotation).
  - Implemented JS-level multi-touch focal-point pinch zoom with compositor-only preview, single CSS zoom commit, and double-tap toggle (fit ⇄ 200%).
  - Removed fighting Compose `pointerInput` pinch interceptor.
- **Real Vector DOCX Print-to-PDF**:
  - Refactored `FilePrintHelper.printDocx` to load dedicated off-screen WebView with `docx_editor.html?print=1`.
  - Generates authentic vector Word document pages via Android `PrintDocumentAdapter`, preserving formatted typography, tables, and images instead of plain text.
- **Sensor Landscape & Seamless Rotation**:
  - Created `ScreenOrientationPolicy` using `SCREEN_ORIENTATION_SENSOR_LANDSCAPE` for accelerometer-based dual-side landscape rotation.
  - Set window background to light `#F0F0F0` and `loadWithOverviewMode = false`, eliminating black blink during screen rotation and keeping visible page anchor.
- **Large File Streaming via HTTP Range Requests**:
  - Implemented `HttpRange` RFC 7233 parser and `PdfRangeResponder` returning `206 Partial Content` with `Accept-Ranges: bytes`.
  - Enabled `disableRange: false` in pdf.js, streaming large documents (150MB+) on demand with millisecond open times.
- **Unit Tests**:
  - Added `HttpRangeTest`, `ScreenOrientationPolicyTest`, and updated `DocxZoomPolicyTest`.
  - All 225 unit tests passed with 100% success rate.

## [Unreleased] Loop 3 (v3) — Source URI Write-Back, Legacy Office (.xls/.doc), Streaming Huge-Text Reader & Trash Bin

### Added
- **Write-Back to Source URI**:
  - Added `sourceUri` column to `RecentFileEntity` to persist origin content URIs for files opened from external applications (Downloads, Files, Email attachments).
  - Implemented Room `MIGRATION_3_4` (`ALTER TABLE recent_files ADD COLUMN sourceUri TEXT DEFAULT NULL`) and bumped `AppDatabase` version to 4.
  - Implemented honest write-back in `MainViewModel` via `contentResolver.openOutputStream(uri, "wt")` when saving text and spreadsheet files, syncing changes back to external providers.
  - Added `DatabaseMigrationTest` verifying schema migration and entity persistence.
- **Legacy Office Format Decoders (.xls & .doc)**:
  - Created pure-Kotlin `Ole2CompoundFile` parser decoding Microsoft OLE2 Compound File Binary (CFB) stream hierarchies offline.
  - Implemented `Biff8XlsParser` reading BIFF8 Excel workbooks (BOF, BOUNDSHEET, SST, LABELSST, NUMBER, RK, MULRK) into structured 2D sheet grids for `CsvPreview`.
  - Implemented `LegacyDocExtractor` extracting clean text paragraphs from legacy Word 97–2003 `.doc` files for `TextSuccess` preview.
  - Covered with unit tests `Biff8XlsParserTest` and `LegacyDocExtractorTest`.
- **Massive-Text Streaming Window Reader**:
  - Implemented `TextWindowReader` for opening 100 MB–500 MB text/log files with O(1) memory by seeking and reading clean newline-aligned byte windows via `RandomAccessFile`.
  - Added `TextWindowReaderTest` verifying newline boundary preservation and line estimation.
- **Safe Multi-File Operations & Gutter Scroll Sync**:
  - Added `BatchAction.TRASH` and `moveToTrash` in `BatchFileOperations` for recoverable soft deletion before permanent unlinking.
  - Synchronized vertical scroll state between line-number gutter and typing console in `EditorScreen`.
- **Loop v4 Autonomous Self-Replicating Prompt**:
  - Generated `NEXT_LOOP_PROMPT_v4.md` and `NEXT_PROMPT.md` for continuous self-improvement.

### Verification
- **Passed:** `compile_applet` build succeeded; `./gradlew :app:testDebugUnitTest` executed 27/27 tasks successfully with 0 test failures across 213 tests.

## [Unreleased] Loop 2 (v2) — In-Document Global Search, DOCX Clean Architecture & Batch Operations

### Added
- **Global Text Search in Document Previews**:
  - Full-document text search and occurrence highlighting implemented across **PDF, DOCX, TXT, and Markdown** previews.
  - PDF: Multi-page text highlighting via canvas overlay and search result navigation with occurrence counts.
  - DOCX: In-DOM `<mark class="search-highlight">` and `.search-highlight-active` highlighting with smooth scrolling to active match.
  - TXT & Markdown: `SearchableDocumentWebView` and `DocumentPreviewScript` highlights all matches and updates occurrence badge with seamless next/previous navigation.
- **Batch Selection Mode in File Browser**:
  - Multi-select files and folders in both `FileTreeComponent` and `AllFilesScreen`.
  - Batch **Move**, **Copy**, and **Delete** actions supported with batch progress feedback and permanent deletion safeguards.
  - Powered by pure-Kotlin `BatchFileOperations.kt` with thread safety, recursive handling, and path sanitization.
- **Clean DOCX Preview**:
  - Debugged and cleanly isolated DOCX preview pipeline using local asset streaming via `WebViewAssetLoader`, robust zoom handling, and honest fallback to pure-Kotlin OOXML text recovery without altering the established Claude UI.
- **Loop v3 Evolution**:
  - Generated `NEXT_LOOP_PROMPT_v3.md` and `NEXT_PROMPT.md` adhering to the Self-Replicating Prompt Rule for cycle continuation.

### Verification
- **Passed:** `compile_applet` build succeeded; `./gradlew :app:testDebugUnitTest` executed 27/27 tasks successfully with 0 test failures.

## [Unreleased] Loop 6 — PDF, DOCX, TXT, Markdown reader improvements

### Added
- Bundled PDF.js 4.10.38 locally with its worker, CMaps, standard fonts, and license notices. Added an offline `WebViewAssetLoader` reader with whole-document PDF text search/highlights, page navigation/bookmarks, password callbacks, reader-style dark canvas, and PrintManager export; search distinguishes the exact match count from retained navigation hits. Removed the obsolete unbounded base64 PDF branch from the generic WebView preview.
- Added shared local TXT/Markdown search/highlight and fenced-code copy controls. TXT fence parsing now supports both backtick and tilde fences and only copies the code body.
- Added bounded-memory `TextFileSearchEngine` for truncated TXT/Markdown files: it scans the whole file, retains at most 10,000 navigable byte offsets, and loads a bounded excerpt for each selected match. Added unit tests for buffer-boundary, newline, Unicode, UTF-16, result-cap, cancellation, and code-fence cases; tests were not run here.
- Added exact DOCX zoom percent entry/reset alongside the 25%–500% slider and 100% default. Search, theme, and zoom operate on the rendered document; print uses the rendered WebView when ready and an extractor fallback otherwise.

### Fixed / changed
- TXT/Markdown preview, print, and copy no longer load a truncated large file wholesale into memory. Large-file print/copy use a 2,000,000-character cap and show a visible error instead of silently using the current excerpt. Printed DOCX/TXT titles and body text are HTML-escaped.
- Immersive preview draws under the system bars, uses reader-canvas theme for system-bar icon contrast, and hides the search overlay while focus controls are hidden. DOCX local assets no longer receive cross-file/universal file access, and external top-level navigation is blocked.
- Organized historical prompt files under `docs/prompts/` with loop-identifying names; removed the exact duplicate `NEXT_PROMPT.md`; created `docs/prompts/LOOP_07_PROMPT.md` for the same four formats.

### Verification
- **Passed:** `git diff --check`; Node.js 22.22.3 syntax checks for extracted PDF.js reader scripts, DOCX script, TXT/Markdown shared script, and bundled PDF.js modules; Python lexical delimiter sanity on 11 changed/added Kotlin files (not a compiler check).
- **Not run:** Android/Gradle build, Kotlin compilation, unit tests, lint/detekt, emulator/device tests, actual PrintManager preview/export, performance benchmarks. `java` and `kotlinc` were missing, `ANDROID_HOME` was unset, and the wrapper is not executable in this checkout; invoking it through `bash` failed because Java is unavailable. No performance numbers were measured.
- Arena Agent Mode did not expose the underlying model identifier. Hermes Agent was inspected but not cloned, installed, or run; no multi-agent team ran.

## [1.4.0] "DOCX Engine Fix, Glass Properties Sheet & PDF Resume": versionCode 6

### Fixed
- **DOCX File Preview Fixed & Pure-Kotlin Fallback Engine**:
  - Fixed WebView DOCX preview by localizing assets into `editor/` directory and resolving cross-origin/file-access CORS restrictions.
  - Resolved base64 IPC Binder transaction limit by exposing `getDocxBase64()` over Android JavascriptInterface.
  - Implemented pure-Kotlin offline OOXML extractor (`DocxExtractor.kt`) that parses `word/document.xml` using `ZipInputStream` without relying on WebView or external dependencies.
  - Added unit test suite `DocxExtractorTest.kt` verifying offline text, word counts, and paragraph extraction.

### Added
- **Glassmorphism Modal Bottom Sheet for File Properties**:
  - Replaced basic AlertDialog with a modern Jetpack Compose `ModalBottomSheet` (`FilePropertiesBottomSheet.kt`).
  - Added drag handle, glassmorphic backdrop, formatted file size, exact byte count, MIME type chip, last-modified timestamp, and permission status.
  - Integrated asynchronous cryptographic checksum calculation (MD5 and SHA-256) with one-tap copy buttons.
  - Storage path box with one-tap "Copy Path" action and clipboard confirmation.
- **Staggered Spring & Fade-in Reveal Animation**:
  - Added `StaggeredAnimatedItem` in `ClaudeComponents.kt` providing staggered spring translation and smooth alpha fade-in across all file cards in `HomeScreen` and `AllFilesScreen`.
- **PDF Last-Read Page Bookmark & Auto-Resume**:
  - PDF viewer now automatically saves the user's active page in `SharedPreferences` upon scrolling.
  - Re-opening any PDF automatically restores the user to their last-read page and displays a confirmation notification ("Resumed at page X").
- **Native Print & PDF Export via PrintManager**:
  - Implemented `FilePrintHelper.kt` utilizing Android's native `PrintManager` and `PrintDocumentAdapter`.
  - Added "Print / Export as PDF" actions to the top app bar and floating action pill for Text, Markdown, Code, and Image files.
- **Quick "Go to Line" in Text & Code Viewer**:
  - Added "Go to Line" dialog with direct line-number entry.
  - Code/Text viewer tags each line and smoothly scrolls to the target line with a temporary highlight flash.

## [1.3.0] "Floating Action Pill, Grid View & In-Viewer Search": versionCode 5

### Added
- **Unified Floating Viewer Control Bar (Action Pill)**:
  - Added a lightweight, floating M3 glassmorphism action pill across PDF, Image, and Text viewers.
  - PDF: quick Page Indicator / Jump to page pill (`current / total`), in-viewer search toggle, night/light mode toggle pill, and share action.
  - Image: photo counter badge, and quick share action.
  - Text: in-viewer search toggle, copy all text shortcut, edit button, and quick share action.
  - Smoothly auto-fades and toggles on screen tap with animated slide and fade transitions.
- **Grid vs List View Toggle & Rich File Cards**:
  - Added a compact Grid / List toggle icon button in `HomeScreen` and `AllFilesScreen`.
  - Grid View displays a 2-column layout with high-contrast `FileExtensionBadge` (PDF, TXT, PNG, JSON, etc.), preview thumbnail, quick star button, file name, formatted size, and relative timestamp.
- **Favorites / Starred Files & Filter Tabs**:
  - Added interactive Star button (`Icons.Filled.Star` / `Icons.Outlined.StarOutline`) to every file card in both list and grid views.
  - Added Filter Tabs (`All Files` vs `★ Favorites`) across `HomeScreen` and `AllFilesScreen` allowing instant filtering with empty-state guidance.
  - Persistent favorite state stored across sessions in `SharedPreferences`.
- **In-Viewer Text Search (PDF & Text files)**:
  - Added a floating "Find in document" overlay search bar with match count indicator (`Match X of Y`) and Previous (`<`) / Next (`>`) navigation buttons.
  - Text files: matches occurrences across the document, highlights them, and updates search match positions.
  - PDF documents: pure-Kotlin `PdfTextSearcher` parses uncompressed and FlateDecode streams to locate matches and jump directly to matching pages.
- **Pure-Kotlin `PdfTextSearcher` & Unit Tests**:
  - Implemented `PdfTextSearcher.kt` in `com.example.core.text`.
  - Added comprehensive unit tests in `PdfTextSearcherTest.kt` verifying uncompressed and compressed stream search.

## [1.2.1] "Viewer Polish & Smart PDF Dark Mode": versionCode 4

### Added
- **Copy Text Action Bar Shortcut**: Added a dedicated copy icon in the top app bar for all text-based files (`txt`, `md`, `json`, code) to copy entire contents to clipboard with instant feedback.
- **Deep Zoom Support**:
  - Image viewer now supports up to 15x zoom factor via `ZoomSpec(maxZoomFactor = 15f)`, providing deep magnification comparable to Google Photos and gallery apps.
  - PDF viewer now supports up to 10x zoom factor (`maxZoom = 10.0f`, `midZoom = 3.5f`), more than triple previous limits.
- **Smart Hue-Preserving PDF Night Mode**:
  - Implemented a specialized document color matrix for PDF night reading that inverts white page backgrounds to dark charcoal (`#1e1e1e`) and black text to comfortable off-white (`#e1e1e1`), while preserving natural color hues and skin tones so photographs and figures do not suffer from inverted "ghost" negative artifacts.
- **Zoom-Out Below 1x for Text Files**: Added `minimum-scale=0.25` in viewport meta and enabled zoom controls across in-app browser previews (`txt`, `md`, `json`, code), allowing users to zoom out down to 25%.

### Changed
- **Default In-App Browser for Text Previews**: All text files (`txt`, `md`, `json`, etc.) now default directly to the premium in-app browser preview matching Claude design aesthetics.
- **Cleaned Preview Menu**: Removed redundant "Show Syntax Code" toggle from 3-dot menus in text/markdown/json files.
- **Image Preview Theme Adaptation & Clean Header**:
  - Image background now adapts dynamically to system theme (light in light mode, dark in dark mode).
  - Removed duplicate overlay showing image name and secondary 3-dots menu beneath the top header for a full edge-to-edge experience.
- **Compact Single-Line Search Box**: Redesigned search bars across `SearchScreen`, `HomeScreen`, and `AllFilesScreen` into a compact, single-line 40.dp rounded pill design with integrated search and clear actions.

## [1.2.0] "Batch Operations & Verification": versionCode 3

### Added
- **Batch Selection Mode in File Browser (`FileTreeComponent`)**:
  - Multi-select mode activated via top "Select" button or long-pressing any file or directory.
  - Interactive checkboxes with smooth selection state management and "Select All" / "Deselect All".
  - **Simultaneous Batch Delete**: Deletes multiple selected files and directories with confirmation preview dialog and instant database/cache cleanup.
  - **Simultaneous Batch Copy**: Non-destructive batch copy with recursive directory support, unique renaming collision safeguards, and interactive destination folder picker.
  - **Simultaneous Batch Move**: Atomic move with cross-filesystem fallback, cycle/self-containment validation, and automatic database index path synchronization.
- **Batch Selection Mode in Recent History (`AllFilesScreen`)**:
  - Multi-select files in history with options to batch remove from history or permanently delete from disk.
- **Pure-Kotlin `BatchFileOperations` Engine & Unit Tests**:
  - `BatchFileOperations.kt` with thread-safe copy, move, and recursive delete routines.
  - Full unit test coverage in `BatchFileOperationsTest.kt` verifying deletion, unique copy name preservation, cycle prevention, and directory moves.
- **GitHub Actions Release Workflow**:
  - Added `.github/workflows/build-and-release.yml` with `workflow_dispatch` manual trigger for parameterized versioning (`version_name`, `version_code`) and automated release artifact publishing.
  - Fixed runner initialization error (`Failed to find package 'tools'`) by removing deprecated `android-actions/setup-android@v3`, configuring native runner Android SDK and `cmdline-tools`, adopting `gradle/actions/setup-gradle@v4`, and adding automated fallback keystore generation and GitHub CLI release publishing.

### Fixed
- **Corrupt PDF Feedback**: Replaced endless loading spinner with honest error UI displaying exact failure diagnostics and a "Go Back" action.
- **Password Field Masking**: Protected user privacy in archive password and PDF password dialogs using `PasswordVisualTransformation`.
- **Honest Auto-Save Messaging**: Renamed "Auto-save Drafts" to "Auto-save on Exit" in Settings and updated editor notification from "Draft auto-saved" to "Changes saved to file" so users know changes write directly to disk.

## [1.1.0] "Foundation & Safety": versionCode 2

### Scoreboard (v1.0 → v1.1.0)
| Metric | v1.0 | v1.1.0 |
|---|---|---|
| Unit tests | 0 | **91** passing (measured) |
| Line coverage of `core/` | n/a (no core, no tests) | **98.3 %** (993/1010), branches 83.2 % (measured, JaCoCo) |
| Registered extensions | ~37 explicit routes + implicit text fallback | **399** (284 full, 32 partial, 83 identify-only) (generated from `FormatRegistry`) |
| New format families with a working route | n/a | **13 families / 63 extensions**: SQLite, SVG, RTF, OpenDocument, EPUB, TTF/OTF fonts, STL, OBJ, gz/bz2/xz files, tar.bz2/tar.xz, ar/deb/cpio, ZIP packages (apk, jar, whl, …), HEIC/AVIF/TIFF/DNG |
| Formats that silently misbehaved | 5 (`.ppt`, `.xls`, `.rar`, plain `.gz`, `.tsv`) | 0 silent: each now parses correctly or says plainly it isn't supported |
| Kotlin source | 9,865 lines / 24 files | 10,653 main lines / 40 files, plus 1,107 test lines |
| `FileManager.kt` | 963 lines | 390 lines |
| `FilePreviewScreen.kt` | 3,880 lines | 2,980 lines |
| Dead code removed | n/a | ~1,500 lines + 4 `.bak` files + 8 one-off patch scripts + a committed build log |
| Unused dependencies removed | n/a | pdfbox-android (~7 MB), kotlin-csv, fastexcel-writer, kotlinx-serialization |

### Benchmarks (JVM, 256 MB heap, `tools/bench/CoreBench.kt`; desktop JVM, **not** device numbers)
| Scenario | Old | New |
|---|---|---|
| Charset check on a 500 MB text file | `OutOfMemoryError` (`readBytes()` of the whole file) | 46 ms |
| Load a 500 MB text file for preview | not possible | 45 ms (250k-char window, flagged truncated) |
| Read a 64 KB window at offset ~499 MB | n/a | 1 ms |
| Archive tree, 100,000 files in one folder | 23,773 ms (O(n²) sibling scan) | 184 ms (≈130×) |
| Archive tree, 100,000 files nested | 373 ms | 562 ms: **slower**, because the new builder also builds a path index and sorts |

Cold start, scroll fps, crash-free rate: **not measured** (needs a device). The only startup change is removing a
hard-coded 500 ms splash wait (now 150 ms) and moving cache-directory I/O off the main thread.

### Fixed: data loss
- **Editing a text file over ~250 KB silently truncated it on save** and wrote a "[truncated]" marker into the file.
  Truncation is now a flag; truncated and converted views are read-only and the ViewModel refuses to save them.
- **Spreadsheets auto-saved on every keystroke**; for `.xlsx` that flattened a multi-sheet workbook to one sheet of
  plain values. Excel is now view-only; CSV/TSV has an explicit Save button and an unsaved-changes prompt.
- `.xls` content was written under an `.xls` name as xlsx bytes; `.tsv` was parsed and saved as comma-CSV. Both fixed.
- All writes were truncate-then-write (a crash left an empty file) and `updateZipEntry` deleted the original before
  renaming. Everything now writes to a temp file, fsyncs, and atomically renames.
- Removed `writeDocxText` / `writePptxText` (unused, but would have destroyed formatting; the PPTX one also emitted
  malformed XML).
- Re-opening the same file name overwrote the earlier imported copy; imports now get unique names, or reuse an identical copy.
- "Remove from history" immediately re-opened the same file (restore effect re-fired on `Idle`).
- Rotation re-imported the intent's file / re-ran shortcuts (effect keyed on non-state `Activity.intent`).

### Fixed: crashes and silent failures
- `determineCharset` read entire files into memory; UTF-16 text was misclassified as binary.
- 7z extraction allocated `ByteArray(entry.size.toInt())`; extraction is now streamed and limit-checked.
- `.rar` showed an empty folder; corrupt/unsupported archives now show a real error.
- Plain `.gz` was parsed as TAR (garbage); handled as a single compressed file.
- Search index covered depth 3 / 5,000 files (missed most of a real device); now depth 8 / 60,000, appended instead of rewritten per open.
- `CacheManager`'s `LruCache` counted entries, not bytes (never bounded memory); cleanup wiped the whole Coil disk cache and deleted open archive entries. Rewritten (age-based, lazy dirs).
- Room used `fallbackToDestructiveMigration()` (any schema bump wiped settings and history). Now fail-fast; schema export enabled.
- PPTX state had no file, so title/share/properties were blank.

### Security
- Zip-bomb and zip-slip protection (entry/size/ratio/depth limits; unsafe names are shown but never extractable).
- `FileProvider` no longer exposes the whole filesystem (`root-path` removed).
- `allowBackup=false` (imported documents were being backed up to the cloud); `BROWSABLE` removed from the VIEW filter.
- HTML previews: scripts off by default, no JS bridge, navigation locked to local files. Markdown/EPUB HTML sanitised.
- All other WebViews: file/content access off (bundled assets still load).
- Untrusted display names sanitised (`PathGuard`); SQLite opened read-only with quoted identifiers; XML parsing forbids DTDs.
- Release signing now comes from environment variables instead of reusing the debug key; archive-password dialog TODO (see backlog).

### Added
- **Universal opener core**: content-sniffing `FormatDetector` (magic bytes + ZIP refinement + heuristics + mismatch reporting).
- Viewers: SQLite browser, font specimen, 3D wireframe (STL/OBJ), SVG, EPUB, RTF/OpenDocument text, hex view labelled with the detected format.
- "Open with" works without all-files access; the permission screen gained "Continue without full access".
- Glass design tokens, `Modifier.glass()`, `GlassBackdrop`; glass cards and app bar.
- `TextWindowReader` (paged access to 500 MB+ files; UI paging still TODO).
- Gradle 8.7 wrapper (AGP 8.4.2 requires ≥ 8.6; the repo pinned 8.5), JaCoCo coverage gate (80 %), detekt (advisory), CI workflow.

### Changed
- Splash wait 500 ms → 150 ms. Opening a file no longer copies it to a temp cache first.
- Docs reorganised into `docs/`; stale pre-1.1.0 docs moved to `docs/legacy/`.

## [1.0.x]: see `docs/legacy/changelogs.md`
