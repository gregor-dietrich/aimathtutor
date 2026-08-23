#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

REQUIRED_JDK_VERSION="25"
REQUIRED_MAVEN_VERSION="3.9.9"

set -e

echo "Running environment checks..."

cd "$DIR/.."

echo "Checking version of $(which java)..."

if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed or not in PATH. Exiting."
    echo "Please install JDK version ${REQUIRED_JDK_VERSION} and add it to PATH."
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

if [[ $JAVA_MAJOR_VERSION -ne $REQUIRED_JDK_VERSION ]]; then
    echo "ERROR: Java version ${JAVA_VERSION} on PATH is not the required JDK ${REQUIRED_JDK_VERSION}. Exiting."
    echo "Please install JDK ${REQUIRED_JDK_VERSION} and make it the default on PATH."
    exit 4
fi
echo "JDK version check passed. (== ${REQUIRED_JDK_VERSION})"

echo "Checking version of $(which ${MVN_CMD})..."

if ! command -v ${MVN_CMD} &> /dev/null; then
    echo "ERROR: Maven not found. Exiting."
    echo "Please install Maven version ${REQUIRED_MAVEN_VERSION} or higher and add it to PATH."
    exit 5
fi

if ! MVN_VERSION_OUTPUT=$(${MVN_CMD} -version); then
    echo "ERROR: '${MVN_CMD} -version' failed. Exiting."
    exit 6
fi

MAVEN_VERSION=$(head -n 1 <<< "${MVN_VERSION_OUTPUT}" | awk '{print $3}')
if [[ -z "$MAVEN_VERSION" ]]; then
    echo "ERROR: Could not determine Maven version. Exiting."
    exit 7
fi
echo "Detected Maven version: ${MAVEN_VERSION}"

printf -v versions '%s\n%s' "$REQUIRED_MAVEN_VERSION" "$MAVEN_VERSION"
if [[ $versions != "$(sort -V <<< "$versions")" ]]; then
    echo "ERROR: Maven version ${MAVEN_VERSION} is below the required version ${REQUIRED_MAVEN_VERSION}. Exiting."
    echo "Please upgrade Maven to version ${REQUIRED_MAVEN_VERSION} or higher and add it to PATH."
    exit 8
fi
echo "Maven version check passed. (>= ${REQUIRED_MAVEN_VERSION})"

# The build runs through Maven, whose JDK can differ from `java` on PATH (e.g.
# Homebrew's mvn wrapper picks its own bundled JDK when JAVA_HOME is unset).
# The build tooling (PMD, Error Prone, ...) is only guaranteed to work on the
# pinned JDK, so the JVM Maven actually runs on must match it exactly too.
MVN_JAVA_VERSION=$(awk '/^Java version:/ {print $3}' <<< "${MVN_VERSION_OUTPUT}" | tr -d ',')
if [[ -z "$MVN_JAVA_VERSION" ]]; then
    echo "ERROR: Could not determine the JDK Maven runs on. Exiting."
    exit 9
fi
echo "Detected Maven JDK version: ${MVN_JAVA_VERSION}"

if [[ $MVN_JAVA_VERSION =~ ^1\.([0-9]+) ]]; then
    MVN_JAVA_MAJOR_VERSION=${BASH_REMATCH[1]}
elif [[ $MVN_JAVA_VERSION =~ ^([0-9]+) ]]; then
    MVN_JAVA_MAJOR_VERSION=${BASH_REMATCH[1]}
else
    echo "ERROR: Could not parse the Maven JDK version format. Exiting."
    exit 10
fi

if [[ $MVN_JAVA_MAJOR_VERSION -ne $REQUIRED_JDK_VERSION ]]; then
    echo "ERROR: Maven (${MVN_CMD}) runs on JDK ${MVN_JAVA_VERSION}, but exactly JDK ${REQUIRED_JDK_VERSION} is required. Exiting."
    echo "Point JAVA_HOME at a JDK ${REQUIRED_JDK_VERSION} installation, e.g. on macOS: export JAVA_HOME=\"\$(/usr/libexec/java_home -v ${REQUIRED_JDK_VERSION})\"."
    exit 11
fi
echo "Maven JDK version check passed. (== ${REQUIRED_JDK_VERSION})"

echo "Environment checks completed."

echo "Checking dependency version pins..."

python3 scripts/version_check.py

echo "Dependency version pin checks completed."

cd - > /dev/null
