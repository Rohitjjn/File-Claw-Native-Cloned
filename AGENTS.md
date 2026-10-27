# Agent instructions

This repository is an existing Android app. Treat the checked-in source, Gradle configuration, manifest, tests, assets, and licenses as the authority. Old logs, prompts, issue lists, and copied project plans are not evidence that behavior or a build is current.

## Required approach

1. Read this file, `docs/STATUS.md`, the relevant architecture/format/security docs, and the implementation plus tests/assets that own the behavior before editing.
2. Trace a user-visible flow end to end: Activity or intent → `MainViewModel` → detection and registered format handler → existing content state → feature screen → family viewer/asset pipeline.
3. Keep each format's parsing and processing in its `formats/<family>/` package. A screen may compose a viewer and manage shared UI state, but must not become the parser or renderer owner.
4. Keep shared Android URI/storage, bounded I/O, atomic writes, archive safety, cache, Room/preferences, navigation, print/share, WebView hardening, errors, and design infrastructure in their existing shared owners. Do not add a parallel format registry or empty package.
5. Preserve existing user-visible behavior, supported formats, security limits, bundled assets and license notices. Do not add preview or format support unless the user specifically changes scope.
6. Do not change the application ID, namespace, SDK levels, signing setup, dependency set, Room schema/migrations, or Gradle module structure without a documented technical reason and user approval.
7. Keep documentation in English. Write it so a non-coder can follow the product/build instructions and an AI coding agent can find the exact source owner, limits, tests, and verification status.
8. Report only checks actually run. Separate source inspection and static checks from Kotlin compilation, unit tests, APK builds, and Android/WebView device tests.

## Current scope

The active request is the Office preview repair and expansion pass: root-cause fixes for DOCX/XLSX/PPTX, verified OOXML variant and legacy Office support, hardened offline viewers, consistent UI controls and reproducible tests. Preserve existing formats, the single `FormatRegistry`, safe-save/URI behavior, bundled assets and licenses. Do not claim Android runtime verification from browser/unit tests alone; see `docs/STATUS.md` and `docs/PREVIEW_PIPELINES.md`.

## Quality gate

Use the repository's wrapper and run, when the toolchain is available:

```bash
./gradlew :app:compileDebugKotlin --no-daemon --max-workers=1
./gradlew :app:testDebugUnitTest --no-daemon --max-workers=1
./gradlew :app:assembleDebug --no-daemon --max-workers=1
./gradlew :app:lintDebug :app:detekt --no-daemon --max-workers=1
./gradlew :app:jacocoCoreVerify --no-daemon --max-workers=1
```

Also run `git diff --check`; run `node --check` on any edited JavaScript (extract inline scripts where needed); parse manifest/resource XML when changed; and test Android/WebView behavior on an emulator or device for renderer changes. If Java, Android SDK, emulator, or other tooling is unavailable, record the exact command and blocker in `docs/STATUS.md` rather than claiming success.

## Ownership quick map

- Detection and route IDs: `core/format/FormatDetector.kt`, `core/format/FormatRegistry.kt`.
- Format contract and app content states: `domain/preview/`.
- Family handlers and viewers: `formats/<family>/`.
- Default handler wiring: `formats/registry/DefaultFormatHandlers.kt` and `FormatRegistryOperations.kt`.
- Cross-family preview composition: `feature/preview/FilePreviewScreen.kt`.
- App coordination: `viewmodel/MainViewModel.kt`.
- Android adapters: `platform/`.
- Persistent entities and migrations: `data/`.
- Offline WebView assets and third-party notices: `app/src/main/assets/` and `THIRD_PARTY_NOTICES.md`.

The Arena branch for this session is fixed. Do not switch or create a different branch. Do not ask for or store credentials in chat.
