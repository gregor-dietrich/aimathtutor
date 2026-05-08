.PHONY: help branch build check clean coverage dev install kill lint password rebase release tag test untag

MAKEFLAGS += --no-print-directory

help:
	@echo "AIMathTutor - Available commands:"
	@echo "  make branch           - create or reset a git branch from a source (prompts for names and pushes)"
	@echo "  make build            - make check, mvn package, docker buildx"
	@echo "  make check            - verify local environment (JDK >=21 and Maven >=3.9.9)"
	@echo "  make clean            - run mvn clean, and remove build artifacts (logs, node_modules, target)"
	@echo "  make coverage         - run tests with JaCoCo and generate coverage-report.md"
	@echo "  make dev              - start Quarkus in dev mode"
	@echo "  make install          - make check, mvn clean install -DskipTests"
	@echo "  make kill             - stop/kill Quarkus and Maven processes and remove Docker containers"
	@echo "  make lint             - run spotless, checkstyle, spotbugs, pmd and cpd"
	@echo "  make password         - generate a salt+hash for a password (for init.sql)"
	@echo "  make rebase           - interactive git rebase against a target (defaults to origin/main)"
	@echo "  make release          - pull from origin/main, make build, make tag, and push Docker image to registry"
	@echo "  make tag              - create, sign and push a new git tag (auto-increments latest tag suggestion)"
	@echo "  make test             - execute the Maven test suite"
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
	@scripts/coverage.sh -o
 
dev:
	@scripts/dev.sh
 
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
