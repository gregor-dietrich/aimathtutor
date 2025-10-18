.PHONY: help branch build check clean dev install kill rebase release tag test untag
help:
	@echo "FIP Dev Tools - Available commands:"
	@echo "  make branch           - Create or reset git branches interactively"
	@echo "  make build            - Interactive build script with customer selection and versioning"
	@echo "  make check            - Verify installed Maven and JDK versions are compatible"
	@echo "  make clean            - Clean Maven artifacts and target directories"
	@echo "  make dev              - Interactive development mode launcher for customer modules"
	@echo "  make install          - Clean install and package project (skips tests)"
	@echo "  make kill             - Kill all Quarkus/Maven processes and Docker containers"
	@echo "  make rebase           - Perform git rebase operations"
	@echo "  make release          - Release a new version"
	@echo "  make tag              - Create and push git tags"
	@echo "  make test             - Interactive test runner for customer modules"
	@echo "  make untag            - Delete git tags"
 
branch:
	@scripts/sh/branch.sh
 
build:
	@scripts/sh/build.sh
 
check:
	@scripts/sh/check.sh
 
clean:
	@scripts/sh/clean.sh
 
dev:
	@scripts/sh/dev.sh
 
install:
	@scripts/sh/install.sh
 
kill:
	@scripts/sh/kill.sh
 
rebase:
	@scripts/sh/rebase.sh
 
release:
	@scripts/sh/release.sh
 
tag:
	@scripts/sh/tag.sh
 
test:
	@scripts/sh/test.sh
 
untag:
	@scripts/sh/untag.sh
