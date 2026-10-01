#!/usr/bin/env bash

set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_DIR="$ROOT_DIR/frontend"
RUN_DIR="$ROOT_DIR/.run"
BACKEND_LOG="$RUN_DIR/backend.log"
FRONTEND_LOG="$RUN_DIR/frontend.log"

start_backend=true
start_frontend=true
restart_services=false
reset_database=false

for argument in "$@"; do
    case "$argument" in
        --backend-only)
            start_frontend=false
            ;;
        --frontend-only)
            start_backend=false
            ;;
        --restart)
            restart_services=true
            ;;
        --reset-db)
            reset_database=true
            ;;
        *)
            echo "Usage: $0 [--restart] [--reset-db] [--backend-only|--frontend-only]" >&2
            exit 2
            ;;
    esac
done

require_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "Required command not found: $1" >&2
        exit 1
    fi
}

require_command docker
require_command curl

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

stop_port() {
    local port="$1"
    if command -v fuser >/dev/null 2>&1; then
        fuser -k "$port/tcp" >/dev/null 2>&1 || true
    fi
}

if [[ "$reset_database" == true ]]; then
    echo "RESET: removing PostgreSQL data volume..."
    (cd "$ROOT_DIR" && docker compose down -v)
fi

if [[ "$restart_services" == true ]]; then
    echo "RESTART: stopping existing application services..."
    stop_port 8080
    stop_port 3000
    echo "RESTART: PostgreSQL data is preserved."
fi

echo "SPINNING UP: PostgreSQL..."
(cd "$ROOT_DIR" && docker compose up -d --wait postgres)

pids=()
cleanup() {
    if ((${#pids[@]} > 0)); then
        echo
        echo "STOPPING: application processes..."
        for pid in "${pids[@]}"; do
            kill -- "-$pid" 2>/dev/null || kill "$pid" 2>/dev/null || true
        done
        wait "${pids[@]}" 2>/dev/null || true
    fi
}
trap cleanup EXIT INT TERM

if [[ "$start_backend" == true ]]; then
    echo "SPINNING UP: Spring Boot API..."
    (cd "$ROOT_DIR" && exec setsid mvn spring-boot:run >"$BACKEND_LOG" 2>&1) &
    pids+=("$!")
fi

if [[ "$start_frontend" == true ]]; then
    echo "SPINNING UP: Next.js frontend..."
    (cd "$FRONTEND_DIR" && exec setsid npm run dev >"$FRONTEND_LOG" 2>&1) &
    pids+=("$!")
fi

wait_for_http() {
    local name="$1"
    local url="$2"
    local pid="$3"
    local attempts=0
    local spinner_index=0
    local spinner='|/-\\'
    while ((attempts < 60)); do
        if ! kill -0 "$pid" 2>/dev/null; then
            printf '\r%-72s\n' "FAILED: $name stopped during startup. Check its log."
            return 1
        fi
        http_status="$(curl --silent --output /dev/null --write-out '%{http_code}' --connect-timeout 1 --max-time 2 "$url" 2>/dev/null || true)"
        if [[ "$http_status" != "000" ]]; then
            printf '\r%-72s\n' "READY: $name at $url"
            return 0
        fi
        printf '\rSPINNING UP: %-24s [%s] waiting for readiness' "$name" "${spinner:spinner_index:1}"
        spinner_index=$(( (spinner_index + 1) % 4 ))
        attempts=$((attempts + 1))
        sleep 1
    done
    printf '\r%-72s\n' "FAILED: $name did not become ready within 60 seconds."
    return 1
}

if [[ "$start_backend" == true ]]; then
    backend_pid="${pids[0]}"
    wait_for_http "Spring Boot API" "http://localhost:8080/api/v1/repositories" "$backend_pid"
fi

if [[ "$start_frontend" == true ]]; then
    frontend_index=0
    if [[ "$start_backend" == true ]]; then
        frontend_index=1
    fi
    wait_for_http "Next.js frontend" "http://localhost:3000" "${pids[$frontend_index]}"
fi

echo "READY: ImageSmith is running. Press Ctrl+C to stop all application services."
echo "Backend log:  $BACKEND_LOG"
echo "Frontend log: $FRONTEND_LOG"

wait -n "${pids[@]}"
exit_code=$?
echo "An application process exited with code $exit_code." >&2
exit "$exit_code"