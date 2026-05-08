#!/bin/bash

IMAGE_NAME=gregordietrich/aimathtutor

. "$(dirname "$0")"/lib/get_dir.sh

set -e

echo "Starting release..."

read -p "Enter the new tag [1.0.0-SNAPSHOT]: " REVISION
REVISION=${REVISION:-1.0.0-SNAPSHOT}
TAG="${IMAGE_NAME}:${REVISION}"
export REVISION=${REVISION}

cd "$DIR/.."

git switch main

git pull

. scripts/clean.sh
. scripts/install.sh
. scripts/lint.sh
. scripts/test.sh
. scripts/build.sh
. scripts/tag.sh

docker login

docker push "$TAG"-alpine
docker push "$TAG"-ubuntu
docker push "$TAG"

docker tag "$TAG"-alpine "$IMAGE_NAME":alpine
docker tag "$TAG"-ubuntu "$IMAGE_NAME":ubuntu
docker tag "$TAG" "$IMAGE_NAME":latest

docker push "$IMAGE_NAME":alpine
docker push "$IMAGE_NAME":ubuntu
docker push "$IMAGE_NAME":latest

echo "Release completed."

cd - > /dev/null
