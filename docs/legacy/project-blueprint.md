# File Claw - Project Blueprint
Last Updated: June 15, 2026
Document Version: 1.0
Next Review Date: December 15, 2026

## 1. Project Overview Section
- **App name**: File Claw
- **Platform**: Android native (Jetpack Compose)
- **Target SDK**: 36 (Android 14+)
- **Minimum SDK**: 24 (Android 7.0)
- **Architecture**: MVVM with Repository pattern
- **Primary purpose**: Local file manager, viewer, and editor with offline-first approach, specializing in processing various file formats smoothly without requiring external applications.

## 2. Core Features Inventory
| Feature Name | Status | Priority | Owning Module/Screen |
|--------------|--------|----------|----------------------|
| File Browser | Stable | P0 | HomeScreen / AllFilesScreen |
| Recent Files | Stable | P0 | HomeScreen |
| Search/Index | Stable | P1 | SearchScreen |
| File Preview (General) | Stable | P0 | FilePreviewScreen |
| Text Editor | Stable | P1 | EditorScreen |
| Settings | Stable | P2 | SettingsScreen |
| Theme System | Stable | P2 | Theme / SettingsScreen |
| Import/Export | Beta | P2 | SettingsScreen / FilePreviewScreen |
| ZIP Navigation | Stable | P1 | FilePreviewScreen (FileManager) |
| DOCX Preview | Stable | P1 | FilePreviewScreen (FileManager) |
| PDF Preview | Stable | P0 | FilePreviewScreen (PDFRenderer / PDFBox) |
| CSV Preview | Stable | P1 | FilePreviewScreen (FileManager) |
| Image Viewer | Stable | P1 | FilePreviewScreen (Coil) |
| Audio Player | Beta | P2 | FilePreviewScreen |
| Video Player | Beta | P2 | FilePreviewScreen |
| Hex Viewer | Beta | P3 | FilePreviewScreen |
| Password PDF Decrypt | Stable | P1 | FilePreviewScreen (PDFBox) |

## 3. Screen Navigation Map
The application utilizes Jetpack Navigation Compose defining a single `NavHost`.

- **Home (`home`)**
  - Parent: None
  - Flow: Primary entry point. Transitions to All Files, Search, Settings, Preview.
  - Deep Link: None.
- **All Files (`allFiles/{path}`)**
  - Arguments: `path` (String, default: root directory)
  - Parent: Home
  - Flow: Navigates deeper into directories, transitions to Preview.
- **Search (`search`)**
  - Parent: Home
  - Flow: Allows querying indexed files. Transitions to Preview.
- **File Preview (`preview/{id}`)**
  - Arguments: `id` (Int, default: -1, queries RecentFileEntity)
  - Parent: Home / All Files / Search
  - Flow: Inspects file content. Can launch `editor/{id}`.
- **Editor (`editor/{id}`)**
  - Arguments: `id` (Int, default: -1)
  - Parent: Preview
  - Flow: Edits plain text/code files and saves them back to disk.
- **Settings (`settings`)**
  - Parent: Home
  - Flow: Displays user preferences, theme options, and app information.
- **Splash (`splash`)**
  - Parent: None
  - Flow: Initial launch screen, checks permissions, transitions to Home.

*Back Stack Behavior*: Standard push/pop. Popping from deep subdirectories in `allFiles` goes to the parent directory until the root is reached, which then pops back to Home.

## 4. Data Flow Architecture
The application adheres strictly to Unidirectional Data Flow (UDF).

1. **UI Layer**: Composable screens observe state from ViewModels using `StateFlow`. User actions trigger events sent to the ViewModel.
2. **ViewModel Layer**: `MainViewModel` scopes execution within `viewModelScope` and manages `MutableStateFlow`. Operations are deferred to the Repository.
3. **Repository Layer**: `AppRepository` abstracts data sources. It coordinates with `AppDatabase` (Room) for tracking recent files/settings, and `FileManager` for disk I/O.
4. **Threading**:
   - `Main`: UI updates, animation rendering.
   - `Dispatchers.IO`: File I/O, PDF rendering, DOCX extraction, database queries.
   - `Dispatchers.Default`: Heavy parsing (e.g., CSV manipulation, Hex dump generation, Search indexing).
5. **Error Propagation**: Exceptions in the data layer are caught, transformed into user-friendly `FileContentState.Error` or `NavigationEvent.ShowError` classes, and bubbled up to the UI.

## 5. External Dependencies Matrix
| Library | Version | Purpose | Features | Replacement Candidate |
|---------|---------|---------|----------|-----------------------|
| Jetpack Compose | 1.6.2/Bom 2024.02.01 | UI Toolkit | All screens | None |
| Coil Compose | 2.5.0 | Image Loading | Image Viewer | Glide Compose |
| Room DB | 2.6.1 | Persistence | Recent Files, Settings | SQLDelight |
| PDFBox-Android | 2.0.27.0 | PDF Parsing/Decrypt | PDF Viewer | PSPDFKit/MuPDF |
| Gson | 2.10.1 | Serialization | Configs | Kotlinx Serialization |

## 6. Build Configuration Summary
- **AGP**: Standard Android application plugin.
- **Kotlin**: 1.9.22.
- **KSP**: Used over KAPT for Room compilation, reducing build times.
- **Minification (ProGuard/R8)**: Shrinking enabled for release builds. Custom rules explicitly keep Apache POI structures or `org.apache.xmlbeans.**` if used historically, to prevent DOCX parser crashes.
- **Signing**: Debug uses default keystore, Release keys configured via system environment.

## 7. Performance Baselines
- **Target cold start time**: < 1.0s to Splash, < 2.0s to Home.
- **File open times**:
  - Text (<1MB): < 300ms
  - Large Text (up to 10MB): < 1.5s (chunked streaming)
  - PDF: < 500ms initial render per page.
  - ZIP: < 800ms to index headers.
- **Memory budget**: 150MB overhead per screen (specifically tracking image/PDF bitmaps).
- **Max File Limit**: Fallback to Hex/Chunked reader for files > 50MB to prevent `OutOfMemoryError`.

## 8. Security Considerations
- **Storage Access**: Uses standard `java.io.File` with `MANAGE_EXTERNAL_STORAGE` permission (target 30+). Fallback to standard reads for older devices.
- **Permissions**: Prompts explicitly on the Splash screen before allowing progression.
- **Data Encryption**: No at-rest encryption currently implemented for local files.
- **PDF Decryption**: Handled securely in memory using PDFBox; decrypted PDFs are written to `cacheDir` temporarily and cleared on session exit.

## 9. Future Roadmap (6-month horizon)
1. **[P1] SAF Migration**: Transition from raw `File` paths to `DocumentFile` and Storage Access Framework URIs for strict Android 14+ scoping compliance.
2. **[P2] Rich Text Editor**: Expand EditorScreen to support Markdown rendering and basic WYSIWYG editing.
3. **[P3] Cloud Sync**: OneDrive/Google Drive plugin support.
4. **[TODO] Architecture Tech Debt**: Split monolithic `MainViewModel` into feature-specific ViewModels (e.g., `PreviewViewModel`, `SettingsViewModel`).

## 10. File Structure Reference
```
com.example
├── data/              # Room entities, DAOs, Database Configuration
├── services/          # FileManager, background I/O engines, Parsers
├── ui/                # UI Components
│   ├── components/    # Reusable Compose widgets (TopBars, Custom buttons)
│   ├── screens/       # Full-screen route destinations
│   ├── theme/         # Color, Typography, Material Theme config
├── viewmodel/         # Stores business logic coordinators
└── MainActivity.kt    # Entry point & NavHost holder
```
