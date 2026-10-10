#!/bin/bash

# Run through make, which exports PROJECT_ROOT and DEVKIT (devkit's make/common.mk).
cd "${PROJECT_ROOT:?run this through make}" || exit
. "${DEVKIT:?run this through make}/scripts/lib/get_maven.sh"

set -e

# Prompt for the password (hidden input) and ask for confirmation.
echo -n "Enter password to hash: "
IFS= read -rs PASS1
echo
echo -n "Confirm password: "
IFS= read -rs PASS2
echo

if [ "$PASS1" != "$PASS2" ]; then
	echo "Passwords do not match. Aborting." >&2
	exit 1
fi

PASSWORD="$PASS1"

# Pass the password on stdin: exec:java splits exec.args on whitespace, and arguments show up in ps.
# Compile first: exec:java runs from target/classes, which a fresh checkout doesn't have.
echo "Generating bcrypt hash..."
printf '%s\n' "$PASSWORD" | ${MVN_CMD} -q -Dexec.mainClass="de.vptr.aimathtutor.util.PasswordUtil" -Dexec.args=generate compile exec:java
echo "Password hash generated."
