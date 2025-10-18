#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh

REQUIRED_MAVEN_VERSION="3.9.9"

if ! command -v mvn &> /dev/null; then
    MVN_CMD="./mvnw"
else
    MVN_CMD="mvn"
fi

cd "$DIR/../.."

if ! command -v ${MVN_CMD} &> /dev/null; then
    echo "ERROR: Maven not found. Exiting."
    echo "Please install Maven version ${REQUIRED_MAVEN_VERSION} or higher and add it to PATH."
    exit 1
fi
MAVEN_VERSION=$(${MVN_CMD} -version | head -n 1 | awk '{print $3}')
if [[ -z "$MAVEN_VERSION" ]]; then
    echo "ERROR: Could not determine Maven version. Exiting."
    exit 2
fi

printf -v versions '%s\n%s' "$REQUIRED_MAVEN_VERSION" "$MAVEN_VERSION"
if [[ $versions != "$(sort -V <<< "$versions")" ]]; then
    if [[ "${MVN_CMD}" != "./mvnw" && -x "./mvnw" ]]; then
        MVN_CMD="./mvnw"
        MAVEN_VERSION=$(${MVN_CMD} -version 2>/dev/null | head -n 1 | awk '{print $3}')
        if [[ -z "$MAVEN_VERSION" ]]; then
            echo "ERROR: Could not determine Maven version using ${MVN_CMD}. Exiting."
            exit 3
        fi
        printf -v versions '%s\n%s' "$REQUIRED_MAVEN_VERSION" "$MAVEN_VERSION"
        if [[ $versions != "$(sort -V <<< "$versions")" ]]; then
            echo "ERROR: Maven version ${MAVEN_VERSION} (from ${MVN_CMD}) is below required ${REQUIRED_MAVEN_VERSION}. Exiting."
            echo "Please upgrade Maven to version ${REQUIRED_MAVEN_VERSION} or update the project wrapper './mvnw'."
            exit 4
        fi
    else
        echo "ERROR: Maven version ${MAVEN_VERSION} is below the required version ${REQUIRED_MAVEN_VERSION}. Exiting."
        echo "Please upgrade Maven to version ${REQUIRED_MAVEN_VERSION} or higher and add it to PATH."
        exit 5
    fi
fi

cd - > /dev/null
