#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

TAG="gregordietrich/aimathtutor:1.1.0-SNAPSHOT"
DOCKERFILE="src/main/docker/Dockerfile.ubi"
PLATFORMS="linux/amd64,linux/arm64"

prompt_yes_no() {
    local question="$1"
    local default_answer="$2"

    if [ -z "$question" ]; then
        question="Proceed"
    fi

    if [ -z "$default_answer" ]; then
        default_answer="n"
    fi

    case "$default_answer" in
        [yYjJ])
            question="${question}? [Y/n]: "
            ;;
        *)
            question="${question}? [y/N]: "
            ;;
    esac

    read -r -p "${question}" reply
    case "$reply" in
        [yYjJ])
            return 0
            ;;
        *)
            return 1
            ;;
    esac
}

set -e

cd "$DIR/../.."

if prompt_yes_no "Do you want to run tests" n; then
	make test
fi

${MVN_CMD} package -DskipTests -Pproduction

# Try to use buildx with the 'default' builder which typically uses the local docker driver
# This avoids starting a docker-container builder that may reference Docker Desktop/WSL bind mounts
if docker buildx inspect default >/dev/null 2>&1; then
	echo "Using buildx 'default' builder to build image. If you want to push multi-arch images, add --push."
	if docker buildx build --builder default --platform "$PLATFORMS" -t "$TAG" -f "$DOCKERFILE" .; then
		echo "buildx multi-platform build finished (results kept in buildx cache). Use --push to publish or --load for a single-platform image."
	else
		echo "buildx multi-platform build failed; attempting single-platform local build with --load for current arch."
		# Try loading a single-platform image into local docker (amd64). If that fails, fallback to plain docker build.
		if ! docker buildx build --load --platform linux/amd64 -t "$TAG" -f "$DOCKERFILE" .; then
			echo "--load build failed; falling back to plain 'docker build' (single-platform)."
			docker build -t "$TAG" -f "$DOCKERFILE" .
		fi
	fi
else
	echo "buildx 'default' builder not available; performing plain docker build (single-platform)."
	docker build -t "$TAG" -f "$DOCKERFILE" .
fi

cd - > /dev/null
