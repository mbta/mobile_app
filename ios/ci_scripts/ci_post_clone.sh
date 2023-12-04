#!/bin/sh

# Fail this script if any subcommand fails.
set -e

# The default execution directory of this script is the ci_scripts directory.
cd $CI_PRIMARY_REPOSITORY_PATH # change working directory to the root of your cloned repo.

# Install Flutter using git.
git clone --quiet --branch $(awk '/flutter/ { print $2 }' .tool-versions) --depth 1 https://github.com/flutter/flutter.git $HOME/flutter
export PATH="$PATH:$HOME/flutter/bin"

# Install Flutter artifacts for iOS (--ios), or macOS (--macos) platforms.
flutter precache --ios

# Install Flutter dependencies.
flutter pub get

# Generate code
# TODO: Uncomment when we start needing to generate code
# dart run build_runner build

# Install CocoaPods using Homebrew.
export HOMEBREW_NO_AUTO_UPDATE=1 # disable homebrew's automatic updates
export HOMEBREW_NO_INSTALL_CLEANUP=1 # disable automatic cleanup
brew install --quiet cocoapods

flutter build ios --config-only --dart-define SENTRY_DSN=$SENTRY_DSN --dart-define SENTRY_ENVIRONMENT=$SENTRY_ENVIRONMENT

# Install CocoaPods dependencies.
cd ios && pod install && cd ..

exit 0
