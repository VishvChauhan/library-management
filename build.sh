#!/usr/bin/env bash
set -euo pipefail

# Move to project root regardless of where the script is called from
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# ── Colours ───────────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

sep() { echo -e "${CYAN}──────────────────────────────────────────────────────────────${RESET}"; }

# ── Timing ────────────────────────────────────────────────────────────────────
START_EPOCH=$(date +%s)

# Accumulate overall result; stays 0 unless a step fails
BUILD_EXIT=0

# ── Step 1: Clean ─────────────────────────────────────────────────────────────
sep
echo -e "${BOLD}[1/3] Cleaning previous build...${RESET}"
./gradlew clean

# ── Step 2: Test ──────────────────────────────────────────────────────────────
sep
echo -e "${BOLD}[2/3] Running tests...${RESET}"
./gradlew test || BUILD_EXIT=1

# ── Step 3: Build (package JAR) ───────────────────────────────────────────────
sep
echo -e "${BOLD}[3/3] Packaging JAR...${RESET}"
# -x test: tests already ran in step 2; skip to avoid a second run
./gradlew build -x test || BUILD_EXIT=1

# ── Duration ──────────────────────────────────────────────────────────────────
END_EPOCH=$(date +%s)
ELAPSED=$(( END_EPOCH - START_EPOCH ))
if [ "$ELAPSED" -ge 60 ]; then
  DURATION_FMT="$((ELAPSED / 60))m $((ELAPSED % 60))s"
else
  DURATION_FMT="${ELAPSED}s"
fi

# ── Parse JUnit XML test results ──────────────────────────────────────────────
TOTAL=0; FAILED=0; SKIPPED=0
XML_DIR="build/test-results/test"
if [ -d "$XML_DIR" ]; then
  while IFS= read -r xml; do
    t=$(grep -oE 'tests="[0-9]+"'    "$xml" | grep -oE '[0-9]+' | head -1)
    f=$(grep -oE 'failures="[0-9]+"' "$xml" | grep -oE '[0-9]+' | head -1)
    e=$(grep -oE 'errors="[0-9]+"'   "$xml" | grep -oE '[0-9]+' | head -1)
    s=$(grep -oE 'skipped="[0-9]+"'  "$xml" | grep -oE '[0-9]+' | head -1)
    TOTAL=$(( TOTAL   + ${t:-0} ))
    FAILED=$(( FAILED + ${f:-0} + ${e:-0} ))
    SKIPPED=$(( SKIPPED + ${s:-0} ))
  done < <(find "$XML_DIR" -name "*.xml" 2>/dev/null)
fi
PASSED=$(( TOTAL - FAILED - SKIPPED ))

# ── Find the Spring Boot fat JAR (exclude *-plain.jar) ────────────────────────
JAR_PATH=""
JAR_SIZE="unknown"
for jar in build/libs/*.jar; do
  [[ "$jar" == *"-plain.jar" ]] && continue
  JAR_PATH="$jar"
  break
done
if [ -n "$JAR_PATH" ] && [ -f "$JAR_PATH" ]; then
  JAR_SIZE="$(du -sh "$JAR_PATH" | cut -f1)"
fi

# ── Report ────────────────────────────────────────────────────────────────────
sep
echo -e "${BOLD}  Build Report${RESET}"
sep

if [ "$BUILD_EXIT" -eq 0 ]; then
  echo -e "  Status    ${GREEN}${BOLD}BUILD SUCCESS${RESET}"
else
  echo -e "  Status    ${RED}${BOLD}BUILD FAILURE${RESET}"
fi

echo -e "  Duration  ${BOLD}${DURATION_FMT}${RESET}"
echo ""

if [ -n "$JAR_PATH" ]; then
  echo -e "  JAR       ${BOLD}${JAR_PATH}${RESET}  (${JAR_SIZE})"
else
  echo -e "  JAR       ${YELLOW}not found — build may have failed${RESET}"
fi
echo ""

echo -e "  Tests     ${GREEN}${PASSED} passed${RESET}  |  ${RED}${FAILED} failed${RESET}  |  ${YELLOW}${SKIPPED} skipped${RESET}  |  ${TOTAL} total"

sep
echo ""

exit "$BUILD_EXIT"
