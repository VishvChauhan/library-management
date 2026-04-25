#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# ── Config ────────────────────────────────────────────────────────────────────
PORT=8080
BASE_URL="http://localhost:${PORT}"
LOG_DIR="logs"
LOG_FILE="${LOG_DIR}/app.log"
STARTUP_TIMEOUT=90   # seconds to wait for Spring Boot to be ready
STARTUP_INTERVAL=2

# ── Colours ───────────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

sep()  { echo -e "${CYAN}──────────────────────────────────────────────────────────────${RESET}"; }
pass() { echo -e "  ${GREEN}PASS${RESET}  $*"; }
fail() { echo -e "  ${RED}FAIL${RESET}  $*"; }
info() { echo -e "  ${BOLD}$*${RESET}"; }

# Pretty-print JSON if python3 is available, otherwise cat
pretty_json() {
  if command -v python3 &>/dev/null; then
    python3 -m json.tool 2>/dev/null || cat
  else
    cat
  fi
}

# Make a request; outputs body to stdout, returns HTTP status via $HTTP_STATUS
http_request() {
  local method="$1"; shift
  local url="$1";    shift
  local tmp; tmp=$(mktemp)
  HTTP_STATUS=$(curl -s -o "$tmp" -w "%{http_code}" -X "$method" "$@" "$url")
  cat "$tmp"
  rm -f "$tmp"
}

SMOKE_FAILURES=0

smoke() {
  local label="$1" expected="$2" method="$3" url="$4"
  shift 4
  local tmp; tmp=$(mktemp)
  local status
  status=$(curl -s -o "$tmp" -w "%{http_code}" -X "$method" "$@" "$url")
  if [ "$status" -eq "$expected" ]; then
    pass "${label}  [HTTP ${status}]"
    cat "$tmp" | pretty_json
  else
    fail "${label}  [expected ${expected}, got ${status}]"
    cat "$tmp"
    SMOKE_FAILURES=$(( SMOKE_FAILURES + 1 ))
  fi
  rm -f "$tmp"
  echo ""
}

# ── Step 1: Ensure JAR exists ─────────────────────────────────────────────────
sep
echo -e "${BOLD}[1/5] Checking JAR...${RESET}"

