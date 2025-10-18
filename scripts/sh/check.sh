#!/bin/bash

REQUIRED_JDK_VERSION="21"
REQUIRED_MAVEN_VERSION="3.9.9"

set -e

cd "$(dirname "$0")"/../..

echo "Checking JDK version..."

if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed or not in PATH. Exiting."
    echo "Please install JDK version ${REQUIRED_JDK_VERSION} or higher and add it to PATH."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')
if [[ -z "$JAVA_VERSION" ]]; then
    echo "ERROR: Could not determine Java version. Exiting."
    exit 1
fi
echo "Detected Java version: ${JAVA_VERSION}"

if [[ $JAVA_VERSION =~ ^1\.([0-9]+) ]]; then
    JAVA_MAJOR_VERSION=${BASH_REMATCH[1]}
elif [[ $JAVA_VERSION =~ ^([0-9]+) ]]; then
    JAVA_MAJOR_VERSION=${BASH_REMATCH[1]}
else
    echo "ERROR: Could not parse Java version format. Exiting."
    exit 1
fi

if [[ $JAVA_MAJOR_VERSION -lt $REQUIRED_JDK_VERSION ]]; then
    echo "ERROR: Java version ${JAVA_VERSION} (major version ${JAVA_MAJOR_VERSION}) is below the required JDK ${REQUIRED_JDK_VERSION}. Exiting."
    echo "Please upgrade to JDK ${REQUIRED_JDK_VERSION} or higher and add it to PATH."
    exit 1
fi
echo "JDK version check passed."

echo "Checking Maven version..."

if ! command -v ./mvnw &> /dev/null; then
    echo "ERROR: Maven Wrapper not found. Exiting."
    echo "Please install Maven version ${REQUIRED_MAVEN_VERSION} or higher and add it to PATH."
    exit 1
fi
MAVEN_VERSION=$(./mvnw -version | head -n 1 | awk '{print $3}')
if [[ -z "$MAVEN_VERSION" ]]; then
    echo "ERROR: Could not determine Maven version. Exiting."
    exit 1
fi
echo "Detected Maven version: ${MAVEN_VERSION}"

printf -v versions '%s\n%s' "$REQUIRED_MAVEN_VERSION" "$MAVEN_VERSION"
if [[ $versions != "$(sort -V <<< "$versions")" ]]; then
    echo "ERROR: Maven version ${MAVEN_VERSION} is below the required version ${REQUIRED_MAVEN_VERSION}. Exiting."
    echo "Please upgrade Maven to version ${REQUIRED_MAVEN_VERSION} or higher and add it to PATH."
    exit 1
fi
echo "Maven version check passed."

cd - > /dev/null
