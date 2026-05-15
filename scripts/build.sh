#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

DOCKERFILE_ALPINE="src/main/docker/Dockerfile.alpine"
DOCKERFILE_UBUNTU="src/main/docker/Dockerfile.ubuntu"
PLATFORMS="linux/amd64,linux/arm64"

# Detect native platform
_native_arch_raw="$(uname -m)"
if [[ "$_native_arch_raw" == "x86_64" ]]; then
    NATIVE_PLATFORM="linux/amd64"
elif [[ "$_native_arch_raw" == "aarch64" || "$_native_arch_raw" == "arm64" ]]; then
    NATIVE_PLATFORM="linux/arm64"
else
    NATIVE_PLATFORM="linux/amd64"
fi

set -e

cd "$DIR/.."

echo "Starting build..."

# Run environment check first
"$DIR/check.sh"

# Register QEMU binfmt handlers only for non-native target platforms
if docker buildx version >/dev/null 2>&1; then
    _binfmt_install_targets=""
    for _platform in ${PLATFORMS//,/ }; do
        _arch="${_platform#linux/}"
        case "$_arch" in
            amd64) _qemu_entry="qemu-x86_64"  ;;
            arm64) _qemu_entry="qemu-aarch64" ;;
            *)     continue ;;
        esac
        [[ "$NATIVE_PLATFORM" == "$_platform" ]] && continue
        grep -q "enabled" "/proc/sys/fs/binfmt_misc/${_qemu_entry}" 2>/dev/null && continue
        _binfmt_install_targets="${_binfmt_install_targets} ${_arch}"
    done
    _binfmt_install_targets="${_binfmt_install_targets# }"

    if [[ -n "$_binfmt_install_targets" ]]; then
        echo "Registering QEMU binfmt handlers for: ${_binfmt_install_targets}"
        # Image is pinned to a specific digest to prevent supply chain attacks from a mutable tag.
        if docker run --privileged --rm \
            tonistiigi/binfmt:qemu-v10.2.1@sha256:d3b963f787999e6c0219a48dba02978769286ff61a5f4d26245cb6a6e5567ea3 \
            --install "${_binfmt_install_targets}" >/dev/null 2>&1; then
            echo "QEMU binfmt handlers installed."
        else
            echo "Warning: failed to install QEMU binfmt handlers; ${_binfmt_install_targets} builds may fail on this host."
        fi
    else
        echo "QEMU binfmt handlers already registered or not required."
    fi
fi

if [[ -z "$REVISION" ]]; then
    read -p "Enter the new tag [1.0.0-SNAPSHOT]: " REVISION
    REVISION=${REVISION:-1.0.0-SNAPSHOT}
fi

TAG="gregordietrich/aimathtutor:${REVISION}"

# Clean before building to avoid corrupted workspace files
${MVN_CMD} -q clean -Drevision="${REVISION}"

${MVN_CMD} -q package -DskipTests -Pproduction -Drevision="${REVISION}"

# Alpine-based image
if docker buildx version >/dev/null 2>&1; then
    echo "Using buildx to build Alpine image. If you want to push multi-arch images, add --push."
    if docker buildx build --platform "$PLATFORMS" -t "$TAG"-alpine -f "$DOCKERFILE_ALPINE" .; then
        echo "buildx multi-platform build finished (results kept in buildx cache). Use --push to publish or --load for a single-platform image."
    else
        echo "buildx multi-platform build failed; attempting single-platform local build with --load for current arch ($NATIVE_PLATFORM)."
        if ! docker buildx build --load --platform "$NATIVE_PLATFORM" -t "$TAG"-alpine -f "$DOCKERFILE_ALPINE" .; then
            echo "--load build failed; falling back to plain 'docker build' (single-platform)."
            docker build -t "$TAG"-alpine -f "$DOCKERFILE_ALPINE" .
        fi
    fi
else
    echo "buildx not available; performing plain docker build (single-platform)."
    docker build -t "$TAG"-alpine -f "$DOCKERFILE_ALPINE" .
fi

# Ubuntu-based image
if docker buildx version >/dev/null 2>&1; then
    echo "Using buildx to build Ubuntu image. If you want to push multi-arch images, add --push."
    if docker buildx build --platform "$PLATFORMS" -t "$TAG"-ubuntu -f "$DOCKERFILE_UBUNTU" .; then
        echo "buildx multi-platform build finished (results kept in buildx cache). Use --push to publish or --load for a single-platform image."
    else
        echo "buildx multi-platform build failed; attempting single-platform local build with --load for current arch ($NATIVE_PLATFORM)."
        if ! docker buildx build --load --platform "$NATIVE_PLATFORM" -t "$TAG"-ubuntu -f "$DOCKERFILE_UBUNTU" .; then
            echo "--load build failed; falling back to plain 'docker build' (single-platform)."
            docker build -t "$TAG"-ubuntu -f "$DOCKERFILE_UBUNTU" .
        fi
    fi
else
    docker build -t "$TAG"-ubuntu -f "$DOCKERFILE_UBUNTU" .
fi

docker tag "$TAG"-alpine "$TAG"

echo "Build completed."

cd - > /dev/null
