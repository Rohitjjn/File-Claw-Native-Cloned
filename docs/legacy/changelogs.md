# File Claw - Changelog
Last Updated: June 15, 2026
Document Version: 1.0
Next Review Date: December 15, 2026

## 1. Changelog Format Rules (MANDATORY)
- **Every entry MUST include**: Version number, Date, Change Type, Affected Files/Modules, Description.
- **Change types**: `[ADDED]`, `[CHANGED]`, `[DEPRECATED]`, `[REMOVED]`, `[FIXED]`, `[SECURITY]`, `[PERFORMANCE]`, `[REFACTOR]`.
- **Breaking changes**: MUST be prefixed with `[BREAKING]` and include a migration guide.

## 2. Version Numbering System
- **MAJOR**: Architectual overhauls, massive UI structural changes, breaking data migrations.
- **MINOR**: New features (Audio player, PDF editing), significant enhancements.
- **PATCH**: Bug fixes, minor visual tweaks.
- Example: `v1.2.4` (Major 1, Minor 2, Patch 4).

## 3. Current Version Section
**Current Version:** v1.0.4
**Release Date:** June 28, 2026
**Total Changes:** Added file tree sidebar, auto-encoding support, smooth scrolling enhancements, fixed cache system, and resolved large PDF crash issues.

**Highlights:**
- Added a collapsible file tree navigation sidebar in HomeScreen drawer.
- Configured "Auto" encoding detection by default in Settings and FileManager.
- Integrated `Modifier.animateItemPlacement()` with `io.iamjosephmj.flinger` for 120 FPS ultra-smooth scrolling.
- Fixed `MainViewModel` cache logic to retain up to 5 files, skipping caching for files over 30MB to save disk space and loading time.
- Fixed large PDF crashing issue by removing Base64 conversion and loading files natively via `AndroidPdfViewer`.

**Issues In Progress:**
- See `known-issues.md` regarding PDF scale resetting on orientation change.

## 4. Version History

### [v1.0.4] - Sidebar, Auto-Encoding & Large PDF Fixes (2026-06-28)
**[ADDED]**
- **Affected:** `HomeScreen.kt`, `FileTreeComponent.kt`
- **Description:** Added a collapsible file tree navigation sidebar in the modal drawer.
- **Affected:** `FileManager.kt`, `SettingsScreen.kt`, `SettingEntity.kt`
- **Description:** Added an "Auto" encoding option, set as the default, which detects file encoding via BOM headers. Added extra encodings to the settings dropdown.

**[CHANGED]**
- **Affected:** `MainViewModel.kt`
- **Description:** Redesigned the temporary cache system to properly cache up to 5 files and skip caching for files larger than 30MB to prevent IO delays.
- **Affected:** `MainViewModel.kt`, `FilePreviewScreen.kt`
- **Description:** Refactored `PdfSuccess` to avoid reading the entire PDF into a Base64 string, preventing `OutOfMemoryError` on 100MB+ PDF files.

**[FIXED]**
- **Affected:** `AnimateItemMock.kt`
- **Description:** Removed mock to properly leverage Jetpack Compose's `animateItemPlacement` alongside Flinger library for 120fps smooth scrolling.
- **Affected:** `FileManager.kt`
- **Description:** Truncated text files to 5MB max length when rendering in the text previewer to prevent OOM errors on large text files.

### [v1.0.3] - Unified Document Viewers & Jam-less WebView Transitions (2026-06-18)
**[ADDED]**
- **Affected:** `/app/src/main/assets/mammoth/mammoth.browser.js`, `/app/src/main/assets/pdfjs/build/pdf.js`, `/app/src/main/assets/pdfjs/build/pdf.worker.js`
- **Description:** Added offline web assets to assets folders for client-side document processing directly in WebView.
- **Affected:** `FilePreviewScreen.kt`
- **Description:** Added `PdfPreviewWebView` and `DocxPreviewWebView` implementations.
**[CHANGED]**
- **Affected:** `FilePreviewScreen.kt`
- **Description:** Updated `MarkdownPreview` to support a continuous theme-matched loading progress overlay during page parsing and WebView instantiation.
- **Affected:** `MainViewModel.kt`
- **Description:** Updated `FileContentState.PdfSuccess` and `FileContentState.DocxSuccess` to carry pre-processed Base64 strings loaded in IO Dispatchers to support instantaneous document file clicks with zero main-thread blockage.

### [v1.0.2] - Markdown WebView Overhaul (2026-06-17)
**[ADDED]**
- **Affected:** `build.gradle.kts`
- **Description:** Added `org.jetbrains:markdown:0.7.3` dependency to parse markdown natively into HTML payload.
**[CHANGED]**
- **Affected:** `FilePreviewScreen.kt`
- **Description:** Shifted primary `MarkdownPreview` method from standard Compose text annotations to a full GPU-accelerated WebView implementation inheriting a Claude CSS aesthetic and JS bridges.

### [v1.0.1] - Startup Crash Hotfix & Optimizations (2026-06-16)
**[FIXED]**
- **Affected:** `AppDatabase.kt`, `RecentFileEntity.kt`
- **Description:** Fixed immediate application launch crash by bumping the Room database version to 3, allowing `fallbackToDestructiveMigration` to safely rebuild and incorporate newly defined indexing patterns without file integrity mismatches on local SQLite storage.
- **Affected:** `EditorScreen.kt`
- **Description:** Shifted undo/redo buffer from standard `mutableStateListOf` to a lean custom stack wrapper container over `ArrayDeque` with 50-item threshold capacity. Avoided continuous recompositions of buttons and text layouts during general keystrokes.
- **Affected:** `MainViewModel.kt`
- **Description:** Debounced user text searches inside local files directory by 150ms and introduced immediate in-flight query job cancellation to keep indices querying non-blocking.
- **Affected:** `HomeScreen.kt`, `SearchScreen.kt`
- **Description:** Integrated `derivedStateOf` mapping for list selections (`take(5)`) under primary lists to leverage optimized UI-thread composition speeds.

### [v1.0.0] - Initial Release (2026-06-15)
**[ADDED]**
- Core MVVM Architecture and Navigation Graph setup.
- `FileManager` service for IO operations.
- `AppDatabase` Room integration.
- PDF Viewing with dual-engine fallback structure.
- CSV parsing and tabular viewing.
- Basic Text Editing and Saving capabilities.

## 5. Change Entry Template
All upcoming pulls or changes MUST map to this template:

```markdown
### [vX.Y.Z] - YYYY-MM-DD
**[TYPE]**
- **Affected:** `Module/Class`
- **Description:** What changed and why.
- **Author:** Name/ID
- **[BREAKING]** (If applicable): Migration details.
```

## 6. Update Rules
- **When**: MUST be updated before any branch merge into `main` or `release`.
- **Who**: The developer executing the change.
- **Review**: Required during PR code review.

## 7. User-Facing Changelog
- Derive user notes by ignoring `[REFACTOR]` and technical `[FIXED]` entries.
- Focus on `[ADDED]` features and user-reported `[FIXED]` items.
- Example User Note: "Added support for viewing password-protected PDFs!"

## 8. Rollback Log
*No rollbacks recorded yet.*

---
**Update Log**
- 2026-06-15: v1.0 - Initial document creation.
- 2026-06-16: v1.1 - Added changelog listings for v1.0.1 hotfix and rendering optimizations.
