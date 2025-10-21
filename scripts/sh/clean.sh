#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

set -e

cd "$DIR/../.."

${MVN_CMD} clean

rm -rf logs
rm -rf node_modules
rm -rf src/main/frontend/generated
rm -rf target

cd - > /dev/null