find_jar() {
  JAR_PATH=""
  for jar in build/libs/*.jar; do
    [[ -f "$jar" ]] || continue
    [[ "$jar" == *"-plain.jar" ]] && continue
    JAR_PATH="$jar"
    return 0
  done
  return 1
}

if ! find_jar; then
  echo -e "  JAR not found — running ${BOLD}./build.sh${RESET} first..."
  echo ""
  ./build.sh
  echo ""
  if ! find_jar; then
    echo -e "${RED}Build failed. Cannot deploy.${RESET}"
    exit 1
  fi
fi

JAR_SIZE="$(du -sh "$JAR_PATH" | cut -f1)"
echo -e "  Found: ${BOLD}${JAR_PATH}${RESET}  (${JAR_SIZE})"

# ── Step 2: Kill any existing process on port ─────────────────────────────────
sep
echo -e "${BOLD}[2/5] Checking port ${PORT}...${RESET}"

EXISTING_PID=$(lsof -ti:"${PORT}" 2>/dev/null || true)
if [ -n "$EXISTING_PID" ]; then
  echo -e "  ${YELLOW}Killing existing process on port ${PORT} (PID ${EXISTING_PID})${RESET}"
  kill -9 "$EXISTING_PID" 2>/dev/null || true
  sleep 1
else
  echo -e "  Port ${PORT} is free"
fi

# ── Step 3: Start the JAR ─────────────────────────────────────────────────────
sep
echo -e "${BOLD}[3/5] Starting application...${RESET}"

mkdir -p "$LOG_DIR"
# Rotate previous log
[ -f "$LOG_FILE" ] && mv "$LOG_FILE" "${LOG_FILE}.prev"

java -jar "$JAR_PATH" > "$LOG_FILE" 2>&1 &
APP_PID=$!

echo -e "  PID:      ${BOLD}${APP_PID}${RESET}"
echo -e "  Log file: ${BOLD}${LOG_FILE}${RESET}"

# Guard: if the process dies immediately, surface the error
sleep 2
if ! kill -0 "$APP_PID" 2>/dev/null; then
  echo -e "${RED}Application exited immediately. Last log lines:${RESET}"
  tail -30 "$LOG_FILE"
  exit 1
fi

# ── Step 4: Wait for startup ──────────────────────────────────────────────────
sep
echo -e "${BOLD}[4/5] Waiting for startup (timeout: ${STARTUP_TIMEOUT}s)...${RESET}"
echo -n "  "

ELAPSED=0
until curl -sf "${BASE_URL}/api/books" > /dev/null 2>&1; do
  if [ "$ELAPSED" -ge "$STARTUP_TIMEOUT" ]; then
    echo ""
    echo -e "${RED}Startup timed out after ${STARTUP_TIMEOUT}s. Last log lines:${RESET}"
    tail -30 "$LOG_FILE"
    kill "$APP_PID" 2>/dev/null || true
    exit 1
  fi
  if ! kill -0 "$APP_PID" 2>/dev/null; then
    echo ""
    echo -e "${RED}Application crashed during startup. Last log lines:${RESET}"
    tail -30 "$LOG_FILE"
    exit 1
  fi
  echo -n "."
  sleep "$STARTUP_INTERVAL"
  ELAPSED=$(( ELAPSED + STARTUP_INTERVAL ))
done

echo -e "  ${GREEN}ready in ${ELAPSED}s${RESET}"

# ── Step 5: Print URLs + info ─────────────────────────────────────────────────
sep
echo -e "${BOLD}  Application is running${RESET}"
sep
echo -e "  ${GREEN}${BOLD}STATUS    UP${RESET}                          PID ${APP_PID}"
echo ""
echo -e "  API       ${BOLD}${BASE_URL}/api/books${RESET}"
echo -e "  Swagger   ${BOLD}${BASE_URL}/swagger-ui.html${RESET}"
echo -e "  API Docs  ${BOLD}${BASE_URL}/api-docs${RESET}"
echo -e "  H2        ${BOLD}${BASE_URL}/h2-console${RESET}  (JDBC: jdbc:h2:mem:librarydb  user: sa)"
echo ""
echo -e "  Log       ${BOLD}${LOG_FILE}${RESET}"
echo -e "  Stream    tail -f ${LOG_FILE}"
sep

# ── Step 6: Smoke tests ───────────────────────────────────────────────────────
echo ""
echo -e "${BOLD}[5/5] Running smoke tests...${RESET}"
echo ""

BOOK_PAYLOAD='{
  "title":         "Smoke Test Book",
  "author":        "Deploy Script",
  "isbn":          "978-0000000001",
  "genre":         "TECHNOLOGY",
  "publishedYear": 2024
}'

# ── Smoke 1: POST /api/books ──────────────────────────────────────────────────
echo -e "  ${CYAN}POST /api/books${RESET}"
TMP=$(mktemp)
POST_STATUS=$(curl -s -o "$TMP" -w "%{http_code}" \
  -X POST "${BASE_URL}/api/books" \
  -H "Content-Type: application/json" \
  -d "$BOOK_PAYLOAD")
POST_BODY=$(cat "$TMP"); rm -f "$TMP"

if [ "$POST_STATUS" -eq 201 ]; then
  pass "Book created  [HTTP 201]"
  echo "$POST_BODY" | pretty_json
  # Extract ID for the GET-by-id smoke test
  BOOK_ID=$(echo "$POST_BODY" | grep -oE '"id"[[:space:]]*:[[:space:]]*[0-9]+' | grep -oE '[0-9]+' | head -1)
else
  fail "Expected 201, got ${POST_STATUS}"
  echo "$POST_BODY"
  BOOK_ID=""
  SMOKE_FAILURES=$(( SMOKE_FAILURES + 1 ))
fi
echo ""

# ── Smoke 2: GET /api/books ───────────────────────────────────────────────────
echo -e "  ${CYAN}GET /api/books${RESET}"
smoke "List all books" 200 GET "${BASE_URL}/api/books"

# ── Smoke 3: GET /api/books/{id} ─────────────────────────────────────────────
echo -e "  ${CYAN}GET /api/books/${BOOK_ID:-<id>}${RESET}"
if [ -n "${BOOK_ID:-}" ]; then
  smoke "Get book by id" 200 GET "${BASE_URL}/api/books/${BOOK_ID}"
else
  echo -e "  ${YELLOW}SKIP${RESET}  No book ID available (POST failed)"
  echo ""
fi

# ── Final result ──────────────────────────────────────────────────────────────
sep
if [ "$SMOKE_FAILURES" -eq 0 ]; then
  echo -e "  ${GREEN}${BOLD}All smoke tests passed.${RESET}  Application is ready."
else
  echo -e "  ${RED}${BOLD}${SMOKE_FAILURES} smoke test(s) failed.${RESET}  Check the log: ${LOG_FILE}"
fi
sep
echo ""
