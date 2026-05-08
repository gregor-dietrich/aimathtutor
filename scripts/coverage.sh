#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

set -e

cd "$DIR/.."

OPEN_REPORT=false
while getopts "o" opt; do
    case "$opt" in
        o) OPEN_REPORT=true ;;
    esac
done

CSV="target/site/jacoco/jacoco.csv"
REPORT=".coverage.md"

REVISION=${REVISION:-1.0.0-SNAPSHOT}

echo "Running tests with JaCoCo coverage..."

${MVN_CMD} -q test -Dquarkus.log.console.enabled=false -Dquarkus.log.file.enabled=false -Drevision="${REVISION}" -Dmaven.test.failure.ignore=true

echo "Generating coverage report..."

if [ ! -f "$CSV" ]; then
    echo "ERROR: JaCoCo CSV still not found after test run" >&2
    exit 1
fi

mkdir -p "$(dirname "$REPORT")"
python3 "scripts/coverage.py" "$CSV" "$REPORT"

if [ "$OPEN_REPORT" = true ]; then
    xdg-open "$REPORT" 2>/dev/null || open "$REPORT" 2>/dev/null || echo "Cannot open $REPORT" >&2
fi

echo "Coverage report generated at $REPORT."
