APP_NAME := DenariiDolor
VERSION ?= $(shell git describe --tags --always --dirty 2>/dev/null || echo dev)

.PHONY: help build test release version

help:
	@echo "Available targets:"
	@echo "  make build    - Build release APK with Gradle wrapper"
	@echo "  make test     - Run unit tests"
	@echo "  make release  - Build and assemble release artifacts"
	@echo "  make version  - Print resolved app version/tag"

build:
	./gradlew clean assembleRelease

test:
	./gradlew test

release: test build
	@echo "Release artifacts built for $(APP_NAME) ($(VERSION))"

version:
	@echo $(VERSION)
