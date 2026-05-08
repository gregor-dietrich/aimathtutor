#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

set -e

cd "$DIR/.."

echo "Starting Quarkus in dev mode..."

${MVN_CMD} -q quarkus:dev

cd - > /dev/null
