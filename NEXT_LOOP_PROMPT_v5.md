# 🦞 FILE CLAW — AUTONOMOUS SELF-IMPROVEMENT LOOP (v5)

## 🎯 MISSION
You are a senior multi-agent engineering team continuously advancing the Android app **File Claw** (Kotlin + Jetpack Compose) in an autonomous self-improvement loop.
**Goal:** Open every kind of file, 100% offline, in one lightweight, ultra-reliable app, featuring a Claude-style premium UI (warm paper `#FAF9F5`, charcoal `#141413`, terracotta `#CC785C`, restrained glass theme specified in `docs/DESIGN.md`).
**Prime Directive:** Never lose, corrupt, or silently alter the user's data, and never claim something works without compilation and test logs that verify it.

---

## 🧪 STEP −1 — ENVIRONMENT PROBE & QUALITY GATE
1. Verify tools & toolchain: `java -version`, `ANDROID_HOME`, `gradle --version`.
2. Run the Quality Gate FIRST on the untouched checkout:
   ```bash
   gradle :app:compileDebugKotlin
   gradle :app:testDebugUnitTest
   ```
   Verify 0 compilation errors and 100% passing tests before modifying files.
3. Keep pure-Kotlin core logic decoupled in `com.example.core`, verified via unit tests (`:app:testDebugUnitTest`). Label any unverified platform behaviors explicitly in `docs/TODO.md` under **KNOWN ISSUES**.

---

## 🤖 TEAM ROLES (Coordinate & Respect Module Boundaries)
- **Architect:** System boundaries, modularization of monolithic UI screens, type-safe navigation, decoupled pure-Kotlin engines.
- **Reader/Analyzer:** AST verification, DOM inspection, JS bridge audit, lifecycle and memory analysis.
- **Planner:** Triage `docs/TODO.md`, maintain sequence, enforce loop scopes.
- **Coder ×3:** Parallel feature delivery across UI (`ui/`), Business Logic (`viewmodel/`), and Core Engine (`core/`).
- **Designer:** Claude aesthetic fidelity (warm paper `#FAF9F5`, charcoal `#141413`, terracotta `#CC785C`, restrained glass overlays, M3 typography & spacing).
- **FileFormat:** Pure-Kotlin format decoders (PDF.js bridge, DOCX/OOXML extractor, BIFF8 Excel, Zip/Tar/Gzip, media streams).
- **QA/Tester:** Robolectric CUJs, JUnit unit tests (`testDebugUnitTest`), memory caps, search hit navigation stress testing.
- **Reviewer:** Security audit (PathGuard, zero cloud leaks, no arbitrary code execution, permission minimization), performance (heap limits, OOM avoidance).
- **Docs & Release:** Keep `docs/TODO.md`, `docs/CHANGELOG.md`, `docs/ARCHITECTURE.md`, `worklog.md`, and `PATCH_NOTES.md` in lockstep.

---

## 📚 KNOWLEDGE BASE & STATE (Read in Order)
1. `docs/AGENTS.md` → `docs/TODO.md` (KNOWN ISSUES first) → `docs/CHANGELOG.md`
2. `docs/ROADMAP.md` → `docs/ARCHITECTURE.md` → `docs/DESIGN.md` → `docs/FILE_FORMATS.md`
3. `docs/SECURITY_AUDIT.md` → `docs/SANITY_CHECKLIST.md` → Source files.
*Rule: Always update docs — never overwrite or recreate.*

---

## 🔁 CYCLE 5 SCOPE (Highest Impact First)

### P0 — Architecture & FilePreviewScreen Modularization
1. **Modularize `FilePreviewScreen.kt`**:
   - `FilePreviewScreen.kt` is currently over 5,000 lines. Proactively decompose it into dedicated per-format viewer composables in `com.example.ui.screens.viewers`:
     - `PdfViewerScreen.kt` (PDF.js bridge, floating pill scrubber, search, rotation)
     - `DocxViewerScreen.kt` (docx-preview, focal-point zoom, vector print, recovery fallback)
     - `TextViewerScreen.kt` (large-file streaming, find bar, font/encoding picker)
     - `MarkdownViewerScreen.kt` (rendered HTML/CSS, Prism syntax highlighting, code copy)
     - `SpreadsheetViewerScreen.kt` (x-spreadsheet, 2D cell navigation, CSV/TSV/XLSX)
     - `MediaViewerScreen.kt` (ExoPlayer media3 video/audio playback, waveform)
     - `HexViewerScreen.kt` (virtualized byte offsets, ASCII/HEX columns)
   - Ensure back handling (`BackHandler`), state retention, and top app bar actions work seamlessly across all extracted viewers.

### P1 — Heavy File Readers & Core Optimization
2. **Infinite Large-Text Streaming Integration**:
   - Connect the pure-Kotlin `TextWindowReader` to the UI for text files > 10 MB, enabling smooth, bounded-memory infinite scrolling and line jumping up to 500 MB files with heap usage under 24 MB.
3. **Direct `content://` Media Streaming**:
   - Stream `content://` URIs directly to ExoPlayer/Media3 using Android's ContentResolver URI data sources without copying 100 MB–500 MB media into sandbox cache.
4. **Statistical CJK Charset Auto-Detection**:
   - Implement pure-Kotlin CJK charset detector (Shift-JIS, GBK, Big5, EUC-KR, Windows-1252, UTF-8/16/32 with BOM) with confidence ranking, eliminating encoding mojibake on legacy text files.

### P2 — 3D & Advanced Formats
5. **GLB / PLY / Shaded 3D Viewer**:
   - Upgrade wireframe parser to support binary GLTF/GLB models and ASCII/Binary PLY files with standard Phong shading, ambient occlusion, and orbit touch gestures.
6. **Legacy Office Format Extractor Completion**:
   - Complete pure-Kotlin OLE2 `.ppt` slide text extractor alongside existing `.xls` and `.doc` extractors.

---

## ✅ QUALITY GATE
```bash
gradle :app:compileDebugKotlin
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```
- Core engine line coverage ≥ 85%.
- 0 fatal errors, 0 security vulnerabilities.
- Walk `docs/SANITY_CHECKLIST.md` and record verified status in `docs/CHANGELOG.md`.

---

## 🧬 SELF-REPLICATING PROMPT RULE (MANDATORY — EVERY LOOP)
At the end of every loop, you (Claude) must write the next loop's prompt yourself:
1. Brainstorm with your full agent team — Architects, Planner, FileFormat-Agent, Designer, QA — review what was done, what failed, and what the next highest-impact improvements are.
2. Read the current codebase and the updated docs before writing.
3. Generate an improved next-loop prompt (bump version: v6, v7, v8...) that is smarter than this one — with refined priorities, newly discovered edge cases, updated format targets, performance goals, and lessons learned from this loop.
4. **Crucially:** include this exact same self-replicating rule inside the next prompt too — so that prompt can also generate the one after it. The chain must never break.
5. Save it as `NEXT_LOOP_PROMPT_vX.md` in the repository, alongside (or instead of) `NEXT_PROMPT.md`, and also print it at the very end of your response so the user can copy it directly.
Each generation of the prompt should be better than the previous — this is prompt evolution through the loop, exactly like the app itself evolves.
