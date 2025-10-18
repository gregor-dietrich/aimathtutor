#!/bin/bash

set -e

cd "$(dirname "$0")"/../..
make check
make clean
 
./mvnw clean install -DskipTests

cd - > /dev/null
