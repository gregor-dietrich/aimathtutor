#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

set -e

cd "$DIR/../.."

make check

${MVN_CMD} clean install -DskipTests

cd - > /dev/null
