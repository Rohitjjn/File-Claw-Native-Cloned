# AGENTS.md: how the improvement loop works

This file lets the next run resume with full context. It is the operating manual for the repository's improvement loop; versioned prompt history and the next prompt are indexed in `docs/prompts/README.md`.

## Roles and truthful tool use
Architect · Reader/Analyzer · Planner · Coder · Designer · FileFormat · QA/Tester · Reviewer · DevOps · Docs · Release are useful review perspectives, not agents that are automatically present. Do not claim a multi-agent team ran unless an actual runner produced verifiable work. The user requested Hermes Agent from `https://github.com/NousResearch/hermes-agent`; it was inspected but was **not cloned, installed, or run** in Loop 6. Use it in a later loop only if a configured runner/provider is actually available. Do not request or store credentials in chat.

Arena Agent Mode did not expose the underlying model identifier in this session. Report that limitation; do not attribute this work to a guessed model such as Claude Sonnet 5 or claim Hermes used a model.

## State to read on every run (in this order)
`docs/AGENTS.md` → `docs/TODO.md` (KNOWN ISSUES first) → `docs/CHANGELOG.md` → `docs/ROADMAP.md` → `docs/ARCHITECTURE.md`, `docs/DESIGN.md`, `docs/SECURITY_AUDIT.md`, `docs/SANITY_CHECKLIST.md` → `docs/prompts/README.md` → source.
Never recreate docs; update them. Add a CHANGELOG entry with **measured** numbers and explicitly label unverified work.

## Commands
```
./gradlew testDebugUnitTest jacocoCoreVerify   # tests + 80% core coverage gate
./gradlew lintDebug detekt                      # static analysis
./gradlew assembleDebug                         # APK
kotlinc + tools/bench/CoreBench.kt              # JVM benchmarks (old vs new)
kotlinc + tools/GenFormatDocs.kt                # regenerate the format matrix
```

## Environment lessons from cycle 1 (important for the next run)
- The authoring sandbox had **no Android SDK and no Maven/Google/Gradle-distribution access**, only GitHub. Gradle could not run there. If the
  next run has the same limits, repeat the strategy: keep new logic in `core/` (pure Kotlin), compile with a GitHub-sourced `kotlinc`, run JUnit tests,
  measure with the JaCoCo CLI, and clearly label everything else *not verified*. If the next run **can** run Gradle, do the Quality Gate first and
  fix compile fallout before adding features.
- `/bin/sh` does not brace-expand: use `bash -c` or separate `mkdir` args.
- Kotlin supports nested block comments: never write `*/` or `/*` inside a KDoc (e.g. `image/*`).
- `InputStream.readNBytes` is not available below API 33; `java.nio.file` is fine on API 26+.
- Byte signature checks must mask with `and 0xFF` before comparing (signed-byte bug found by tests).
- **Loop 6 environment:** `java` and `kotlinc` were not installed; `ANDROID_HOME` was unset. `gradlew` is tracked without its executable bit (`100644`); invoking it through `bash gradlew testDebugUnitTest --no-daemon` reached the wrapper but failed because Java was unavailable. Do not treat the Gradle suite as run.
- Node.js 22.22.3 was available. Extracted PDF.js/DOCX/preview JavaScript and bundled PDF.js module syntax checks passed; these do not verify WebView or Android runtime behavior.

## Decisions taken without asking (cycle 1)
1. Excel became **view-only** (editing flattened workbooks); CSV/TSV keep editing with explicit save.
2. HTML preview scripts default **off** with a per-file opt-in.
3. RAR stays unsupported (previous owner dropped junrar), but reported honestly.
4. Files opened from other apps are edited as an in-app copy (with a visible message) until write-back is built.
5. Glass design shipped as tokens + cards + app bar + permission screen; root-level backdrop deferred because opaque Scaffolds + NavHost crossfades would overlap.
6. Gradle wrapper moved 8.5 → 8.7 (AGP 8.4.2 needs ≥ 8.6).
7. Preview cap kept at 250k chars (unmeasured perf), made safe via read-only flag rather than raising it.

## Loop 6 decisions (PDF, DOCX, TXT, Markdown only)
8. Large TXT/Markdown previews stay bounded to the existing preview window. Full-file search streams the file, counts all matches, stores at most 10,000 navigable byte offsets, and loads a bounded excerpt at the selected result; it does not retain the entire document in memory. The complete large-file viewer/pager remains a Loop 7 goal.
9. TXT/Markdown print and copy use a bounded 2,000,000-character load for truncated files; larger files fail with a visible message rather than silently printing/copying only the preview excerpt. Small, already-loaded files use the detected source charset.
10. Bundled PDF.js 4.10.38 is served locally through `WebViewAssetLoader`; no remote PDF assets were added. Its Apache, CMap, and font notices are retained and listed in `THIRD_PARTY_NOTICES.md`.
11. DOCX needs file access only to load bundled `android_asset/editor/` resources; cross-file and content access are disabled, and non-editor top-level navigation is blocked. PDF.js uses its own local asset path and the exact PDF file path.
12. Current work is scoped only to PDF, DOCX, TXT, and Markdown. The next prompt remains within those four formats.

## Release checklist
Version bump (`app/build.gradle.kts`) · CHANGELOG · TODO/ROADMAP · regenerate FILE_FORMATS · SANITY_CHECKLIST run · zip + `PATCH_NOTES.md` · next-loop prompt.
