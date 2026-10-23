# Work remaining

This is the short active checklist. For exact prior commands/results, see [`STATUS.md`](STATUS.md). The active task does not include adding formats or preview capabilities.

## Required verification gates

- [ ] On a machine with JDK 17 and Android SDK 34, run debug Kotlin compilation, all local unit tests, APK assembly, `lintDebug`, `detekt`, and `:app:jacocoCoreVerify`. Record each command and its actual result.
- [ ] Run the Room `DatabaseMigrationTest`; inspect KSP output and decide whether the version-4 Room schema JSON should be checked in. Current source declares DB version 4, but only `app/schemas/.../3.json` is committed.
- [ ] Install the built APK on an emulator/device and exercise existing PDF and DOCX previews. The PDF/DOCX MIME change is source-level only until this is done.

## PDF preview acceptance

- [ ] Open a regular local PDF and verify the first page renders; inspect local PDF.js main-module and worker requests and confirm both `.mjs` assets use `text/javascript`.
- [ ] Open a PDF from an external file intent and an archive entry; exercise a password-protected file if available.
- [ ] Check search and next/previous navigation, page jumps/bookmark, rotate, zoom, page scrubber, print/share, and existing error/retry states.
- [ ] Check a large or mixed-orientation file without removing the existing range, resident-page, pixel-budget, or dimension limits.

## DOCX preview acceptance

- [ ] Open ordinary DOCX files from app storage, an external file intent, and a ZIP entry. Confirm the editor page and both bundled scripts load, `/docx-src/document.docx` returns bytes, and formatted content renders.
- [ ] Check search/count navigation, page tracking, toolbar/slider/pinch zoom, the existing dark-mode behavior, print, and retry/recovery.
- [ ] Confirm a renderer or malformed-file failure remains an honest recovery surface; do not present extracted text as a successful formatted rendering.
- [ ] Inspect actual `WebViewDocx` logs and visible recovery reason if the preview still fails.

## Separate source-inspection follow-up

- [ ] Decide whether `ArchiveLimits.maxTotalBytes` is meant to be an enforced archive limit. It is currently declared but unused; do not rely on or describe a 16 GiB aggregate cap until it is implemented and tested. See [`SECURITY.md`](SECURITY.md). This is separate from the PDF/DOCX preview repair.

## Scope boundary

Preserve existing behavior, limits, data migrations, assets, and licenses. Do not add new PDF, DOCX, XLSX, PPTX, or other preview support in this task. Do not redesign unrelated UI or change app identity/build structure.
