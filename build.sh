#!/bin/bash

set -e

mvn clean install -DskipTests
mvn test
mvn package -DskipTests -Pproduction

docker build -t gregordietrich/aimathtutor:1.0.0-SNAPSHOT -f src/main/docker/Dockerfile.jvm .
