#!/bin/bash

IMAGE_NAME=gregordietrich/aimathtutor

. "$(dirname "$0")"/lib/get_dir.sh

prompt_yes_no() {
    local question="$1"
    local default_answer="$2"

    if [ -z "$question" ]; then
        question="Proceed"
    fi

    if [ -z "$default_answer" ]; then
        default_answer="n"
    fi

    case "$default_answer" in
        [yYjJ])
            question="${question}? [Y/n]: "
            ;;
        *)
            question="${question}? [y/N]: "
            ;;
    esac

    read -r -p "${question}" reply
    case "$reply" in
        [yYjJ])
            return 0
            ;;
        *)
            return 1
            ;;
    esac
}

set -e

read -p "Enter the new tag [1.0.0-SNAPSHOT]: " REVISION
REVISION=${REVISION:-1.0.0-SNAPSHOT}
TAG="${IMAGE_NAME}:${REVISION}"
export REVISION=${REVISION}

RUN_TESTS=false
if prompt_yes_no "Do you want to run tests" n; then
    RUN_TESTS=true
fi

cd "$DIR/../.."

git switch main

git pull

make clean

make install

if [ "$RUN_TESTS" = true ]; then
    make test
fi

make build

make tag

docker login

docker push "$TAG"-alpine
docker push "$TAG"-ubi
docker push "$TAG"

docker tag "$TAG"-alpine "$IMAGE_NAME":alpine
docker tag "$TAG"-ubi "$IMAGE_NAME":ubi
docker tag "$TAG" "$IMAGE_NAME":latest

docker push "$IMAGE_NAME":alpine
docker push "$IMAGE_NAME":ubi
docker push "$IMAGE_NAME":latest

cd - > /dev/null
