#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh

set -e

cd "$DIR/.."

echo "Starting rebase..."

git fetch

read -p "Enter the branch/commit to rebase against [origin/main]: " REBASE_TARGET
REBASE_TARGET=${REBASE_TARGET:-origin/main}

echo "Rebasing against: $REBASE_TARGET"
git rebase "$REBASE_TARGET"

echo "Force pushing..."
git push --force-with-lease

echo "Rebase completed."

cd - > /dev/null
