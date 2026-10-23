# Patch notes: v1.1.0 "Foundation & Safety"

This zip is the **full updated source** (same layout as your `File-Claw-native--main` download). Replace your repo contents with it.

## Read this first
The Android layer of this patch was written in a sandbox with no Android SDK and no Maven/Google access. What was actually run:
- ✅ `core/` (pure Kotlin) compiled with Kotlin 1.9.22: **91 tests pass, 98.3 % line / 83.2 % branch coverage** (JaCoCo CLI), via `tools/local-verify/verify-core.sh` using a stand-in JUnit runner.
- ✅ All changed Kotlin files pass a parse check (no syntax errors).
- ✅ JVM benchmarks for the claims in the CHANGELOG (`tools/bench/CoreBench.kt`).
- ❌ **Not run:** `./gradlew build / test / lint / detekt / assembleDebug`, any Compose UI, any device. Type-level errors against Android/Compose/zip4j/commons-compress APIs are possible.

## Build
```
./gradlew assembleDebug          # single command; see BUILD_GUIDE.md for the full Quality Gate
```

## What changed (highlights; full list in docs/CHANGELOG.md)
- **Data-loss fixes:** large-file edit truncation, spreadsheet auto-save (Excel is now view-only; CSV/TSV save explicitly), non-atomic writes, `.tsv`/`.xls` mis-saves.
- **Universal opener core:** content-sniffing format detection (399 extensions), 13 new format families (SQLite, SVG, RTF, OpenDocument, EPUB, fonts, STL, OBJ, gz/bz2/xz, tar.bz2/tar.xz, ar/deb/cpio, ZIP packages, HEIC/AVIF/TIFF/DNG).
- **Security:** zip-bomb/zip-slip limits, FileProvider narrowed, backup off, locked-down WebViews, sanitised HTML, safe imports.
- **Open-with fixes:** works without all-files access; no re-import on rotation.
- **Build:** Gradle wrapper 8.7 (AGP 8.4.2 needs ≥ 8.6), fresh clones build without a keystore, JaCoCo gate, detekt, CI workflow, dead dependencies removed (~7 MB pdfbox).
- **Design:** glass tokens, glass cards/app bar/permission screen (full rollout in cycle 2).

## Behaviour changes you will notice
1. `.xlsx` is **view-only**. 2. CSV edits need the **Save** button (with an unsaved-changes prompt). 3. Large text files (> 250k chars) are **read-only** with a banner.
4. HTML preview has **scripts off** until you tap "Run scripts". 5. Files opened from other apps are edited as an **in-app copy** (you're told on save).
6. App backup is disabled. 7. Splash is shorter (500 → 150 ms).

## Files added at the root (historical v1.1.0 snapshot)
`BUILD_GUIDE.md`, `PATCH_NOTES.md`, the then-current prompt files, `THIRD_PARTY_NOTICES.md`, `tools/`, `config/`, `.github/workflows/ci.yml`.

## Later prompt archive update
The historical Cycle 2/3 prompts are now stored under `docs/prompts/` as `LOOP_02_PROMPT.md` and `LOOP_03_PROMPT.md`. The former root `NEXT_PROMPT.md` was removed because it was an exact duplicate of the old v3 prompt. See `docs/prompts/README.md` for the current Loop 7 handoff.
