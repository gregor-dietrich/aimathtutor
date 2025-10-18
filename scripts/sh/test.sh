#!/bin/bash

set -e

cd "$(dirname "$0")"/../..

make install

./mvnw test

cd - > /dev/null
