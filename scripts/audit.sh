#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

set -e

cd "$DIR/.."

# NVD API key: environment first, else the gitignored .env.build (kept out of .env, which docker-compose
# hands to the app container). Sourced, so `export`, quotes and comments behave as in any shell file.
if [[ -z "${NVD_API_KEY:-}" && -f .env.build ]]; then
    . ./.env.build
fi
NVD_API_KEY=${NVD_API_KEY%$'\r'}
export NVD_API_KEY
[[ -n "$NVD_API_KEY" ]] || echo "WARNING: NVD_API_KEY not set (environment or .env.build); the NVD download will be very slow."

echo "Running OWASP dependency-check..."

${MVN_CMD} org.owasp:dependency-check-maven:check

echo "Dependency-check completed. Report: target/dependency-check-report.html"

cd - > /dev/null
