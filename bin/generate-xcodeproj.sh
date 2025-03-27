#!/usr/bin/env zsh
set -e

# Generates iosApp.xcodeproj and iosApp.xcworkspace based on project.yml.

cd "$(dirname "$0")"/..
ls gradlew > /dev/null 2>/dev/null || (echo "Failed to move to repo root" && exit 1)
pushd iosApp
xcodegen generate
bundle exec pod install
popd
