# Verification plan and preparation results

## Observed on 2026-10-22

- `java -version`: exit 127, `java: command not found`.
- `command -v kotlinc`: no compiler found.
- `ANDROID_HOME` and `ANDROID_SDK_ROOT`: unset; no configured Android SDK.
- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:assembleDebug`:
  exit 1 before Gradle starts: `JAVA_HOME is not set and no 'java' command could be found`.
- All 366 archive payload files were compared byte-for-byte immediately before cleanup: passed.
- No production source, tests, assets, schemas, manifest or build configuration modified.

No test pass count, coverage, navigation, rendering or APK result is claimed. The supplied archive
has unit tests but no `androidTest` source tree, dedicated fixture tree or `.github/workflows`.
The local verification shell script contains `|| true` around compilation: its exit status alone is
not trustworthy evidence of successful compilation. Audit it before relying on its custom JUnit shim.

## Required gates once the toolchain is available

```sh
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:lintDebug :app:detekt
./gradlew :app:jacocoCoreVerify
```

Lint, detekt, coverage and instrumentation were not run in this environment. Current detekt is advisory;
coverage only includes core. When format helpers move out of core, preserve quality-gate scope explicitly
rather than making coverage appear better by excluding migrated code.

## Regression requirements

- Detector signatures, extensions, ambiguous MIME and container types; registry-to-handler routing.
- Every existing parser's valid, malformed, oversized, truncated and encoding cases.
- ZIP traversal, archive expansion/entry limits, unsafe names, password errors, nested entries.
- Text/CSV editability, BOM/encoding, truncation guards, atomic writes, archive/source URI write-back.
- DOC/XLS fallback behavior distinct from DOCX/XLSX; no invented legacy PPT support.
- Session cancellation, stream/descriptor closure, WebView/media cleanup and bounded caches.
- Room migration/schema compatibility; favorites, recents, index and preference persistence.

On a device/emulator test browsing, external ACTION_VIEW (content/file URI), import permissions,
back/forward navigation, rotation/process recreation, focus mode, keyboard shortcuts, find navigation,
text/CSV save, printing/PDF export, archive extraction, checksums, settings and themes. Verify every
existing preview family against pre-migration screenshots/behavior; include long PDF/DOCX documents,
large text, mixed-page orientation, multi-sheet workbooks, codec-dependent media and unsupported files.
