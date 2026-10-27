# Security and data-safety notes

These notes describe controls visible in the current source and the adversarial
tests actually run. Hostile OOXML/spreadsheet fixtures were exercised with an
independent Python validator, headless Chromium and a plain-JVM harness that
compiles the real `OoxmlPackageInspector.kt`. This is **not** a penetration
test, an Android `WebView` fuzz campaign, or an on-device memory test.

## Android permissions and storage

- The manifest declares read permissions for legacy external storage and media, `WRITE_EXTERNAL_STORAGE` only through API 29, and `MANAGE_EXTERNAL_STORAGE`. It does **not** declare `INTERNET`.
- `android:allowBackup` is `false`.
- External file/provider input is handled by the current intent and storage adapters. A file path alone does not grant long-term provider access; URI handling and save behavior remain in the existing platform/storage and ViewModel paths.
- `FileProvider` uses the narrowed `app/src/main/res/xml/file_paths.xml` configuration. Preserve that configuration when changing export/share code.

## WebView boundaries

- All Office preview pages, scripts and data are served from the fixed `https://appassets.androidplatform.net/` `WebViewAssetLoader` origin. `OfficeWebViewSecurity` whitelists only `/assets/editor/`, `/assets/spreadsheet/`, `/docx-src/`, `/pptx-src/` and `/spreadsheet-src/`; all other navigation/resource requests receive a 403 or are blocked.
- Office WebViews disable file/content access, file-scheme cross-origin access, DOM storage, form saving, geolocation, multiple windows, mixed content and network image/load access. JavaScript is enabled only for the bundled renderer pages.
- `MimeAwareAssetsPathHandler` assigns `text/javascript` to local `.js`/`.mjs` assets and rejects path traversal/encoded separators before opening any asset.
- Bridges expose only bounded viewer callbacks and one-cell edit/copy values. They do not expose file paths, arbitrary reads, arbitrary writes or macro execution.
- Generated spreadsheet HTML uses `textContent` and a JSON payload with manual escaping. The Chromium adversarial test injects `<img onerror=...>`, quotes, backslashes, control characters and Unicode; no script/image element is created and no page error occurs.
- PPTX/DOCX external relationships are counted and blocked from network/file access. Chromium verifies that an external relationship fixture produces zero external requests.
- User-provided HTML scripts remain off by default and require an explicit per-file action; Markdown/HTML generation uses the existing sanitizing/escaping paths.

## Untrusted OOXML package limits

`OoxmlPackageInspector` runs before docx-preview, pptx-preview or fastexcel. It
rejects:

- empty, OLE2/encrypted or over-96 MiB packages;
- more than 10,000 entries, duplicate names, symlink entries, encrypted ZIP entries, absolute/drive/encoded-traversal paths and path depth over 32;
- more than 512 MiB total expanded data, any single part over 128 MiB, and compression ratios above 2,000:1 for entries over 4 MiB;
- DOCTYPE/ENTITY declarations in any XML/relationship part (including UTF-16 variants), invalid content types, missing main parts and family/content mismatches;
- `file:`, `javascript:`, `data:` and `content:` external relationship targets.

`parseSecureXml` uses secure processing, disables DOCTYPE/external entities and
never expands entity references. Macro/template variants are read-only; macros
are never executed. The independent Python validator and the JVM harness each
cover 27 fixtures, including path traversal, XXE, ZIP bomb, truncation,
misleading extension and external relationships. The JVM harness uses a small
ZIP metadata shim; it validates the inspector's Kotlin logic, not Commons
Compress itself.

## Bounded reads, rendering and saving

- Office previews are bounded before rendering. The spreadsheet grid uses 20,000 rows, 100 columns, 4,000 characters/cell and 8,000,000 total characters. Any truncation disables editing and shows a notice.
- The shared grid materialises only visible cells. Chromium measured ~125 DOM cells for both a 31-row and a 5,001-row payload.
- DOCX uses `content-visibility` only for documents with 20+ rendered pages and verifies that the first page has visible dimensions before declaring success.
- PPTX slide text indexing is capped at 1,000 slides, 2 MiB of XML per slide and 5,000 search matches.
- Legacy OLE2 text recovery is capped by stream size, character count and paragraph count; PPT record traversal is capped at 200,000 records.
- Text previews use the existing 250,000-character format-handler limit; truncated content is flagged and cannot be saved as the whole original.
- Spreadsheet light-edit saves write values atomically through `AtomicFileWriter`; a rejected oversized edit leaves the original bytes unchanged. Multi-sheet, macro, template and truncated workbooks are never editable.
- PDF canvas pixel budgets, dimension caps and resident-page limits remain enforced.
- **Known archive gap:** `ArchiveLimits.maxTotalBytes` is declared as 16 GiB but is not enforced by the general archive extractor. `ArchiveTree.totalSize` is metadata only. The OOXML-specific inspector enforces its own aggregate cap, but do not claim a general 16 GiB archive cap until that separate change is implemented and tested.

## Third-party code

Bundled PDF.js, DOCX/PPTX renderers, JSZip, x-spreadsheet (legacy asset retained
but unused by the current viewer), and other third-party code have notices in
[`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md) and license files stored
with assets. Test-only Chromium/Puppeteer, Kotlin compiler/JDK and Python
fixture-generator dependencies are not packaged into the APK. Do not update or
remove bundled assets without checking their exact license and notice
requirements.
