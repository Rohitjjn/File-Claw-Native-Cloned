# Security audit: v1.1.0

Scope: code review of the full v1.0 source plus the v1.1.0 changes. No dynamic testing, fuzzing or device pen-testing was performed.

| # | Finding (v1.0) | Severity | v1.1.0 status |
|---|---|---|---|
| 1 | Zip bombs: 7z extraction `ByteArray(entry.size.toInt())`, no ratio/size/entry limits | High | **Fixed**: `ArchiveLimits`, streamed `copyLimited`, entry cap |
| 2 | Zip-slip names displayed/handled unchecked | Medium | **Fixed**: normalised, flagged `unsafe`, never extractable; output paths are cache-chosen |
| 3 | `FileProvider` `<root-path path=".">` exposed the whole filesystem | High | **Fixed**: removed (external, cache, files only) |
| 4 | Local HTML ran JS with file access, plus a JS bridge | High | **Fixed**: scripts off by default, no bridge, nav locked, URL via `Uri.fromFile` |
| 5 | Markdown raw HTML + JS-enabled WebView with a clipboard bridge | Medium | **Fixed**: `HtmlSanitizer` |
| 6 | All WebViews had `allowFileAccess`/`allowContentAccess = true` | Medium | **Fixed** (assets unaffected) |
| 7 | `allowBackup="true"` backed imported documents up to cloud | Medium | **Fixed** |
| 8 | VIEW filter `BROWSABLE` (web-triggerable) | Low | **Fixed** |
| 9 | Import used the raw display name (overwrite / odd names) | Medium | **Fixed**: `PathGuard.sanitizeFileName` + unique names |
| 10 | Whole-file reads (`readBytes`) of untrusted files, OOM | High | **Fixed** for text/charset; DOCX/PPTX capped at 25 MB |
| 11 | Release build signed with the debug key | Medium | **Mitigated**: env-based release signing; falls back to debug key |
| 12 | Untrusted SQLite would be opened by the new viewer | n/a (new) | Opened read-only, quoted identifiers, SELECT-only; residual risk: SQLite parser bugs on hostile files |
| 13 | XML parsing of ODF/EPUB (XXE) | n/a (new) | DTDs/external entities disabled |
| 14 | No `INTERNET` permission | n/a | **Confirmed**: nothing can be exfiltrated even if script injection occurred |
| 15 | Archive password field not masked | Low | **Open** (TODO P0) |
| 16 | Unscoped JitPack repository | Low | **Open** (TODO P2) |
| 17 | `MANAGE_EXTERNAL_STORAGE` still requested (Play policy risk) | Policy | **Open**: now optional for opening files, still needed for browsing/search |
| 18 | Bundled JS libraries unpinned/unlicensed notice | Low | Notices added; license verification open |

Residual risk notes: WebView renderers parse hostile DOCX/PPTX in JS; the mitigation is no network and no bridge capabilities beyond view callbacks.

## Loop 6 review notes (static review only; no device test)
- PDF.js 4.10.38 and its worker/CMaps/fonts are bundled locally. The PDF viewer serves only `/assets/` from app assets and the requested `document.pdf` through an exact path check; PDF WebView file/content access remains disabled, and top-level navigation is limited to those local reader/source paths. The trusted local reader uses a JavaScript bridge for page/search/password/tap callbacks. No remote assets or `INTERNET` permission were added.
- TXT preview HTML escapes source text; Markdown HTML is sanitized. Their local JavaScript bridge is limited to code-copy and reader callbacks. External navigation from those generated previews is blocked.
- DOCX keeps file access enabled only to load `android_asset/editor/` resources; content access, cross-file access, and universal file access are disabled. Top-level navigation outside the editor asset path is blocked. The bridge exposes DOCX bytes plus view/search callbacks; it does not expose file writes.
- Large TXT/Markdown search streams the original file and retains bounded hit offsets; it does not load the complete source into the WebView. Print/copy use a bounded character load with an error on overflow. This does not remove parser/renderer risks for hostile document contents.
- Java/Kotlin compilation and dynamic Android/WebView security checks were not run in this sandbox. Confirm `INTERNET` permission, asset interception, navigation blocking, and memory bounds on a built app before release.
