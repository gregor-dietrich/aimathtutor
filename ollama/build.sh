#!/bin/bash

set -e

DOCKERFILE="Dockerfile"

# Fetch the 5 latest tags from Ollama releases
echo "Fetching latest Ollama releases..."
TAGS=$(curl -s "https://api.github.com/repos/ollama/ollama/releases" | \
    grep -oP '"tag_name":\s*"\K[^"]+' | head -5)

if [ -z "$TAGS" ]; then
    echo "Error: Failed to fetch releases from GitHub API"
    exit 1
fi

# Find the first tag without a dash (stable release) as default
DEFAULT_TAG=""
for tag in $TAGS; do
    if [[ "$tag" != *-* ]]; then
        DEFAULT_TAG="$tag"
        break
    fi
done

# If no stable tag found, use the first one
if [ -z "$DEFAULT_TAG" ]; then
    DEFAULT_TAG=$(echo "$TAGS" | head -1)
fi

echo ""
echo "Available tags:"
echo "---------------"
i=1
for tag in $TAGS; do
    if [ "$tag" = "$DEFAULT_TAG" ]; then
        echo "  $i) $tag (default)"
    else
        echo "  $i) $tag"
    fi
    ((i++))
done
echo ""

read -p "Select tag to use [$DEFAULT_TAG]: " SELECTED_TAG
SELECTED_TAG="${SELECTED_TAG:-$DEFAULT_TAG}"

# Validate selection (could be a number or tag name)
if [[ "$SELECTED_TAG" =~ ^[1-5]$ ]]; then
    SELECTED_TAG=$(echo "$TAGS" | sed -n "${SELECTED_TAG}p")
fi

echo ""
echo "Using tag: $SELECTED_TAG"

# Fetch assets for the selected release
echo ""
echo "Fetching available Linux assets for $SELECTED_TAG..."
ASSETS=$(curl -s "https://api.github.com/repos/ollama/ollama/releases/tags/$SELECTED_TAG" | \
    grep -oP '"name":\s*"\K[^"]+' | grep -E '.*-linux-.*\.tgz$')

if [ -z "$ASSETS" ]; then
    echo "Error: No Linux .tgz assets found for release $SELECTED_TAG"
    exit 1
fi

echo ""
echo "Available Linux assets:"
echo "-----------------------"
i=1
DEFAULT_ASSET=""
DEFAULT_ASSET_NUM=""
for asset in $ASSETS; do
    # Default to amd64 without -rocm
    if [[ "$asset" == *amd64* ]] && [[ "$asset" != *-rocm* ]] && [ -z "$DEFAULT_ASSET" ]; then
        DEFAULT_ASSET="$asset"
        DEFAULT_ASSET_NUM="$i"
        echo "  $i) $asset (default)"
    else
        echo "  $i) $asset"
    fi
    ((i++))
done

# If no default found, use the first one
if [ -z "$DEFAULT_ASSET" ]; then
    DEFAULT_ASSET=$(echo "$ASSETS" | head -1)
    DEFAULT_ASSET_NUM="1"
fi

ASSET_COUNT=$((i - 1))
echo ""

read -p "Select asset to download [$DEFAULT_ASSET_NUM]: " SELECTED_ASSET_NUM
SELECTED_ASSET_NUM="${SELECTED_ASSET_NUM:-$DEFAULT_ASSET_NUM}"

# Validate selection
if [[ "$SELECTED_ASSET_NUM" =~ ^[0-9]+$ ]] && [ "$SELECTED_ASSET_NUM" -ge 1 ] && [ "$SELECTED_ASSET_NUM" -le "$ASSET_COUNT" ]; then
    SELECTED_ASSET=$(echo "$ASSETS" | sed -n "${SELECTED_ASSET_NUM}p")
else
    echo "Error: Invalid selection"
    exit 1
fi

echo ""
read -p "Push image to Docker Hub? [Y/n]: " PUSH_IMAGE
PUSH_IMAGE="${PUSH_IMAGE:-y}"

echo ""
echo "Downloading: $SELECTED_ASSET"

# Construct download URL and download the asset
DOWNLOAD_URL="https://github.com/ollama/ollama/releases/download/$SELECTED_TAG/$SELECTED_ASSET"
rm -rf payload && mkdir payload
curl -fsSL "$DOWNLOAD_URL" | tar zx -C payload

echo ""
echo "Download complete: $SELECTED_ASSET"

IMAGE_TAG="gregordietrich/ollama:${SELECTED_TAG#v}"
docker build -t "$IMAGE_TAG" -f "$DOCKERFILE" .

if [[ "$PUSH_IMAGE" =~ ^[Yy]$ ]]; then
    docker login
    docker push "$IMAGE_TAG"
    echo "Image pushed: $IMAGE_TAG"
else
    echo "Skipping push. Image available locally: $IMAGE_TAG"
fi
