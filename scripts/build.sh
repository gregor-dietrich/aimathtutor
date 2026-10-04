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

TAG="${IMAGE_NAME}:${REVISION}"

package_app

# build_local_image <dockerfile> <tag> [<tag>...]: build for the native platform into the local image
# store; `make release` builds and pushes the multi-platform images instead.
build_local_image() {
    local dockerfile=$1 tag tag_args=()
    shift
    for tag in "$@"; do tag_args+=(-t "$tag"); done
    if docker buildx version >/dev/null 2>&1; then
        docker buildx build --load --platform "$NATIVE_PLATFORM" "${tag_args[@]}" -f "$dockerfile" .
    else
        echo "buildx not available; performing plain docker build."
        docker build "${tag_args[@]}" -f "$dockerfile" .
    fi
}

build_local_image "$DOCKERFILE_ALPINE" "$TAG"-alpine "$TAG"
build_local_image "$DOCKERFILE_UBUNTU" "$TAG"-ubuntu

echo "Build completed."

cd - > /dev/null
