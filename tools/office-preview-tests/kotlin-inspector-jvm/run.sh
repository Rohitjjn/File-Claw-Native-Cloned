#!/usr/bin/env bash
# Compile and exercise the real OoxmlPackageInspector.kt on a plain JVM.
#
# The app normally compiles this file against Apache Commons Compress inside
# Gradle. This harness supplies a tiny source-compatible ZIP metadata shim so
# the inspector's own logic can be tested with only a JDK and Kotlin compiler,
# without an Android SDK or Maven access. It is a logic test, not a replacement
# for the Gradle/Android unit tests.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$HERE/../../.." && pwd)"
FIXTURES="${1:-$REPO_ROOT/tools/office-preview-tests/fixtures}"
OUTPUT="${2:-}"

KOTLINC="${KOTLINC:-kotlinc}"
KOTLINC_BIN="$(command -v "$KOTLINC" || true)"
if [[ -z "$KOTLINC_BIN" ]]; then
  echo "error: kotlinc not found. Set KOTLINC to a Kotlin compiler path or install kotlin-compiler@1.9.22." >&2
  exit 2
fi
KOTLIN_HOME="$(cd "$(dirname "$KOTLINC_BIN")/.." && pwd)"
KOTLIN_STDLIB="$KOTLIN_HOME/lib/kotlin-stdlib.jar"
if [[ ! -f "$KOTLIN_STDLIB" ]]; then
  echo "error: kotlin-stdlib.jar not found at $KOTLIN_STDLIB" >&2
  exit 2
fi

JAVAC="${JAVAC:-${JAVA_HOME:+$JAVA_HOME/bin/javac}}"
JAVAC="${JAVAC:-javac}"
JAVA="${JAVA:-${JAVA_HOME:+$JAVA_HOME/bin/java}}"
JAVA="${JAVA:-java}"

echo "Kotlin compiler: $("$KOTLINC" -version 2>&1 | head -n 1)"
echo "Java runtime:    $("$JAVA" -version 2>&1 | head -n 1)"
echo "Fixtures:        $FIXTURES"

if [[ -n "$OUTPUT" ]]; then
  mkdir -p "$(dirname "$OUTPUT")"
fi

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP/stub" "$TMP/classes"

"$JAVAC" -proc:none -d "$TMP/stub" \
  "$HERE"/src/main/java/org/apache/commons/compress/archivers/zip/*.java

"$KOTLINC" -classpath "$TMP/stub" -d "$TMP/classes" \
  "$REPO_ROOT/app/src/main/java/com/example/formats/ooxml/OoxmlPackageInspector.kt" \
  "$HERE/src/main/kotlin/OoxmlInspectorHarness.kt"

RUN_ARGS=("$FIXTURES")
if [[ -n "$OUTPUT" ]]; then
  RUN_ARGS+=("$OUTPUT")
fi
"$JAVA" -cp "$TMP/classes:$TMP/stub:$KOTLIN_STDLIB" \
  OoxmlInspectorHarnessKt "${RUN_ARGS[@]}"
