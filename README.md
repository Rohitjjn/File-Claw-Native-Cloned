# File Claw

File Claw is an Android app for browsing files and opening them with the app's existing format-specific viewers. Some formats have a full viewer, some are shown through limited text extraction, and some are only identified. The exact list is in [Supported formats](docs/FILE_FORMATS.md).

## Project facts

| Item | Current source value |
|---|---|
| Android Gradle module | `:app` (the project has one app module) |
| Kotlin package namespace | `com.example` |
| Installed application ID | `com.aistudio.filesclaw.apxqzy` |
| Minimum Android version | API 26 |
| Compile / target SDK | API 34 / API 34 |
| Gradle app version defaults | version code `3`, version name `1.3.0` |
| Room database version | `4`; migration `3 → 4` adds `recent_files.sourceUri` |
| Runtime network permission | No `INTERNET` permission is declared in the manifest |

A manually started release workflow can override the version values; see [Build guide](BUILD_GUIDE.md). The database facts are documented in [Database and user data](docs/DATABASE.md).

## Open the project

1. Install Android Studio with a **JDK 17** runtime and Android SDK platform **34**.
2. Open this repository's root folder in Android Studio and allow Gradle sync to finish.
3. Select a connected Android device or emulator and run the `app` configuration.

There is no `.env` file or AI API key needed to build or run this app. Gradle does need network access to resolve build plugins and dependencies on a first build. The installed app does not declare Android's `INTERNET` permission.

To build a debug APK from a shell:

```bash
./gradlew :app:assembleDebug --no-daemon --max-workers=1
```

The expected output path is `app/build/outputs/apk/debug/app-debug.apk`. For compile, unit-test, lint, coverage, and release commands, see [BUILD_GUIDE.md](BUILD_GUIDE.md). For source-defined resource limits and the measurement checklist, see [PERFORMANCE.md](PERFORMANCE.md). The current checkout's actual verification status is recorded in [docs/STATUS.md](docs/STATUS.md); don't treat the existence of a command or an old log as proof it passed.

## How the app is organized

- `app/src/main/java/com/example/core/` contains shared detection, safety rules, and bounded I/O.
- `app/src/main/java/com/example/formats/<family>/` contains format-family handlers, parsers, policies, and viewers.
- `app/src/main/java/com/example/feature/` contains app screens and user flows.
- `app/src/main/java/com/example/platform/` contains shared Android integrations such as storage, printing, sharing, and WebView asset loading.
- `app/src/main/java/com/example/data/` contains the Room database and repositories.
- `app/src/main/assets/` contains bundled local preview pages and their third-party assets.
- `app/src/test/` contains local JVM unit tests. There is currently no `app/src/androidTest/` test suite.

Read [AGENTS.md](AGENTS.md) before making code changes. Browse the [documentation index](docs/README.md) or the [source-verified architecture map](docs/ARCHITECTURE.md).
