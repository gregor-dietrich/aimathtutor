#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

set -e

cd "$DIR/../.."

# Usage: ./password.sh [password]
# If a password is provided as the first argument, use it non-interactively.
# Otherwise prompt the user (hidden input) and ask for confirmation.

if [ "$#" -ge 1 ] && [ -n "$1" ]; then
	PASSWORD="$1"
else
	# Prompt for password (hidden)
	echo -n "Enter password to generate salt+hash: "
	read -s PASS1
	echo
	echo -n "Confirm password: "
	read -s PASS2
	echo

	if [ "$PASS1" != "$PASS2" ]; then
		echo "Passwords do not match. Aborting." >&2
		exit 1
	fi

	PASSWORD="$PASS1"
fi

# Pass the password as the second token after "generate" (the Java CLI expects args[1])
${MVN_CMD} -q -Dexec.mainClass="de.vptr.aimathtutor.util.PasswordUtility" -Dexec.args="generate ${PASSWORD}" exec:java

cd - > /dev/null
