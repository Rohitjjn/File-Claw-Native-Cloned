# Security and data-safety notes

These notes describe controls visible in the current source. They are not a penetration-test report. Runtime security testing, archive fuzzing, and hostile-document testing were not performed as part of this documentation and preview pass.

## Android permissions and storage

- The manifest declares read permissions for legacy external storage and media, `WRITE_EXTERNAL_STORAGE` only through API 29, and `MANAGE_EXTERNAL_STORAGE`. It does **not** declare `INTERNET`.
- `android:allowBackup` is `false`.
- External file/provider input is handled by the current intent and storage adapters. Do not assume a file path alone grants long-term provider access; keep URI handling and save behavior in the existing platform/storage and ViewModel paths.
- `FileProvider` uses the narrowed `app/src/main/res/xml/file_paths.xml` configuration. Preserve that configuration when changing export/share code.

## WebView boundaries

- PDF and DOCX preview pages, JavaScript libraries, and workers are packaged in app assets and loaded through `WebViewAssetLoader`; the PDF/DOCX preview WebViews disable raw file and content access.
- The PDF WebView serves one in-app PDF endpoint through `PdfRangeResponder`; its page and worker remain local.
- DOCX source bytes are served by a path-specific local handler. The JavaScript bridge is for preview callbacks and the existing base64 fallback; it is not a general file-write API.
- Local JavaScript files and modules are explicitly assigned `text/javascript` by `MimeAwareAssetsPathHandler`. Preserve the path whitelist and exact local routes.
- User-provided HTML scripts are off by default and require an explicit per-file action to run. Markdown/HTML generation uses the existing sanitizing/escaping paths.
- Keep external navigation blocked for locally generated or packaged previews. Do not enable universal file access as a workaround for an asset-loading defect.

## Bounded reads and archive safety

- Text previews use a 250,000-character format-handler limit; truncated content carries a separate flag and is not treated as safe-to-save full content.
- Archive listing applies a 200,000-entry cap and flags unsafe paths/deep nesting (configured depth 64). Extraction applies a 2 GiB per-entry byte cap. The 2,000:1 compression-ratio check for entries above 4 MiB is applied where ZIP metadata supplies compressed size; other extraction paths cannot apply that check.
- **Known source gap:** `ArchiveLimits.maxTotalBytes` is declared as 16 GiB but is not enforced. `ArchiveTree.totalSize` is only accumulated metadata. Do not describe or rely on a 16 GiB aggregate cap until a separate change implements and tests it. Archive listing does not apply the per-entry extraction cap either.
- DOCX/PPTX WebView documents have an existing 25 MiB threshold before the archive route is used. This is not permission to raise the limit without measuring memory and testing.
- PDF canvas pixel budgets, dimension caps, and resident-page limits are enforced by existing PDF-family policies. Do not remove them to mask a rendering issue.
- Writes and exports should use the existing atomic-write/export helpers and format-specific save capabilities. Preserve source-URI and migration behavior.

## Third-party code

Bundled PDF.js, DOCX/PPTX renderers, ZIP helpers, and other third-party code have notices/licenses in the repository. Preserve `THIRD_PARTY_NOTICES.md` and the license files stored with assets. Do not update, replace, or remove bundled assets without checking their exact license and notice requirements.
