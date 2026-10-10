PROJECT      := aimathtutor
JAVA_VERSION := 25
MODULES      := # empty for a monolith
FRONTEND_DIR := .
DEVKIT := $(shell ./devkitw path)
ifeq ($(DEVKIT),)
$(error devkitw failed; see its message above)
endif
include .devkit/make/common.mk
include .devkit/make/java-maven.mk

.PHONY: build dev password regen-frontend release frontend-manifest test-scripts

build: check ## build the app and its Docker images for this host (prompts for the image tag)
	@scripts/build.sh

dev: ## start Quarkus in dev mode (port 9001)
	@scripts/dev.sh

password: ## generate a bcrypt hash for seed data or an administrator reset
	@scripts/password.sh

regen-frontend: ## recreate package.json and package-lock.json from scratch at the current Vaadin version
	@scripts/regen-frontend.sh

release: ## pull from origin/main, test, make tag, and buildx-push multi-platform images
	@scripts/release.sh

# Builds install the frontend from the committed manifest, so refuse to build without one.
install: frontend-manifest
frontend-manifest:
	@bash -c '. scripts/lib/frontend.sh && require_frontend_manifest'

# The release-script tests need no Maven or network access, so they run before the Java tests.
test: test-scripts
test-scripts: ## run the Python tests of the build scripts
	@python3 -B -m unittest discover -s scripts/tests -v
