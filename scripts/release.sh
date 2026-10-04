#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"
. "$DIR/lib/images.sh"

set -e

echo "Starting release..."

if [[ -z "$REVISION" ]]; then
    read -r -p "Enter the new tag [1.0.0-SNAPSHOT]: " REVISION
    REVISION=${REVISION:-1.0.0-SNAPSHOT}
fi
export REVISION
[[ $REVISION =~ ^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$ ]] \
    || { echo "Error: '$REVISION' is not a valid image tag; nothing was tagged or pushed." >&2; exit 1; }
TAG="${IMAGE_NAME}:${REVISION}"

cd "$DIR/.."

if [[ -z "${RELEASE_REEXEC:-}" ]]; then
    # A host that cannot build and push every platform fails here, before the tests, the tag or any push.
    register_qemu
    require_multiplatform_builder || { echo "Aborting the release; nothing was tagged or pushed." >&2; exit 1; }
    docker login

    git switch main
    git pull
    # Run the release with the scripts just pulled, not the ones this run started from
    RELEASE_REEXEC=1 exec scripts/release.sh
fi

. scripts/clean.sh
. scripts/install.sh
. scripts/lint.sh
. scripts/test.sh
package_app
. scripts/tag.sh

# buildx builds every platform and pushes all of an image's tags at once; nothing comes from the local
# image store. A failure stops the release, but an image pushed before it stays published.
push_image . "$DOCKERFILE_ALPINE" "$TAG"-alpine "$IMAGE_NAME":alpine "$TAG" "$IMAGE_NAME":latest
push_image . "$DOCKERFILE_UBUNTU" "$TAG"-ubuntu "$IMAGE_NAME":ubuntu

echo "Release completed."

cd - > /dev/null
