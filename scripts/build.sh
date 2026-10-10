#!/bin/bash

# Run through make, which exports PROJECT_ROOT and DEVKIT (devkit's make/common.mk).
cd "${PROJECT_ROOT:?run this through make}" || exit
. "${DEVKIT:?run this through make}/scripts/lib/get_maven.sh"
. scripts/lib/images.sh || exit

set -e

echo "Starting build..."

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
