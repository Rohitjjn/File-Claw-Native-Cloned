# 🦞 FILE CLAW — LOOP 1 PROMPT & ARCHITECTURE

## 🎯 MISSION
Initialize File Claw autonomous self-improvement loop.
Set up pure-Kotlin core layer, format detection registry, crash-safe IO, and initial viewer capabilities.

---

### CATEGORY 1: UI & ARCHITECTURE
1. **Material Design 3 & Claude Theme Foundation**:
   - Establish restrained glassmorphism aesthetic (light/dark palette, typography, glass modifier).
   - Core screens: Home, File List, Search, Settings, File Preview, Editor.
2. **Format Detector & Registry**:
   - Implement `FormatDetector` and `FormatRegistry` supporting 40+ MIME types and extensions.
   - Pure-Kotlin signature sniffing (magic bytes) for PDF, ZIP, Office OOXML, images, audio, database.

---

### CATEGORY 2: CORE SAFETY & OPERATIONS
1. **Crash-Safe Atomic IO**:
   - Create `AtomicFileWriter` using temporary staging files and atomic renames (`StandardCopyOption.ATOMIC_MOVE`).
   - `PathGuard` path traversal protection.
2. **Archive Safety**:
   - Zip-slip and archive-bomb protection (`ArchiveSafety.kt`).
3. **Database & Persistence**:
   - Room database (`AppDatabase`, `RecentFileDao`, `SettingDao`) for recent files and settings.
