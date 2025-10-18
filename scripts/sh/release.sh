#!/bin/bash

# Allow tag to be set via first argument or TAG env var, fallback to default
TAG="${1:-${TAG:-gregordietrich/aimathtutor:1.0.0-SNAPSHOT}}"

set -e

cd "$(dirname "$0")"/../..

make build

docker push "$TAG"

cd - > /dev/null
