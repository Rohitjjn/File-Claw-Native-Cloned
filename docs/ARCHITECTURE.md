# Architecture and source map

File Claw is a single-module Android application. The current module is `:app`; production source stays under `app/src/main/java/com/example/`. This page is a map to the implementation, not a proposed redesign.

## Main request flow

1. `MainActivity` is the Android entry point. It receives app launches and supported file intents, then hosts the Compose UI.
2. `platform/intent/IncomingIntentRouter.kt` and the storage/permission helpers prepare file input for the app.
3. `viewmodel/MainViewModel.kt` coordinates recent-file persistence and open operations. `core/format/FormatDetector.kt` identifies the content and selects a `FormatSpec` from the existing `core/format/FormatRegistry.kt` metadata.
4. `MainViewModel` sends a `FormatOpenRequest` through the extension functions in `formats/registry/FormatRegistryOperations.kt`. The core registry owns the single handler-ID route table; default providers are assembled by `formats/registry/DefaultFormatHandlers.kt`.
5. A family handler returns an existing `domain/preview/FileContentState`. `feature/preview/FilePreviewScreen.kt` selects the matching existing family viewer.

Format support is not inferred from an extension list alone. Detection can use file signatures and text heuristics, and `FormatSpec.support` describes current metadata status. See [`FILE_FORMATS.md`](FILE_FORMATS.md).

## Package responsibilities

| Package | Responsibility | Examples |
|---|---|---|
| `core/archive/` | Archive path validation, extraction quotas, safe tree construction | `ArchivePaths`, `ArchiveLimits`, `ArchiveTreeBuilder` |
| `core/format/` | Format IDs/metadata, content detection, the single route table | `FormatRegistry`, `FormatDetector` |
| `core/io/` | Bounded text reads, encoding, atomic writes/exports, range parsing | `TextLoader`, `TextSaver`, `HttpRange`, `AtomicFileWriter` |
| `core/search/` | Shared file-name ranking | `FileSearchRanker` |
| `core/security/` | Path guards and HTML escaping/sanitizing | `PathGuard`, `HtmlSanitizer` |
| `data/` | Room entities, DAO interfaces, database, repository | `AppDatabase`, `RecentFileEntity`, `AppRepository` |
| `domain/preview/` | Preview state and typed handler requests, capabilities, results | `FileContentState`, `FormatHandler` |
| `feature/` | App screens and user flows; shared preview composition | `feature/home/`, `feature/preview/`, `feature/navigation/` |
| `formats/<family>/` | Family-owned parsing, policy, handlers, loaders, and viewers | `formats/pdf/`, `formats/docx/`, `formats/csv/` |
| `formats/registry/` | The default handler provider and typed extensions on the existing registry | `DefaultFormatHandlers` |
| `platform/` | Shared Android services and adapters | `storage/`, `intent/`, `print/`, `share/`, `webview/` |
| `ui/component/`, `ui/theme/` | Shared Compose components, tokens, and theme | `ClaudeComponents.kt`, `Color.kt`, `Glass.kt`, `Theme.kt` |
| `viewmodel/` | App-level coordination and UI state | `MainViewModel` |

Format-specific parsers and rendering decisions belong in a format-family package. The screen layer may connect shared UI controls to a family viewer, but it should not acquire format-specific parsing or processing.

## Important ownership details

- `core/format/FormatRegistry.kt` stores metadata and the single runtime route map through the small `FormatRouteHandler` contract. `domain/preview/FormatHandler` extends that contract with typed operations. There is no second application-level route registry.
- `MainViewModel` remains the broad app coordinator. It owns app-wide recent-file, search, edit, and navigation coordination; it should not absorb a family parser or WebView renderer.
- `feature/preview/FilePreviewScreen.kt` remains the cross-family composition and shared viewer-chrome surface. Individual viewer implementations live under `formats/<family>/ui/` where extracted.
- Shared storage and archive operations are in `platform/storage/FileManager.kt`; archive limits and path safety are in `core/archive/`.
- Printing, sharing, clipboard, permissions, incoming intents, lifecycle policy, and local WebView assets have shared adapters under `platform/`.
- Room state is isolated under `data/`; app preferences and cache behavior remain with their existing owners. See [`DATABASE.md`](DATABASE.md).
- Offline HTML/JavaScript assets are under `app/src/main/assets/`. Preserve them and their license notices.

## Format-family package map

Current family areas include `archive`, `binary`, `common`, `csv`, `doc`, `docx`, `epub`, `font`, `html`, `image`, `legacyoffice`, `markdown`, `media`, `model3d`, `odf`, `ooxml`, `pdf`, `pptx`, `rtf`, `sqlite`, `svg`, `text`, `xls`, and `xlsx`. A package exists only where the source has an actual responsibility; do not create empty families merely to match a diagram.

## Tests and assets

- Local JVM tests mirror `core`, `data`, `feature`, `formats`, and `platform` responsibilities under `app/src/test/java/`.
- No Android instrumentation-test source tree is present at the time of this documentation pass.
- WebView pages and bundled libraries live in `app/src/main/assets/editor/` and `app/src/main/assets/pdfjs/`.
- Third-party license/attribution records are retained in `THIRD_PARTY_NOTICES.md` and alongside bundled assets. Do not remove or regenerate legal notices as ordinary documentation.

For the existing PDF and DOCX call chains, assets, and remaining runtime checks, see [`PREVIEW_PIPELINES.md`](PREVIEW_PIPELINES.md).
