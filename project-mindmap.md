# File Claw - Project Mindmap
Last Updated: June 15, 2026
Document Version: 1.0
Next Review Date: December 15, 2026

* **File Claw App** (Central Node)
    * **UI Layer**
        * **Screens**
            * *Splash*: Permission gating, initial routing.
            * *Home*: Recent files carousel, quick action tiles, navigation drawer.
            * *All Files*: Hierarchical list, path breadcrumbs, sorting/filtering.
            * *Search*: Live query input, highlight matching files.
            * *Preview*: Dynamic rendering engine.
                * Sub-states: Textual, Graphical (Images), Paginated (PDF/DOCX), Tabular (CSV).
            * *Editor*: Text input field, save action, language syntax (planned).
            * *Settings*: Theme toggles, cache clearing.
        * **Shared Components**
            * `FileIcon`: Maps extensions to Vector drawables.
            * `ZoomableBox`: Custom gesture handler for Images/PDFs.
            * `AppBars`: Consistent top navigation slots.
        * **Theme System**
            * `Color.kt`: M3 dynamic colors, fallback dark/light arrays.
            * `Typography.kt`: Font scales.
    * **Data Layer**
        * **Database (Room)**
            * `RecentFileEntity`: Tracks timestamps and paths.
            * `SettingEntity`: Key-value persistence.
        * **File System**
            * *Internal Storage*: Sandbox, app internal states.
            * *External Storage*: User files, payload access.
            * *Cache Directives*: Temporary decrypted PDFs (`cacheDir/decrypted_pdf`).
        * **Search Index**
            * Real-time directory traversal caching.
    * **Domain Layer**
        * **ViewModels**
            * `MainViewModel`: Hub for all screen states globally.
        * **Business Logic**
            * Format Detection: Mime type and magic byte sniffing.
            * `FileContentState`: Sealed class hierarchy managing view loading.
                * States: Idle, Loading, TextSuccess, PdfSuccess, Error, etc.
        * **Event System**
            * `NavigationEvent`: Side effect channels mapped to Compose `LaunchedEffect`.
    * **Infrastructure**
        * **FileManager Service**
            * File crawling, read/write ops, encoding guessing.
        * **PDF Engine**
            * Primary: Native `android.graphics.pdf.PdfRenderer`.
            * Password Fallback: `PDFBoxResourceProvider` (Tom Roush).
        * **DOCX Engine**
            * Custom unzipping and XML traversal for paragraphs.
        * **Media Playback**
            * Android Native MediaPlayer wrappers.
    * **External Systems**
        * **Android OS Integration**
            * `MANAGE_EXTERNAL_STORAGE` permission intent overlay.
        * **Intents**
            * Action View fallbacks for unsupported encodings.
    * **User Experience**
        * **Navigation Patterns**
            * Standard BottomNavigationBar (planned) vs existing Deep Subdirectory stacks.
        * **Feedback Systems**
            * Snackbars on file save.
            * CircularProgress loading screens.
            * Empty states ("No recent files").
        * **Accessibility**
            * Standard Compose scaling features utilized.

## Structural Interdependencies
- `ui.screens.*` strictly depend on `MainViewModel`.
- `MainViewModel` tightly couples to `FileManager` (Infrastructure) and `AppRepository` (Data).
- `FileManager` dynamically relies on `PDFBox` or raw `java.io.File` logic depending on format.

## Decision Log
- **Decision**: Avoid SAF (Storage Access Framework) initially.
  - **Why**: Allows rapid, unrestricted offline file indexing across the entire device which SAF heavily rate-limits.
- **Decision**: PDFBox fallback.
  - **Why**: Native `PdfRenderer` fails violently on password-protected documents. PDFBox is bulky but essential for handling AES encrypted PDFs universally.
