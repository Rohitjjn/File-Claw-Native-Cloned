# File Claw Native

Native Android file browser, previewer and editor built with Kotlin, Jetpack Compose and Room.
The source archive has been extracted into this repository; open the repository root in Android Studio.
No Gemini API key or signing configuration edits are required for a debug build.

## Status

**Architecture migration: preparation only; blocked at the build baseline.** Production code is unchanged.
The current application still has mixed-responsibility screens and services; the target format-oriented
architecture is a plan, not a delivered implementation. No new format support has been added.

Namespace: `com.example`. Application ID: `com.aistudio.filesclaw.apxqzy`.
Minimum SDK 26; compile/target SDK 34; Java 17; Kotlin 1.9.22.

## Build and verification

See [BUILD_GUIDE.md](BUILD_GUIDE.md). The preparation environment had no Java/Kotlin toolchain
or Android SDK, so neither Android compilation nor tests could run. Historical test claims are
not results for this checkout.

## Developer entry points

- [Current architecture and migration plan](docs/ARCHITECTURE.md)
- [Production file inventory and proposed destinations](docs/FILE_MIGRATION_MAP.md)
- [Format ownership map](docs/FORMAT_PACKAGE_MAP.md)
- [Single-format development guide](docs/FORMAT_DEVELOPMENT_GUIDE.md)
- [Test plan and observed baseline](docs/TEST_PLAN.md)
- [Preparation changelog](docs/CHANGELOG.md)
- [Existing known issues](known-issues.md)

The original root specifications and `docs/legacy/` are historical context, not current build or
support guarantees. The source registry is `core/format/FormatRegistry.kt`; its support labels
are coarse and do not imply editing, saving or complete rendering fidelity.
