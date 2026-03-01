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

	# Register QEMU binfmt handlers to enable cross-platform emulation (required for linux/arm64 on amd64 hosts)
	if docker run --privileged --rm tonistiigi/binfmt --install all >/dev/null 2>&1; then
		echo "QEMU binfmt handlers installed for multi-platform builds."
	else
		echo "Warning: failed to install QEMU binfmt handlers; linux/arm64 builds may fail on this host."
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
