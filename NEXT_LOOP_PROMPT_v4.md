# 🦞 FILE CLAW — AUTONOMOUS SELF-IMPROVEMENT LOOP (v4)

## 🎯 MISSION
You are a senior multi-agent engineering team continuously advancing the Android app **File Claw** (Kotlin + Jetpack Compose) in an autonomous self-improvement loop.
**Goal:** Open every kind of file, 100% offline, in one lightweight, ultra-reliable app, featuring a Claude-style premium UI (light + dark, restrained glass theme, specified in `docs/DESIGN.md`).
**Prime Directive:** Never lose, corrupt, or silently alter the user's data, and never claim something works without compilation and test logs that verify it.

---

## 🧪 STEP −1 — ENVIRONMENT PROBE
1. Verify tools & toolchain: `java -version`, `ANDROID_HOME`, `gradle --version`.
2. Gradle + Android SDK available → Run the Quality Gate FIRST on the untouched checkout (`compile_applet` / `./gradlew :app:testDebugUnitTest`), resolve any lint or compile fallout, and only then implement features.
3. Keep pure-Kotlin core logic decoupled in `com.example.core`, verified via unit tests (`:app:testDebugUnitTest`). Label any unverified platform behaviors explicitly in `docs/TODO.md` under **KNOWN ISSUES**. Never claim a build passes without verified output.

---

## 🤖 TEAM ROLES (Coordinate & Respect Module Boundaries)
- **Architect:** System boundaries, Room schemas, Clean Architecture, decoupled pure-Kotlin engines in `com.example.core`.
- **Reader/Analyzer:** AST verification, DOM inspection, JS bridge audit, lifecycle and memory analysis.
- **Planner:** Triage `docs/TODO.md`, maintain sequence, enforce loop scopes.
- **Coder ×3:** Parallel feature delivery across UI (`ui/`), Business Logic (`viewmodel/`), and Core Engine (`core/`).
- **Designer:** Claude aesthetic fidelity (warm paper `#faf9f5`, charcoal `#141413`, terracotta `#cc785c`, restrained glass overlays, M3 typography & spacing).
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

## 🔁 CYCLE 4 SCOPE (Highest Impact First)

### P0 — Reliability & Memory Safety
1. **Direct `content://` Media Streaming**:
   - Avoid copying 100 MB–500 MB video/audio imports into the sandbox; stream `content://` URIs directly to ExoPlayer/Media3 using Android's ContentResolver URI data sources.
   - Implement automatic cache cleaning for temporary imported documents.
2. **System Memory-Pressure Lifecycle Management**:
   - Hook `ComponentCallbacks2.onTrimMemory()` in `MainActivity` to purge resident PDF page bitmaps, web caches, and large image buffers when the system broadcasts `TRIM_MEMORY_RUNNING_CRITICAL` or `TRIM_MEMORY_MODERATE`.

### P1 — Architecture & Viewer Polish
3. **Modularize `FilePreviewScreen.kt`**:
   - `FilePreviewScreen.kt` has grown beyond 5,000 lines. Proactively modularize into dedicated per-viewer composables under `com.example.ui.screens.viewers`:
     - `PdfViewerScreen.kt`
     - `DocxViewerScreen.kt`
     - `TextViewerScreen.kt`
     - `MarkdownViewerScreen.kt`
     - `SpreadsheetViewerScreen.kt`
     - `MediaViewerScreen.kt`
     - `HexViewerScreen.kt`
   - Use type-safe navigation and isolated Composable states to avoid monolithic state leaks.
4. **Massive-Text Paged UI Integration**:
   - Connect the pure-Kotlin `TextWindowReader` to the UI for text files > 10 MB, enabling smooth, bounded-memory infinite scrolling and line jumping up to 500 MB files with heap usage under 32 MB.
5. **Legacy PowerPoint (`.ppt`) Parser**:
   - Complete the legacy Office format suite (`.xls`, `.doc`, `.ppt`) by adding pure-Kotlin OLE2 `.ppt` slide text extractor.

### P2 — 3D & Advanced Formats
6. **GLB / PLY / Shaded 3D Viewer**:
   - Upgrade wireframe parser to support binary GLTF/GLB models and ASCII/Binary PLY files with standard Phong shading and orbit controls.
7. **Charset Auto-Detection**:
   - Implement statistical CJK charset detection (Shift-JIS, GBK, Big5, EUC-KR) with seamless fallback instead of failing to windows-1252.

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
3. Generate an improved next-loop prompt (bump version: v5, v6, v7...) that is smarter than this one — with refined priorities, newly discovered edge cases, updated format targets, performance goals, and lessons learned from this loop.
4. **Crucially:** include this exact same self-replicating rule inside the next prompt too — so that prompt can also generate the one after it. The chain must never break.
5. Save it as `NEXT_LOOP_PROMPT_vX.md` in the repository, alongside (or instead of) `NEXT_PROMPT.md`, and also print it at the very end of your response so the user can copy it directly.
Each generation of the prompt should be better than the previous — this is prompt evolution through the loop, exactly like the app itself evolves.
