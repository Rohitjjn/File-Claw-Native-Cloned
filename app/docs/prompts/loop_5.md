# 🦞 FILE CLAW — LOOP 5 PROMPT: DOCX FIX, GLASS PROPERTIES & PDF AUTO-RESUME

## 🎯 MISSION
Fix DOCX rendering, add glassmorphism modal bottom sheet for file properties, staggered animations, PDF auto-resume bookmarks, native PrintManager integration, and quick "Go to Line".

---

### CATEGORY 1: UI-RELATED CHANGES
1. **Glassmorphism Modal Bottom Sheet for File Properties**:
   - M3 `ModalBottomSheet` with drag handle, size, MIME type, permissions, path copy, and async MD5/SHA-256 hash calculation.
2. **Staggered Spring & Fade-in Reveal Animation**:
   - `StaggeredAnimatedItem` across grid and list cards.

---

### CATEGORY 2: CORE FEATURES
1. **DOCX Preview Fix & Pure-Kotlin Fallback Engine**:
   - Localized assets into `editor/`, fixed CORS, and bypassed IPC limits with `getDocxBase64()` interface.
   - Built pure-Kotlin offline OOXML extractor (`DocxExtractor.kt`) and test suite.
2. **PDF Last-Read Page Bookmark & Auto-Resume**:
   - Persisted active page in `SharedPreferences` and restored automatically with confirmation notification.
3. **Native Print & PDF Export via PrintManager**:
   - `FilePrintHelper.kt` utilizing Android's native `PrintManager` and `PrintDocumentAdapter`.
4. **Quick "Go to Line"**:
   - Direct line navigation dialog with smooth scroll and highlight flash.
