#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh

set -e

cd "$DIR/../.."

git fetch

LATEST_TAG=$(git tag --sort=-version:refname | head -n1)
LATEST_VERSION=${LATEST_TAG:-1.0.0-SNAPSHOT}

read -p "Enter the tag to delete [${LATEST_VERSION}]: " VERSION
VERSION=${VERSION:-$LATEST_VERSION}

if [[ -z "$VERSION" ]]; then
    echo "Tag is required. Exiting."
    exit 1
fi

echo "Deleting tag ${VERSION}..."

if git tag -l | grep -q "^${VERSION}$"; then
    git tag -d "$VERSION"
    echo "Tag ${VERSION} deleted locally."
else
    echo "Tag ${VERSION} not found locally."
fi

if git ls-remote --tags origin | grep -q "refs/tags/${VERSION}$"; then
    git push origin ":refs/tags/${VERSION}"
    echo "Tag ${VERSION} deleted from remote."
else
    echo "Tag ${VERSION} not found on remote."
fi

cd - > /dev/null
