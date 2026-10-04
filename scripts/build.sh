#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"
. "$DIR/lib/images.sh"

set -e

cd "$DIR/.."

echo "Starting build..."

# Run environment check first
"$DIR/check.sh"

if [[ -z "$REVISION" ]]; then
    read -r -p "Enter the new tag [1.0.0-SNAPSHOT]: " REVISION
    REVISION=${REVISION:-1.0.0-SNAPSHOT}
fi
require_valid_revision "$REVISION" || exit 1

TAG="${IMAGE_NAME}:${REVISION}"

package_app

# Images for the local image store only; `make release` builds and pushes the multi-platform images
build_local_image "$DOCKERFILE_ALPINE" "$TAG"-alpine "$TAG"
build_local_image "$DOCKERFILE_UBUNTU" "$TAG"-ubuntu

echo "Build completed."

cd - > /dev/null
