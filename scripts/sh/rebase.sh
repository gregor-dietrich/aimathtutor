#!/bin/bash

set -e

cd "$(dirname "$0")"/../..

git fetch

read -p "Enter the branch/commit to rebase against [origin/main]: " REBASE_TARGET
REBASE_TARGET=${REBASE_TARGET:-origin/main}

echo "Rebasing against: $REBASE_TARGET"
git rebase "$REBASE_TARGET"

cd - > /dev/null
