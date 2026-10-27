# Build and verification guide

This guide describes the build configuration in this checkout. It does not claim that the current working tree has built successfully; see [`docs/STATUS.md`](docs/STATUS.md) for the exact current result.

## Requirements

- JDK **17**. The Android Gradle Plugin is `8.4.2`; the project uses Gradle wrapper **8.7** and Kotlin **1.9.22**.
- Android SDK platform **34**. The app compiles against and targets API 34 and has `minSdk 26`.
- Network access during the first Gradle sync/build so Gradle can download plugins and dependencies from the configured repositories.
- Android Studio is the simplest way to install the SDK and choose a device. A device or emulator is needed only for installation and runtime checks, not for local unit tests or APK assembly.

Set `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) to the SDK directory, or let Android Studio create a local `local.properties` with `sdk.dir`. Do not commit local SDK paths. The wrapper script is executable in the current checkout; if a checkout loses that file mode, run `chmod +x gradlew`.

## Debug build and quality checks

Run from the repository root:

```bash
./gradlew :app:compileDebugKotlin --no-daemon --max-workers=1
./gradlew :app:testDebugUnitTest --no-daemon --max-workers=1
./gradlew :app:assembleDebug --no-daemon --max-workers=1
./gradlew :app:lintDebug :app:detekt --no-daemon --max-workers=1
./gradlew :app:jacocoCoreVerify --no-daemon --max-workers=1
```

The expected debug APK is `app/build/outputs/apk/debug/app-debug.apk`. `:app:jacocoCoreVerify` is configured to require at least 80% line coverage for `com.example.core`. Detekt is configured for the core source tree and currently uses `ignoreFailures = true`; it is advisory until that configuration changes.

For a single all-in-one check, the same task can be invoked in one Gradle process, but separate commands make it easier to see which stage failed:

```bash
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:assembleDebug --no-daemon --max-workers=1
```

## Office renderer checks (no Android SDK)

The bundled Office HTML/JavaScript viewers can be verified without Gradle:

```bash
cd tools/office-preview-tests
npm ci
npm run syntax
python3 validate_ooxml.py --fixtures fixtures --output results/ooxml-validation.json
npm test
```

These commands use Node 20+, Python 3.11+ and the pinned Chromium package in
`tools/office-preview-tests/package.json`. They do not compile Kotlin or exercise
Android `WebView`/`PrintManager`; run the Gradle and device checks below for
that. See [`tools/office-preview-tests/README.md`](tools/office-preview-tests/README.md).

## Release builds and signing

`app/build.gradle.kts` reads release signing values from environment variables:

- `FILECLAW_KEYSTORE`
- `FILECLAW_STORE_PASSWORD`
- `FILECLAW_KEY_ALIAS`
- `FILECLAW_KEY_PASSWORD`

Without a supplied release keystore, the build falls back to a debug key. That may be suitable for local installation but is **not a production Play Store signing identity**. Never place signing secrets in source control or chat.

The workflow at `.github/workflows/build-and-release.yml` is manually dispatched. It sets up Java 17, accepts a version name/code and debug/release choice, builds an APK, can also build a release AAB, generates SHA-256 checksums, and publishes or updates a GitHub Release. Running it publishes artifacts; do so only with the appropriate release approval.

## Useful source locations

- Versions and plugin aliases: `gradle/libs.versions.toml`.
- Wrapper version: `gradle/wrapper/gradle-wrapper.properties`.
- App ID, SDK levels, app versions, build types and dependencies: `app/build.gradle.kts`.
- Manifest and permissions: `app/src/main/AndroidManifest.xml`.
- Unit tests: `app/src/test/java/`.
- Room schema export location: `app/schemas/`.

The checked-in Gradle configuration is a single `:app` module. This guide does not authorize changing modules, application identity, SDK levels, signing, or dependencies.
