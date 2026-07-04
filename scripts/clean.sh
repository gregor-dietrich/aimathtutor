#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

set -e

cd "$DIR/.."

REVISION=${REVISION:-1.0.0-SNAPSHOT}

echo "Cleaning build artifacts..."

${MVN_CMD} -q clean -Drevision="${REVISION}"

rm -f package.json package-lock.json
rm -rf logs
rm -rf node_modules
rm -rf src/main/frontend/generated
rm -rf target

echo "Clean completed."

cd - > /dev/null
