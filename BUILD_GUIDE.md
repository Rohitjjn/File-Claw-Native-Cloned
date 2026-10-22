# Build guide

## Requirements
JDK 17 · Android SDK (platform 34, build-tools 34) via Android Studio or `sdkmanager` · internet access for the first Gradle sync.
Set `ANDROID_HOME` (or `local.properties` → `sdk.dir=...`).

## One command
```
./gradlew assembleDebug
```
APK: `app/build/outputs/apk/debug/app-debug.apk`. No keystore needed: without `debug.keystore.base64` the AGP debug key is used.

## Quality Gate (run this first on a new machine; cycle 1 could not run it)
```
./gradlew build                        # compile + all checks
./gradlew testDebugUnitTest            # existing unit tests
./gradlew jacocoCoreVerify             # fails below 80 % line coverage of com.example.core
./gradlew lintDebug detekt             # lint must have 0 errors; detekt is advisory for now
./gradlew assembleDebug
```
See `docs/TEST_PLAN.md` for observed baseline blockers and `known-issues.md` for existing runtime issues.

## Verify the core without Gradle
```
KOTLINC=/path/to/kotlinc/bin/kotlinc JACOCO_DIR=/path/to/jacoco ./tools/local-verify/verify-core.sh
```

## Release build
```
export FILECLAW_KEYSTORE=/path/to/release.jks FILECLAW_STORE_PASSWORD=... FILECLAW_KEY_ALIAS=... FILECLAW_KEY_PASSWORD=...
./gradlew assembleRelease
```
Without those variables the release APK is signed with the debug key (sideloading only).

## Automation status

No GitHub Actions workflow was included in the supplied archive. Run the gates locally on a configured Android toolchain; do not assume release automation exists.

## Benchmarks / generated docs
`tools/bench/CoreBench.kt` (old vs new algorithms) · `tools/GenFormatDocs.kt` (regenerates the format matrix). Both compile against `com.example.core` with `kotlinc`.

## Preparation checkpoint (2026-10-22)

The current sandbox has no Java or Kotlin compiler and no configured Android SDK. Gradle exits before starting. Dependency repositories for Android/Gradle are outside the sandbox network allowlist. No APK or test result was produced; see `docs/TEST_PLAN.md`.
