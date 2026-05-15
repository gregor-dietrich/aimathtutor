#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

cd "$DIR/.."

OPEN_REPORT=false
while getopts "o" opt; do
    case "$opt" in
        o) OPEN_REPORT=true ;;
    esac
done

set -e

REVISION=${REVISION:-1.0.0-SNAPSHOT}
CSV="target/site/jacoco/jacoco.csv"
REPORT=".coverage.md"

echo "Running tests with JaCoCo coverage..."

maven_status=0
${MVN_CMD} -q verify -Dquarkus.log.console.enabled=false -Dquarkus.log.file.enabled=false -Drevision="${REVISION}" -Dmaven.test.failure.ignore=true || maven_status=$?

echo "Generating coverage report..."

if [ ! -f "$CSV" ]; then
    echo "ERROR: JaCoCo CSV not found after test run" >&2
    exit 1
fi

python3 "scripts/coverage.py" "$REPORT" "$CSV"

if [ "$OPEN_REPORT" = true ]; then
    xdg-open "$REPORT" 2>/dev/null || open "$REPORT" 2>/dev/null || echo "Cannot open $REPORT" >&2
fi

echo "Coverage report generated at $REPORT."

exit $maven_status
