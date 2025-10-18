#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh

# Allow tag to be set via first argument or TAG env var, fallback to default
TAG="${1:-${TAG:-gregordietrich/aimathtutor:1.0.0-SNAPSHOT}}"

set -e

cd "$DIR/../.."

make build

docker push "$TAG"

cd - > /dev/null
