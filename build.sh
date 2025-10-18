#!/bin/bash

set -e

mvn clean install -DskipTests
mvn test
mvn package -DskipTests -Pproduction

docker buildx build --platform linux/amd64,linux/arm64 -t gregordietrich/aimathtutor:1.0.0-SNAPSHOT -f src/main/docker/Dockerfile.jvm .
