#!/usr/bin/env zsh
set -e

# Installs the CocoaPods dependencies and updates the Gradle cache to recognize that the
# dependencies are installed.

cd "$(dirname "$0")"/..
ls gradlew > /dev/null 2>/dev/null || (echo "Failed to move to repo root" && exit 1)
pushd iosApp
bundle exec pod install
popd
./gradlew :shared:podInstallSyntheticIos
