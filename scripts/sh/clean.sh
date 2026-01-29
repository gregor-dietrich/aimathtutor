#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

set -e

cd "$DIR/../.."

REVISION=${REVISION:-1.0.0-SNAPSHOT}

${MVN_CMD} clean -Drevision=${REVISION}

rm -rf logs
rm -rf node_modules
rm -rf src/main/frontend/generated
rm -rf target

cd - > /dev/null
