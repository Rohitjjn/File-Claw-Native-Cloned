#!/bin/bash
# Verifies the pure-Kotlin core WITHOUT Gradle/Android: compile, run JUnit-style tests, optional JaCoCo coverage.
# Needs: java 17+, kotlinc 1.9.22 (https://github.com/JetBrains/kotlin/releases), optional JaCoCo zip (GitHub releases).
#   KOTLINC=/path/to/kotlinc/bin/kotlinc  JACOCO_DIR=/path/to/jacoco  ./tools/local-verify/verify-core.sh
# The tests use standard org.junit imports; Shim.kt/Runner.kt stand in for JUnit only when it cannot be downloaded.
set -e
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
K="${KOTLINC:-kotlinc}"; OUT="${OUT:-/tmp/fileclaw-verify}"
STDLIB="$(dirname "$K")/../lib/kotlin-stdlib.jar"
rm -rf "$OUT" && mkdir -p "$OUT/main" "$OUT/test" "$OUT/shim"
"$K" -jvm-target 17 "$ROOT"/tools/local-verify/Shim.kt "$ROOT"/tools/local-verify/Runner.kt -d "$OUT/shim" 2>&1 | grep -v jvm-target || true
"$K" -jvm-target 17 $(find "$ROOT/app/src/main/java/com/example/core" -name '*.kt') -d "$OUT/main" 2>&1 | grep -v jvm-target || true
"$K" -jvm-target 17 -cp "$OUT/main:$OUT/shim" $(find "$ROOT/app/src/test" -name '*.kt') -d "$OUT/test" 2>&1 | grep -v jvm-target || true
CLASSES=$(cd "$OUT/test" && find . -name '*Test.class' | sed 's#^\./##; s#\.class$##; s#/#.#g' | sort | tr '\n' ' ')
AGENT=""
if [ -n "$JACOCO_DIR" ]; then AGENT="-javaagent:$JACOCO_DIR/lib/jacocoagent.jar=destfile=$OUT/jacoco.exec,includes=com.example.core.*"; fi
java $AGENT -cp "$OUT/main:$OUT/test:$OUT/shim:$STDLIB" RunnerKt $CLASSES
if [ -n "$JACOCO_DIR" ]; then
  java -jar "$JACOCO_DIR/lib/jacococli.jar" report "$OUT/jacoco.exec" --classfiles "$OUT/main" \
    --sourcefiles "$ROOT/app/src/main/java" --csv "$OUT/coverage.csv" >/dev/null 2>&1 && echo "coverage csv: $OUT/coverage.csv"
fi
