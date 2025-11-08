#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh

# Allow tag to be set via first argument or TAG env var, fallback to default
TAG="${1:-${TAG:-gregordietrich/aimathtutor:1.2.0-SNAPSHOT}}"

set -e

cd "$DIR/../.."

git switch main

git pull

make build

make tag

docker push "$TAG"

cd - > /dev/null
