# Current project and verification status

**Documentation/source review date:** 2026-10-23. **Working branch:** `arena/29f255df-file-claw-native-cloned`. **Git base at task start:** `ec4d91ca50d51749429970bf8caf631cc1439db0`.

This page is the current record of the active PDF/DOCX repair and documentation refresh. Source status and runtime verification are different: a success-state branch or a syntax check does not prove that Android WebView rendered a document.

## Current worktree changes

- A shared MIME-aware local asset handler now serves `.js` and `.mjs` files as `text/javascript` in PDF, DOCX, and DOCX print WebView asset-loader routes. This replaces the PDF handler's explicit `application/javascript` response and makes DOCX script MIME explicit instead of relying on platform extension mapping.
- Moved the existing `PdfRangeResponder` into `formats/pdf/` to keep PDF-specific byte serving with the PDF family. Its response logic was not changed.
- Added three local unit tests for JavaScript MIME selection and non-JavaScript pass-through. They have not been compiled or run.
- Replaced stale project notes with source-based English documentation, including a new root `PERFORMANCE.md` that describes source limits and a measurement plan without claiming benchmark results. `THIRD_PARTY_NOTICES.md`, bundled assets/licenses, Gradle/app identity configuration, resources, Room implementation, and module structure were not changed by this documentation/preview pass. Existing architecture-migration edits were already present in the working tree and remain in scope.
- The MIME change is a targeted WebView compatibility correction based on the local asset-loading path. The original preview failure has no captured runtime log here, so the root cause and user-visible resolution remain **unconfirmed** until an Android WebView check passes.
- Archive-source review found a documentation-worthy gap: `ArchiveLimits.maxTotalBytes` is declared but not enforced. See [`SECURITY.md`](SECURITY.md); no archive implementation was changed.

## Checks completed

| Check | Result |
|---|---|
| JavaScript syntax for inline PDF/DOCX scripts and four bundled JS/MJS files | **PASS**: `node --check` passed for all 7 scripts after the MIME source change. |
| Referenced preview assets | **PASS**: PDF.js main/worker MJS files and DOCX JSZip/renderer script files exist at the expected local paths. |
| Manifest and resource XML parse | **PASS**: 14 files parsed successfully. |
| Markdown links and obsolete-document references | **PASS** after adding `PERFORMANCE.md`: local links in 20 current Markdown files resolve; no references to removed legacy documents or prompts were found. |
| `git diff --check` | **PASS**. |
| `PERFORMANCE.md` policy arithmetic | **PASS as a source calculation only**: documented PDF RGBA figures match the per-page pixel tiers × 3-page target × 4 bytes/pixel. These are not runtime memory measurements. |
| Protected configuration/assets/notices diff review | **PASS**: no diff in app/Gradle identity configuration, manifest/resources, schemas, bundled assets, licenses, or `THIRD_PARTY_NOTICES.md`. |
| Production import/reference scan and PDF responder relocation | **PASS as a static source scan only** across 131 Kotlin production files; the PDF responder now has a PDF-family package and two expected call sites. Not a compiler check. |
| Format metadata/handler-ID comparison from preceding migration review | **PASS as a static source comparison only**: 69 metadata IDs and 69 handler IDs; no missing/extra/multiply-owned IDs. Not an executed test. The registry still has 69 format groups; no new group was added. |
| Test-source inventory | 36 Kotlin test files and 228 `@Test` annotations. This is an inventory count, not a passing test result. |
| `tools/GenFormatDocs.kt` | Updated to match the inventory format, but **not compiled or run**. The table was rebuilt by parsing `FormatRegistry.kt`; it has not been validated against generator output. |
| WebView runtime render | **NOT RUN**; no Android device/emulator or usable Android runtime was available. |
| Kotlin/Gradle compile, unit tests, APK assembly, lint/detekt, Room migration test | **NOT RUN successfully**; Java/JDK blocker below. |

## Exact Gradle results

Baseline attempts before the MIME source change:

```text
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:assembleDebug --no-daemon --max-workers=1
./gradlew lintDebug detekt --no-daemon --max-workers=1
```

Post-change attempts:

```text
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:assembleDebug --no-daemon --max-workers=1
./gradlew lintDebug detekt --no-daemon --max-workers=1
./gradlew :app:jacocoCoreVerify --no-daemon --max-workers=1
```

Each command exited with status 1 **before Gradle started**. The exact blocker was:

```text
ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
```

No compilation, unit tests, APK assembly, lint, detekt, coverage task, or Room migration test has a passing result. Rerun the gates on a machine with JDK 17 and Android SDK 34. A pre-existing `build.log` belongs to another environment and is not evidence for this worktree.

## Database note

`AppDatabase` source is version **4** and defines `MIGRATION_3_4`. The checked-in `app/schemas/` directory contains only schema `3.json`; the v4 export was absent during review. Verify Room/KSP schema generation and run `DatabaseMigrationTest` in the next successful Gradle run. No database version or migration was changed in this task.

## Remaining acceptance checks

- Build and run on an emulator/device, then open ordinary DOCX and PDF files from app storage, an external file intent, and a ZIP entry.
- Confirm the DOCX page, both local scripts, and `/docx-src/document.docx` load; formatted content appears rather than only extracted text; search, zoom, dark-mode behavior, retry/recovery, and existing print remain intact.
- Confirm PDF.js main module and worker load with the explicit JavaScript MIME type; open a real PDF; check page rendering, password/error states, range loading, search, rotation, zoom, and print/share.
- Record actual Android/WebView version, file size/source category, visible error, and relevant `WebViewDocx`/`PdfJsReader` logs without logging document contents.
- Run the post-change compile, unit tests, assemble, lint/detekt, and core coverage task on a configured machine. Record each separately.

See [`TODO.md`](TODO.md) and [`SANITY_CHECKLIST.md`](SANITY_CHECKLIST.md) for the next steps and broader manual regression list.
