.PHONY: help branch build check clean coverage dev format install kill lint password rebase release tag test untag

MAKEFLAGS += --no-print-directory

help:
	@echo "AIMathTutor - Available commands:"
	@echo "  make branch           - create or reset a git branch from a source (prompts for names and pushes)"
	@echo "  make build            - make check, mvn package, docker buildx"
	@echo "  make check            - verify local environment (JDK >=25 and Maven >=3.9.9)"
	@echo "  make clean            - run mvn clean, and remove build artifacts (logs, node_modules, target)"
	@echo "  make coverage         - run all tests (including ITs) and generate coverage report"
	@echo "  make dev              - start Quarkus in dev mode"
	@echo "  make format           - run spotless to format code"
	@echo "  make install          - make check, mvn clean install -DskipTests, auto-regenerates the"
	@echo "                          frontend manifest if it's out of date for the current Vaadin version"
	@echo "  make kill             - stop/kill Quarkus and Maven processes and remove Docker containers"
	@echo "  make lint             - run quality gate plugins"
	@echo "  make password         - generate a salt+hash for a password (for init.sql)"
	@echo "  make rebase           - interactive git rebase against a target (defaults to origin/main)"
	@echo "  make release          - pull from origin/main, make build, make tag, and push Docker image to registry"
	@echo "  make tag              - create, sign and push a new git tag (auto-increments latest tag suggestion)"
	@echo "  make test             - run unit tests (skips ITs)"
	@echo "  make untag            - delete a local and remote git tag (prompts for tag to delete)"

branch:
	@scripts/branch.sh

build:
	@scripts/build.sh

check:
	@scripts/check.sh

clean:
	@scripts/clean.sh

coverage:
	@scripts/coverage.sh

dev:
	@scripts/dev.sh

format:
	@scripts/format.sh

install:
	@scripts/install.sh

kill:
	@scripts/kill.sh

lint:
	@scripts/lint.sh

password:
	@scripts/password.sh

rebase:
	@scripts/rebase.sh

release:
	@scripts/release.sh

tag:
	@scripts/tag.sh

test:
	@scripts/test.sh

untag:
	@scripts/untag.sh
