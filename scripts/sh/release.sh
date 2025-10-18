#!/bin/bash

TAG="gregordietrich/aimathtutor:1.0.0-SNAPSHOT"

set -e

cd "$(dirname "$0")"/../..

make build

docker push "$TAG"

cd - > /dev/null
