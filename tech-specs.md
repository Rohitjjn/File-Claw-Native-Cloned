# File Claw - Technical Specifications
Last Updated: June 15, 2026
Document Version: 1.0
Next Review Date: December 15, 2026

## 1. Technology Stack Specification
- **Language**: Kotlin 1.9.22. Utilizing Coroutines for async tasks, `StateFlow` for state, and Sealed Classes extensively for type-safe state representations.
- **UI Framework**: Jetpack Compose 1.6.2 (BOM 2024.02.01). Heavy use of `LazyColumn`, `BoxWithConstraints` (for dynamic zooming), and `pointerInput` for gesture detection.
- **Database**: Room 2.6.1 utilizing KSP as the annotation processor.
- **Navigation**: Jetpack Navigation Compose (`androidx.navigation:navigation-compose:2.7.7`). Type-safe string routing. No complex deep links configured currently.

## 2. Architecture Pattern Details
- **MVVM Implementation**: `MainViewModel` extends `AndroidViewModel` to access application context for content resolvers and cache directories. It survives configuration changes seamlessly.
- **State Management**: `MainViewModel` exposes properties like `val fileContentState: StateFlow<FileContentState>`. UI components strictly observe this via `collectAsStateWithLifecycle()`.
- **Repository Strategy**: `AppRepository` abstracts `AppDatabase` queries. `FileManager` is currently injected directly or instantiated via ViewModel rather than a strict global singleton pattern.
- **Dependency Injection**: Currently manual constructor injection. Extracted manual providers are preferred to avoid the setup overhead of Hilt.

## 3. Threading and Concurrency Model
- **UI Dispatcher (`Dispatchers.Main`)**: Strictly handles Compose recomposition.
- **IO Dispatcher (`Dispatchers.IO`)**: Mandatory context for `java.io.File` reads, Room DAO invocations, and PDF renderer initializations. Wrapping logic in `withContext(Dispatchers.IO)` is heavily enforced in `FileManager`.
- **Default Dispatcher (`Dispatchers.Default`)**: Leveraged specifically when mapping/transforming large lists of files during Search indexing or converting byte arrays to Hex strings.
- **Cancellation**: Lifecycle-aware `viewModelScope` automatically cancels trailing I/O tasks if the ViewModel clears.

## 4. Memory Management Specification
- **Bitmaps**: The `PdfBrowser` explicitly configures `Bitmap.Config.ARGB_8888`. Bitmaps are discarded immediately upon scroll-out utilizing Compose `LaunchedEffect(pageIndex, isVisible)`.
- **Large Text Files**: Full byte-array loading is standard. [GAP]: True streaming for files strictly over 10MB is currently insufficient, causing latency spikes.
- **Cache Strategy**: Password-protected PDFs are decrypted via `PDFBoxResourceProvider` and temporarily saved to `context.cacheDir`. The system is relied upon to prune this directory, though explicit cleanup on app exit is planned.

## 5. File I/O Specifications
- **Format Sniffing**: Determining file type relies fundamentally on suffix analysis (e.g., `.endsWith(".pdf")`). Secondary structural sniffing is avoided for speed.
- **Text Encoding**: UTF-8 is assumed universally. Reading uses `file.readText()`.
- **DOCX Extraction**: The app manually unwraps the `.docx` (which is a ZIP archive), locating `word/document.xml`, and parsing basic `<w:t>` tags via `XmlPullParser` instead of importing massive Apache POI libraries.

## 6. Database Schema
**Current Version: 1**
- Entity: `RecentFileEntity`
  - `id` (Int, Primary Key, Auto-increment)
  - `name` (String)
  - `path` (String, Unique index mapped)
  - `lastAccessed` (Long, Unix timestamp)
  - `isDirectory` (Boolean)
- Entity: `SettingEntity`
  - `key` (String, Primary Key)
  - `value` (String)

*Indices*: The `path` column in `RecentFileEntity` is indexed for rapid `INSERT OR REPLACE` conflict resolution.

## 7. API and Interface Contracts
**MainViewModel Public API**
```kotlin
val currentFiles: StateFlow<List<File>>
val recentFiles: StateFlow<List<RecentFileEntity>>
val fileContentState: StateFlow<FileContentState>

fun switchDirectory(path: String)
fun openFile(fileEntity: RecentFileEntity)
fun search(query: String)
fun saveFile(path: String, newContent: String)
```

**FileContentState (Sealed Class)**
```kotlin
sealed class FileContentState {
    data object Idle : FileContentState()
    data object Loading : FileContentState()
    data class TextSuccess(val content: String, val file: RecentFileEntity) : FileContentState()
    data class PdfSuccess(val file: RecentFileEntity) : FileContentState()
    data class Error(val message: String) : FileContentState()
    // ... CsvSuccess, DocxSuccess, MediaSuccess, etc.
}
```

## 8. Build and Deployment Specs
- **ProGuard**: Required specific rules to prevent obfuscating XML processing elements used in DOCX parsing.
- **Release Strategy**: App bundles (AAB) are expected to be signed with a production keystore configured via the CI pipeline properties.

## 9. Platform-Specific Adaptations
- **API 30+ (Android 11)**: Due to Scoped Storage, `MANAGE_EXTERNAL_STORAGE` via `ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION` is aggressively requested as this app functions explicitly as a broad raw file manager. Standard storage requests fallback for API < 30.
- **PDF Viewing**: Built on native `android.graphics.pdf.PdfRenderer` initialized with `MODE_READ_ONLY`. Advanced zooming relies on custom `pointerInput` tracking.

## 10. Accessibility & Internationalization
- **i18n Status**: Hardcoded English strings currently predominantly used. Externalizing to `strings.xml` is actively tracked as tech debt.
- **Accessibility**: Minimal standard implementations. Interactive lists leverage standard semantics implicitly via Compose elements. Content Descriptions are applied to core interaction buttons.
