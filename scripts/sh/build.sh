#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

DOCKERFILE_ALPINE="src/main/docker/Dockerfile.alpine"
DOCKERFILE_UBI="src/main/docker/Dockerfile.ubi"
DOCKERFILE_UBUNTU="src/main/docker/Dockerfile.ubuntu"
PLATFORMS="linux/amd64,linux/arm64"

set -e

cd "$DIR/../.."

# Run environment check first
"$DIR/check.sh"

if [[ -z "$REVISION" ]]; then
    read -p "Enter the new tag [1.0.0-SNAPSHOT]: " REVISION
    REVISION=${REVISION:-1.0.0-SNAPSHOT}
fi

TAG="gregordietrich/aimathtutor:${REVISION}"

# Clean before building to avoid corrupted workspace files
${MVN_CMD} clean -Drevision=${REVISION}

${MVN_CMD} package -DskipTests -Pproduction -Drevision=${REVISION}

# Try to use buildx with the 'default' builder which typically uses the local docker driver
# This avoids starting a docker-container builder that may reference Docker Desktop/WSL bind mounts
if docker buildx inspect default >/dev/null 2>&1; then
	echo "Using buildx 'default' builder to build image. If you want to push multi-arch images, add --push."

	# Register QEMU binfmt handlers only for non-native target platforms,
	# and only if not already enabled, to minimize privileged side effects.
	_native_arch="$(uname -m)"
	_binfmt_install_targets=""
	for _platform in ${PLATFORMS//,/ }; do
		_arch="${_platform#linux/}"
		case "$_arch" in
			amd64) _native_equiv="x86_64";  _qemu_entry="qemu-x86_64"  ;;
			arm64) _native_equiv="aarch64"; _qemu_entry="qemu-aarch64" ;;
			*)     continue ;;
		esac
		[[ "$_native_arch" == "$_native_equiv" ]] && continue
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
		echo "QEMU binfmt handlers already registered; skipping installation."
	fi

    # Alpine-based image
	if docker buildx build --builder default --platform "$PLATFORMS" -t "$TAG"-alpine -f "$DOCKERFILE_ALPINE" .; then
		echo "buildx multi-platform build finished (results kept in buildx cache). Use --push to publish or --load for a single-platform image."
	else
		echo "buildx multi-platform build failed; attempting single-platform local build with --load for current arch."
		# Try loading a single-platform image into local docker (amd64). If that fails, fallback to plain docker build.
		if ! docker buildx build --load --platform linux/amd64 -t "$TAG"-alpine -f "$DOCKERFILE_ALPINE" .; then
			echo "--load build failed; falling back to plain 'docker build' (single-platform)."
			docker build -t "$TAG"-alpine -f "$DOCKERFILE_ALPINE" .
		fi
	fi

    # UBI-based image
    if docker buildx build --builder default --platform "$PLATFORMS" -t "$TAG"-ubi -f "$DOCKERFILE_UBI" .; then
		echo "buildx multi-platform build finished (results kept in buildx cache). Use --push to publish or --load for a single-platform image."
	else
		echo "buildx multi-platform build failed; attempting single-platform local build with --load for current arch."
		# Try loading a single-platform image into local docker (amd64). If that fails, fallback to plain docker build.
		if ! docker buildx build --load --platform linux/amd64 -t "$TAG"-ubi -f "$DOCKERFILE_UBI" .; then
			echo "--load build failed; falling back to plain 'docker build' (single-platform)."
			docker build -t "$TAG"-ubi -f "$DOCKERFILE_UBI" .
		fi
	fi

    # Ubuntu-based image
    if docker buildx build --builder default --platform "$PLATFORMS" -t "$TAG"-ubuntu -f "$DOCKERFILE_UBUNTU" .; then
		echo "buildx multi-platform build finished (results kept in buildx cache). Use --push to publish or --load for a single-platform image."
	else
		echo "buildx multi-platform build failed; attempting single-platform local build with --load for current arch."
		# Try loading a single-platform image into local docker (amd64). If that fails, fallback to plain docker build.
		if ! docker buildx build --load --platform linux/amd64 -t "$TAG"-ubuntu -f "$DOCKERFILE_UBUNTU" .; then
			echo "--load build failed; falling back to plain 'docker build' (single-platform)."
			docker build -t "$TAG"-ubuntu -f "$DOCKERFILE_UBUNTU" .
		fi
	fi
else
	echo "buildx 'default' builder not available; performing plain docker build (single-platform)."
	docker build -t "$TAG"-alpine -f "$DOCKERFILE_ALPINE" .
	docker build -t "$TAG"-ubi -f "$DOCKERFILE_UBI" .
	docker build -t "$TAG"-ubuntu -f "$DOCKERFILE_UBUNTU" .
fi

docker tag "$TAG"-ubuntu "$TAG"

cd - > /dev/null
