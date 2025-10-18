#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh

set -e

cd "$DIR/../.."

read -p "Enter the target branch to create/reset: " TARGET_BRANCH

if [[ -z "$TARGET_BRANCH" ]]; then
    echo "No target branch entered. Exiting."
    exit 1
fi

read -p "Enter the source branch to create/reset ${TARGET_BRANCH} from/to [origin/main]: " SOURCE_BRANCH
SOURCE_BRANCH=${SOURCE_BRANCH:-origin/main}

if [[ -z "$SOURCE_BRANCH" ]]; then
    echo "No source branch entered. Exiting."
    exit 2
fi

git fetch
if [ $? -ne 0 ]; then
    echo "Git fetch failed. Exiting."
    exit 3
fi

if git show-ref --verify --quiet "refs/heads/${TARGET_BRANCH}" || git show-ref --verify --quiet "refs/remotes/origin/${TARGET_BRANCH}"; then
    git checkout "${TARGET_BRANCH}"
    if [ $? -ne 0 ]; then
        echo "Failed to checkout ${TARGET_BRANCH} branch. Exiting."
        exit 4
    fi

    git reset --hard "${SOURCE_BRANCH}"
    if [ $? -ne 0 ]; then
        echo "Git reset failed. Exiting."
        exit 5
    fi
else
    git checkout -b "${TARGET_BRANCH}" "${SOURCE_BRANCH}"
    if [ $? -ne 0 ]; then
        echo "Failed to create ${TARGET_BRANCH} branch. Exiting."
        exit 6
    fi
fi

if git ls-remote --exit-code --heads origin "${TARGET_BRANCH}" >/dev/null 2>&1; then
    git push --force-with-lease origin "${TARGET_BRANCH}"
else
    git push -u origin "${TARGET_BRANCH}"
fi

if [ $? -ne 0 ]; then
    echo "Failed to push ${TARGET_BRANCH} branch. Exiting."
    exit 7
fi

cd - > /dev/null
