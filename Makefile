SHELL := /bin/bash

# Release flow: pushing a tag matching v* triggers .github/workflows/cd.yml,
# which builds, signs, verifies and publishes the Android release.
#
# Usage:
#   make release VERSION=v1.2.3        # checks, tags HEAD of main, pushes the tag (triggers CD)
#   make release VERSION=v1.3.0-rc.1   # pre-release (marked as such on GitHub)
#   make tag VERSION=v1.2.3            # create the annotated tag locally only
#   make push-tag VERSION=v1.2.3       # push an existing local tag (triggers CD)

RELEASE_BRANCH ?= main
# Must stay in sync with the tag check in cd.yml (each part 0-999, optional -prerelease).
VERSION_REGEX := ^v[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}(-[0-9A-Za-z.-]+)?$$

.PHONY: help check-version check-repo tag push-tag release

help:
	@echo "Targets:"
	@echo "  make release VERSION=vX.Y.Z   Verify repo state, create and push the tag (triggers CD)"
	@echo "  make tag VERSION=vX.Y.Z       Create annotated tag locally"
	@echo "  make push-tag VERSION=vX.Y.Z  Push an existing tag to origin (triggers CD)"

check-version:
	@if [[ -z "$(VERSION)" ]]; then \
		echo "ERROR: VERSION is required (example: VERSION=v0.1.0)"; exit 1; \
	fi
	@if [[ ! "$(VERSION)" =~ $(VERSION_REGEX) ]]; then \
		echo "ERROR: VERSION must look like vMAJOR.MINOR.PATCH[-prerelease], each part 0-999 (example: v1.2.3)"; exit 1; \
	fi

check-repo:
	@git rev-parse --is-inside-work-tree >/dev/null
	@if [[ -n "$$(git status --porcelain)" ]]; then \
		echo "ERROR: working tree has uncommitted changes"; exit 1; \
	fi
	@if [[ "$$(git rev-parse --abbrev-ref HEAD)" != "$(RELEASE_BRANCH)" ]]; then \
		echo "ERROR: releases are cut from '$(RELEASE_BRANCH)' (current: $$(git rev-parse --abbrev-ref HEAD))"; exit 1; \
	fi
	@git fetch --quiet origin "$(RELEASE_BRANCH)" --tags
	@if [[ "$$(git rev-parse HEAD)" != "$$(git rev-parse "origin/$(RELEASE_BRANCH)")" ]]; then \
		echo "ERROR: local $(RELEASE_BRANCH) differs from origin/$(RELEASE_BRANCH); push or pull first"; exit 1; \
	fi

tag: check-version
	@git rev-parse --is-inside-work-tree >/dev/null
	@if git rev-parse -q --verify "refs/tags/$(VERSION)" >/dev/null; then \
		echo "ERROR: tag $(VERSION) already exists"; exit 1; \
	fi
	git tag -a "$(VERSION)" -m "Release $(VERSION)"
	@echo "Created tag $(VERSION)"

push-tag: check-version
	@if ! git rev-parse -q --verify "refs/tags/$(VERSION)" >/dev/null; then \
		echo "ERROR: tag $(VERSION) does not exist locally. Run: make tag VERSION=$(VERSION)"; exit 1; \
	fi
	git push origin "refs/tags/$(VERSION)"
	@echo "Pushed tag $(VERSION) - CD workflow started: https://github.com/$$(git remote get-url origin | sed -E 's#(git@github.com:|https://github.com/)##; s#\.git$$##')/actions/workflows/cd.yml"

release: check-version check-repo tag push-tag
	@echo "Release $(VERSION) tagged and pushed."
