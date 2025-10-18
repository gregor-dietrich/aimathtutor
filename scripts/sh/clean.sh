#!/bin/bash
set -e

cd "$(dirname "$0")"/../..

# Run Maven clean
./mvnw clean

rm -rf logs
rm -rf node_modules
rm -rf target
cd - > /dev/null
