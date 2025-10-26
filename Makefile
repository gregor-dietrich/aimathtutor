.PHONY: help branch build check clean dev install kill password rebase release tag test untag

MAKEFLAGS += --no-print-directory

help:
	@echo "AIMathTutor - Available commands:"
	@echo "  make branch           - create or reset a git branch from a source (prompts for names and pushes)"
	@echo "  make build            - make check, mvn package, docker buildx
	@echo "  make check            - verify local environment (JDK >=21 and Maven >=3.9.9)"
	@echo "  make clean            - run mvn clean, and remove build artifacts (logs, node_modules, target)"
	@echo "  make dev              - start Quarkus in dev mode"
	@echo "  make install          - make check, mvn clean install -DskipTests"
	@echo "  make kill             - stop/kill Quarkus and Maven processes and remove Docker containers"
	@echo "  make password         - generate a salt+hash for a password (for init.sql)"
	@echo "  make rebase           - interactive git rebase against a target (defaults to origin/main)"
	@echo "  make release          - pull from origin/main, make build, make tag, and push Docker image to registry"
	@echo "  make tag              - create, sign and push a new git tag (auto-increments latest tag suggestion)"
	@echo "  make test             - execute the Maven test suite"
	@echo "  make untag            - delete a local and remote git tag (prompts for tag to delete)"

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

password:
	@scripts/sh/password.sh
 
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
