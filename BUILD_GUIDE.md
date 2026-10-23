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
./gradlew testDebugUnitTest            # 91 JUnit tests (core/)
./gradlew jacocoCoreVerify             # fails below 80 % line coverage of com.example.core
./gradlew lintDebug detekt             # lint must have 0 errors; detekt is advisory for now
./gradlew assembleDebug
```
If the first build fails to compile, the likely places are listed in `docs/TODO.md` → KNOWN ISSUES #1.

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

## Automated GitHub Actions Release Workflow
A manual GitHub Actions workflow is provided in `.github/workflows/build-and-release.yml`.

### How to run:
1. Go to your repository on GitHub.
2. Click on the **Actions** tab.
3. In the left sidebar, select **Build and Release Android App**.
4. Click **Run workflow**:
   - **App Version Name**: Enter your release version (e.g., `1.2.0` or `2.0.0`).
   - **App Version Code**: Enter the numeric version code (e.g., `3`, or leave blank to auto-increment).
   - **Build Type**: Choose `release` (default) or `debug`.
   - **Build Bundle**: Check to also generate `.aab` (Android App Bundle).
   - **Release Title & Notes**: Optionally add custom title and changelog notes.
5. Click the green **Run workflow** button.

### What happens:
- GitHub Actions compiles the Android app targeting all modern architectures (`arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86`).
- It generates the universal Release APK (`FilesClaw-v<version>-release.apk`) and Android App Bundle (`FilesClaw-v<version>.aab`).
- It computes SHA-256 checksums for security verification.
- It automatically creates a new **GitHub Release** tagged `v<version>` and uploads all artifacts directly to the Release section so anyone can easily download and install the app!
- If optional signing secrets (`FILECLAW_KEYSTORE_BASE64`, `FILECLAW_STORE_PASSWORD`, etc.) are configured in repository Settings → Secrets, it uses them; otherwise, it signs with the fallback key so sideloading works immediately out of the box.

## Benchmarks / generated docs
`tools/bench/CoreBench.kt` (old vs new algorithms) · `tools/GenFormatDocs.kt` (regenerates the format matrix). Both compile against `com.example.core` with `kotlinc`.
