# File Claw: Architecture (v1.1.0)

## Principle
Everything that can be decided without the Android framework lives in **`com.example.core`**: pure Kotlin,
no `android.*` imports, unit-tested on the plain JVM. The Android layers are thin adapters around it.

```
┌──────────────────────────────────────────────────────────────────────────┐
│ ui/            Compose screens + viewers (Home, AllFiles, Search, Settings,│
│                FilePreview, Editor, UniversalViewers, theme/Glass.kt)       │
├──────────────────────────────────────────────────────────────────────────┤
│ viewmodel/     MainViewModel: routing (FormatDetector -> FileContentState),│
│                saves, recents, search index, intent import                  │
├───────────────────────────────┬──────────────────────────────────────────┤
│ services/                     │ data/ + utils/                            │
│  FileManager (facade)         │  Room (recent_files, settings), repository │
│  SqliteBrowser                │  CacheManager, SmartCacheWorker            │
├───────────────────────────────┴──────────────────────────────────────────┤
│ core/   (pure Kotlin; historical 98% coverage, not rerun for Loop 6)       │
│  format/   FormatRegistry · FormatDetector · MagicSniffer · TextHeuristics │
│  io/       TextLoader · TextEncoding · TextSaver · TextWindowReader ·      │
│            AtomicFileWriter                                                │
│  archive/  ArchiveLimits · ArchivePaths · LimitedInputStream ·             │
│            ArchiveTreeBuilder                                              │
│  csv/      DelimitedParser · DelimitedWriter                               │
│  security/ PathGuard                                                       │
│  text/     RtfExtractor · OdfTextExtractor · EpubReader · HtmlSanitizer    │
│  model3d/  MeshParser (STL, OBJ)                                           │
│  sqlite/   SqlIdentifier                                                   │
└──────────────────────────────────────────────────────────────────────────┘
```

## Opening a file (the universal opener pipeline)
1. **Intake.** A file arrives from the picker, the tree, search, recents, a shortcut, or another app
   (`ACTION_VIEW`). External `content://` URIs are copied once into `filesDir/imported/` under a sanitised,
   non-colliding name (`PathGuard`). Handled in `onCreate` / `onNewIntent`, never in a Compose effect.
2. **Detect** (`FormatDetector.detect`). Reads at most 8 KB. Order: strong binary signature (always wins) →
   meaningful extension (checked against content) → weak text signature (only if the extension says
   nothing) → text/binary heuristics. ZIP containers are refined by entry names (docx/xlsx/pptx/odf/epub/apk).
   Result carries `FormatSpec`, `Confidence`, and `mismatch` (extension disagrees with content).
3. **Route** (`MainViewModel.buildContentState`): `FileKind` → a `FileContentState` the preview screen renders.
4. **Render.** Compose viewers, or sandboxed WebViews for PDF (bundled PDF.js via `WebViewAssetLoader`), DOCX / PPTX / spreadsheet grid, Markdown, and HTML. TXT/Markdown previews use bounded HTML windows; truncated TXT/Markdown search streams the original file and opens bounded excerpts at stored match offsets.
5. **Save** (editable states only): `AtomicFileWriter` (temp file + fsync + atomic rename); charset and BOM are
   preserved; truncated or converted views are read-only and the ViewModel refuses to save them.

## Data model
`FileContentState` (sealed) is the single contract between routing and rendering. Text and sheets carry
`truncated` / `readOnlyReason` **as flags, never as text inside the content**.

## Safety boundaries
| Boundary | Mechanism |
|---|---|
| Huge files | 8 KB sniff, 250k-char preview cap with `truncated` flag, `TextWindowReader` for bounded search excerpts, streamed full-file TXT/Markdown search with at most 10k retained offsets, 50k-row grid cap |
| Archives | entry cap, per-entry size and ratio limits, depth cap, `copyLimited`, zip-slip flagging, cache-chosen output names |
| Writes | atomic replace everywhere (text, delimited, ZIP entry update) |
| WebViews | no INTERNET permission; HTML scripts off by default and no bridge; Markdown sanitized with a narrow copy/search bridge; PDF.js and DOCX use trusted local bridges; DOCX cross-file/content access off; external top-level navigation blocked |
| Database | SQLite opened read-only, identifiers quoted, only generated SELECTs |
| Persistence | Room fails fast on schema change without a migration (no silent wipe); backup disabled |

## Known structural debt (see TODO.md)
`FilePreviewScreen.kt` is now roughly 4,100 lines after the Loop 6 readers; navigation is route-without-arguments backed by one shared
ViewModel state; imported copies don't write back to the source URI; DOCX/WebView renderers pass whole documents
to JS as base64 (size-capped, not streamed).
