.PHONY: help branch build check clean dev install kill rebase release tag test untag

MAKEFLAGS += --no-print-directory

help:
	@echo "AIMathTutor - Available commands:"
	@echo "  make branch           - create or reset a git branch from a source (prompts for names and pushes)"
	@echo "  make build            - build the JVM Docker image (runs tests/install and Maven package, uses docker buildx)"
	@echo "  make check            - verify local environment (JDK >=21 and Maven >=3.9.8)"
	@echo "  make clean            - run 'mvnw clean' and remove build artifacts (logs, node_modules, target)"
	@echo "  make dev              - install deps and start Quarkus in dev mode (runs 'mvnw quarkus:dev')"
	@echo "  make install          - run checks, clean, and perform 'mvnw clean install' (skips tests)"
	@echo "  make kill             - stop/kill Quarkus and Maven processes and remove Docker containers"
	@echo "  make rebase           - interactive git rebase against a target (defaults to origin/main)"
	@echo "  make release          - build and push the Docker image tag to the registry"
	@echo "  make tag              - create, sign and push a new git tag (auto-increments latest tag suggestion)"
	@echo "  make test             - run install then execute the Maven test suite"
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
