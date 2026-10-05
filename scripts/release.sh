#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"
. "$DIR/lib/images.sh"

set -e

abort_untagged() {
    echo "Aborting the release; nothing was tagged or pushed." >&2
    exit 1
}

echo "Starting release..."

if [[ -z "$REVISION" ]]; then
    read -r -p "Enter the version to release: " REVISION
fi
export REVISION
require_valid_revision "$REVISION" || abort_untagged
[[ $REVISION != *-SNAPSHOT ]] || { echo "Error: a release needs a real version, not '$REVISION'." >&2; abort_untagged; }
TAG="${IMAGE_NAME}:${REVISION}"

cd "$DIR/.."

# --pulled is private to this script: the run below, with the scripts just pulled
if [[ "$1" != --pulled ]]; then
    git switch main
    git pull
    exec scripts/release.sh --pulled
fi

# A host that cannot build and push every platform fails here, before the tests, the tag or any push.
register_qemu
require_multiplatform_builder || abort_untagged
docker login || abort_untagged

. scripts/clean.sh
. scripts/install.sh
. scripts/lint.sh
. scripts/test.sh
package_app

# Build every platform into the buildx cache before the git tag, so only a push can fail after it.
prebuild_image "$DOCKERFILE_ALPINE"
prebuild_image "$DOCKERFILE_UBUNTU"

. scripts/tag.sh

# buildx pushes every platform under all of an image's tags at once, reusing the cached build; nothing
# comes from the local image store. A failure stops the release, but an image pushed before it stays published.
push_image "$DOCKERFILE_ALPINE" "$TAG"-alpine "$IMAGE_NAME":alpine "$TAG" "$IMAGE_NAME":latest
push_image "$DOCKERFILE_UBUNTU" "$TAG"-ubuntu "$IMAGE_NAME":ubuntu

echo "Release completed."

cd - > /dev/null
