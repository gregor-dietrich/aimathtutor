#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

set -e

cd "$DIR/../.."

make install

${MVN_CMD} test

cd - > /dev/null
