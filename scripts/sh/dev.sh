#!/bin/bash

set -e

cd "$(dirname "$0")"/../..

make install

./mvnw quarkus:dev

cd - > /dev/null
