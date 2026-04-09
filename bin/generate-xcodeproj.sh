#!/usr/bin/env zsh
set -e

# Generates iosApp.xcodeproj and iosApp.xcworkspace based on project.yml.
# Use --force to skip the xcodegen cache (useful if overwriting the project).

use_cache_flag="--use-cache"
if [ "$1" = "--force" ]; then
  use_cache_flag=""
fi

cd "$(dirname "$0")"/..
ls gradlew > /dev/null 2>/dev/null || (echo "Failed to move to repo root" && exit 1)
pushd iosApp
xcodegen generate $use_cache_flag
popd
