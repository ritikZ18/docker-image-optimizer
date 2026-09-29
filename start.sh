#!/usr/bin/env bash

set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_DIR="$ROOT_DIR/frontend"
RUN_DIR="$ROOT_DIR/.run"
BACKEND_LOG="$RUN_DIR/backend.log"
FRONTEND_LOG="$RUN_DIR/frontend.log"

start_backend=true
start_frontend=true

case "${1:-}" in
    --backend-only)
        start_frontend=false
        ;;
    --frontend-only)
        start_backend=false
        ;;
    "")
        ;;
    *)
        echo "Usage: $0 [--backend-only|--frontend-only]" >&2
        exit 2
        ;;
esac

require_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "Required command not found: $1" >&2
        exit 1
    fi
}

require_command docker

if [[ "$start_backend" == true ]]; then
    require_command mvn
    if [[ -d /usr/lib/jvm/java-21-openjdk-amd64 ]]; then
        export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
        export PATH="$JAVA_HOME/bin:$PATH"
    fi
    if ! command -v javac >/dev/null 2>&1 || [[ "$(java -version 2>&1 | head -n 1)" != *'version "21'* ]]; then
        echo "Java 21 JDK is required. Set JAVA_HOME to your Java 21 installation." >&2
        exit 1
    fi
fi

if [[ "$start_frontend" == true ]]; then
    require_command npm
    if [[ ! -d "$FRONTEND_DIR/node_modules" ]]; then
        echo "Frontend dependencies are missing. Run: (cd frontend && npm install)" >&2
        exit 1
    fi
fi

mkdir -p "$RUN_DIR"

echo "Starting PostgreSQL..."
(cd "$ROOT_DIR" && docker compose up -d postgres)

pids=()
cleanup() {
    if ((${#pids[@]} > 0)); then
        echo
        echo "Stopping application processes..."
        kill "${pids[@]}" 2>/dev/null || true
        wait "${pids[@]}" 2>/dev/null || true
    fi
}
trap cleanup EXIT INT TERM

if [[ "$start_backend" == true ]]; then
    echo "Starting Spring Boot API on http://localhost:8080"
    (cd "$ROOT_DIR" && mvn spring-boot:run >"$BACKEND_LOG" 2>&1) &
    pids+=("$!")
fi

if [[ "$start_frontend" == true ]]; then
    echo "Starting Next.js frontend on http://localhost:3000"
    (cd "$FRONTEND_DIR" && npm run dev >"$FRONTEND_LOG" 2>&1) &
    pids+=("$!")
fi

echo ""
echo "ImageSmith is running. Press Ctrl+C to stop application processes."
echo "Backend log:  $BACKEND_LOG"
echo "Frontend log: $FRONTEND_LOG"

wait -n "${pids[@]}"
exit_code=$?
echo "An application process exited with code $exit_code." >&2
exit "$exit_code"