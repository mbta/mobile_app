#!/usr/bin/env zsh
set -e

# Generates the Xcode project from scratch in a temporary directory and prints differences between what the Xcode project should look like and what it does look like.

cd "$(dirname "$0")"/..
ls gradlew > /dev/null 2>/dev/null || (echo "Failed to move to repo root" && exit 1)
REAL_REPO=$(pwd)
FAKE_ROOT=$(mktemp -dt $(basename "$0"))
mkdir -p "$FAKE_ROOT"/iosApp
ln -s "$REAL_REPO"/.tool-versions "$FAKE_ROOT"
ln -s "$REAL_REPO"/Gemfile "$FAKE_ROOT"
ln -s "$REAL_REPO"/Gemfile.lock "$FAKE_ROOT"
ln -s "$REAL_REPO"/iosApp/* "$FAKE_ROOT"/iosApp
ln -s "$REAL_REPO"/iosApp/.swift* "$FAKE_ROOT"/iosApp
rm "$FAKE_ROOT"/iosApp/iosApp.xcodeproj
rm "$FAKE_ROOT"/iosApp/iosApp.xcworkspace
ln -s "$REAL_REPO"/shared "$FAKE_ROOT"
pushd "$FAKE_ROOT"/iosApp
xcodegen generate --quiet
bundle exec pod install --silent
bundle exec xcodeproj project-diff $REAL_REPO/iosApp/iosApp.xcodeproj $FAKE_ROOT/iosApp/iosApp.xcodeproj
popd
rm -r "$FAKE_ROOT"
