#!/bin/bash

# Image building shared by build.sh (`make build`: images in the local image store) and release.sh
# (`make release`: multi-platform images that buildx pushes itself). Source it after get_maven.sh and
# call its functions from the repository root.

# shellcheck disable=SC2034 # read by build.sh and release.sh
IMAGE_NAME="gregordietrich/aimathtutor"
# shellcheck disable=SC2034 # read by build.sh and release.sh
DOCKERFILE_ALPINE="src/main/docker/Dockerfile.alpine"
# shellcheck disable=SC2034 # read by build.sh and release.sh
DOCKERFILE_UBUNTU="src/main/docker/Dockerfile.ubuntu"
PLATFORMS="linux/amd64,linux/arm64"

case "$(uname -m)" in
    aarch64 | arm64) NATIVE_PLATFORM="linux/arm64" ;;
    *) NATIVE_PLATFORM="linux/amd64" ;; # x86_64, and the fallback for anything else
esac

# Register QEMU binfmt handlers only for non-native target platforms
register_qemu() {
    docker buildx version >/dev/null 2>&1 || return 0
    local platform arch qemu_entry targets=""
    for platform in ${PLATFORMS//,/ }; do
        arch="${platform#linux/}"
        case "$arch" in
            amd64) qemu_entry="qemu-x86_64"  ;;
            arm64) qemu_entry="qemu-aarch64" ;;
            *)     continue ;;
        esac
        [[ "$NATIVE_PLATFORM" == "$platform" ]] && continue
        grep -q "enabled" "/proc/sys/fs/binfmt_misc/${qemu_entry}" 2>/dev/null && continue
        targets="${targets} ${arch}"
    done
    targets="${targets# }"

    if [[ -n "$targets" ]]; then
        echo "Registering QEMU binfmt handlers for: ${targets}"
        # Image is pinned to a specific digest to prevent supply chain attacks from a mutable tag.
        if docker run --privileged --rm \
            tonistiigi/binfmt:qemu-v10.2.1@sha256:d3b963f787999e6c0219a48dba02978769286ff61a5f4d26245cb6a6e5567ea3 \
            --install "${targets// /,}" >/dev/null 2>&1; then
            echo "QEMU binfmt handlers installed."
        else
            echo "Warning: failed to install QEMU binfmt handlers; ${targets} builds may fail on this host."
        fi
    else
        echo "QEMU binfmt handlers already registered or not required."
    fi
}

# Package the application as version $REVISION: the target/quarkus-app the Dockerfiles copy.
package_app() {
    # Clean before building to avoid corrupted workspace files
    ${MVN_CMD} -q clean -Drevision="${REVISION}"
    # Vaadin reuses prod.bundle and node_modules while package.json is unchanged,
    # ignoring lockfile-only changes; remove both so npm installs from the lockfile
    rm -rf src/main/bundles/prod.bundle node_modules
    ${MVN_CMD} -q package -DskipTests -Pproduction -Drevision="${REVISION}"
}

# Fail unless the current buildx builder can build every platform in $PLATFORMS in one build and push
# the result. Buildkit lists the platforms it can run (QEMU included), but the docker driver builds
# several at once only on the containerd image store; on the classic store buildx refuses.
require_multiplatform_builder() {
    local inspect driver platforms platform store
    inspect=$(docker buildx inspect --bootstrap 2>&1) \
        || { echo "Error: no usable buildx builder: $inspect" >&2; return 1; }
    driver=$(sed -n 's/^Driver: *//p' <<< "$inspect")
    platforms=$(sed -n 's/^Platforms: *//p' <<< "$inspect" | tr -d ' *' | paste -sd, -)
    for platform in ${PLATFORMS//,/ }; do
        [[ ",$platforms," == *",$platform,"* ]] || {
            echo "Error: the buildx builder (${driver} driver) cannot build $platform; it lists: ${platforms}." >&2
            return 1
        }
    done
    [[ "$driver" == docker ]] || return 0
    store=$(docker info -f '{{.DriverStatus}}') || { echo "Error: docker info failed." >&2; return 1; }
    if [[ "$store" != *io.containerd.snapshotter* ]]; then
        echo "Error: a docker-driver buildx builder cannot build several platforms on the classic image store." \
            "Enable the containerd image store, or select a docker-container builder" \
            "(docker buildx create --use --driver docker-container)." >&2
        return 1
    fi
}

# push_image <context_dir> <dockerfile> <tag> [<tag>...]: build for every platform in $PLATFORMS and
# push under every tag, straight from buildx. Nothing goes through the local image store, so neither a
# stale nor a single-platform image can be published.
push_image() {
    (( $# >= 3 )) || { echo "usage: push_image <context_dir> <dockerfile> <tag> [<tag>...]" >&2; return 2; }
    local context=$1 dockerfile=$2 tag tag_args=()
    shift 2
    for tag in "$@"; do tag_args+=(-t "$tag"); done
    echo "Building and pushing $*..."
    docker buildx build --platform "$PLATFORMS" --push "${tag_args[@]}" -f "$dockerfile" "$context"
}
