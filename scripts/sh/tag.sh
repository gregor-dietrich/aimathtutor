#!/bin/bash

set -e

cd "$(dirname "$0")"/../..

git fetch

LATEST_TAG=$(git tag --sort=-version:refname | head -n1)
LATEST_TAG=${LATEST_TAG:-1.0.0}

if [[ "$LATEST_TAG" =~ ^([0-9]+(\.[0-9]+)*)(.*)$ ]]; then
    BASE="${BASH_REMATCH[1]}"
    SUFFIX="${BASH_REMATCH[3]}"

    IFS='.' read -r -a parts <<< "$BASE"
    last_index=$(( ${#parts[@]} - 1 ))
    last_value=${parts[$last_index]}

    if [[ "$last_value" =~ ^[0-9]+$ ]]; then
        parts[$last_index]=$(( last_value + 1 ))
        NEW_BASE="${parts[0]}"
        for ((i=1;i<=last_index;i++)); do
            NEW_BASE+=".${parts[i]}"
        done
        LATEST_VERSION="${NEW_BASE}${SUFFIX}"
    else
        # Fallback: if last component isn't purely numeric, append .1
        LATEST_VERSION="${BASE}.1${SUFFIX}"
    fi
else
    LATEST_VERSION="$LATEST_TAG"
fi

read -p "Enter the new tag [${LATEST_VERSION}]: " VERSION
VERSION=${VERSION:-$LATEST_VERSION}
 
if [[ -z "$VERSION" ]]; then
    echo "Tag is required. Exiting."
    exit 1
fi

echo "Creating and pushing tag ${VERSION}..."

git tag -a -s ${VERSION} -m "${VERSION}"
if [ $? -ne 0 ]; then
    echo "Git tag creation failed. Exiting."
    exit 2
fi

git push origin ${VERSION}
if [ $? -ne 0 ]; then
    echo "Failed to push tag. Exiting."
    exit 3
fi

cd - > /dev/null
