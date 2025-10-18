#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

REQUIRED_JDK_VERSION="21"
REQUIRED_MAVEN_VERSION="3.9.9"

set -e

cd "$DIR/../.."

echo "Checking version of $(which java)..."

if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed or not in PATH. Exiting."
    echo "Please install JDK version ${REQUIRED_JDK_VERSION} or higher and add it to PATH."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')
if [[ -z "$JAVA_VERSION" ]]; then
    echo "ERROR: Could not determine Java version. Exiting."
    exit 2
fi
echo "Detected Java version: ${JAVA_VERSION}"

if [[ $JAVA_VERSION =~ ^1\.([0-9]+) ]]; then
    JAVA_MAJOR_VERSION=${BASH_REMATCH[1]}
elif [[ $JAVA_VERSION =~ ^([0-9]+) ]]; then
    JAVA_MAJOR_VERSION=${BASH_REMATCH[1]}
else
    echo "ERROR: Could not parse Java version format. Exiting."
    exit 3
fi

if [[ $JAVA_MAJOR_VERSION -lt $REQUIRED_JDK_VERSION ]]; then
    echo "ERROR: Java version ${JAVA_VERSION} (major version ${JAVA_MAJOR_VERSION}) is below the required JDK ${REQUIRED_JDK_VERSION}. Exiting."
    echo "Please upgrade to JDK ${REQUIRED_JDK_VERSION} or higher and add it to PATH."
    exit 4
fi
echo "JDK version check passed. (>= ${REQUIRED_JDK_VERSION})"

echo "Checking version of $(which ${MVN_CMD})..."

if ! command -v ${MVN_CMD} &> /dev/null; then
    echo "ERROR: Maven not found. Exiting."
    echo "Please install Maven version ${REQUIRED_MAVEN_VERSION} or higher and add it to PATH."
    exit 5
fi
MAVEN_VERSION=$(${MVN_CMD} -version | head -n 1 | awk '{print $3}')
if [[ -z "$MAVEN_VERSION" ]]; then
    echo "ERROR: Could not determine Maven version. Exiting."
    exit 6
fi
echo "Detected Maven version: ${MAVEN_VERSION}"

printf -v versions '%s\n%s' "$REQUIRED_MAVEN_VERSION" "$MAVEN_VERSION"
if [[ $versions != "$(sort -V <<< "$versions")" ]]; then
    echo "ERROR: Maven version ${MAVEN_VERSION} is below the required version ${REQUIRED_MAVEN_VERSION}. Exiting."
    echo "Please upgrade Maven to version ${REQUIRED_MAVEN_VERSION} or higher and add it to PATH."
    exit 7
fi
echo "Maven version check passed. (>= ${REQUIRED_MAVEN_VERSION})"

cd - > /dev/null
