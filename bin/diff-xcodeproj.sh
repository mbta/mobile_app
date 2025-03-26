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
rm "$FAKE_ROOT"/iosApp/Pods
ln -s "$REAL_REPO"/shared "$FAKE_ROOT"
pushd "$FAKE_ROOT"/iosApp
xcodegen generate --quiet
bundle exec pod install --silent
# this will print a leading '---' for YAML reasons even if the diff is empty, so tail -n +2 strips off the first line
bundle exec xcodeproj project-diff $REAL_REPO/iosApp/iosApp.xcodeproj $FAKE_ROOT/iosApp/iosApp.xcodeproj | tail -n +2 > "$FAKE_ROOT"/diff.yml
# check if diff.yml is nonempty
if [ -s "$FAKE_ROOT"/diff.yml ]; then
  ERR=1
  echo "Xcode project is dirty. If all of this is noise, just run bin/generate-xcodeproj.sh to clobber local changes, but if any of this is important, adjust project.yml to include it first."
  cat "$FAKE_ROOT"/diff.yml
else
  ERR=0
fi
popd
rm -rf "$FAKE_ROOT"/iosApp/Pods
rm -r "$FAKE_ROOT"
exit $ERR
