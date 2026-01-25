#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh

# Allow tag to be set via first argument or TAG env var, fallback to default
TAG="${1:-${TAG:-gregordietrich/aimathtutor:2.2.0-SNAPSHOT}}"

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

docker push "$TAG"
docker push "$TAG"-alpine
docker push "$TAG"-ubi

cd - > /dev/null
