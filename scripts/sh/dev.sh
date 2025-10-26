#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

set -e

cd "$DIR/../.."

${MVN_CMD} quarkus:dev

cd - > /dev/null
