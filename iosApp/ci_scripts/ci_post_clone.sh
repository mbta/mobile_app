#!/bin/bash

# Fail this script if any subcommand fails.
set -e

# Outgoing network connections from Xcode Cloud frequently just break, so retry everything a lot.
# https://unix.stackexchange.com/a/137639
function retry() {
  local n=1
  # for some reason, the syntax for "$foo or bar if $foo is unset" is ${foo-bar}
  local max=${RETRIES-5}
  local delay=5
  while true; do
    if "$@"; then
      break
    else
      if [[ $n -lt $max ]]; then
        ((n++))
        echo "Command failed. Attempt $n/$max:"
        sleep $delay
      else
        return 1
      fi
    fi
  done
}

# Install SwiftLint
retry brew install swiftlint

# Install Java
JDK_PATH="${CI_DERIVED_DATA_PATH}/JDK"

echo "Checking for cached JDK"
# If the JDK isn't already installed, download it JDK and move it to JDK_PATH
# JAVA_HOME env var must be manually configured to match JDK_PATH
if [ -d $JDK_PATH ]; then
  echo "JDK already found, skipping download"
  exit
fi

retry brew install asdf
retry asdf plugin-add java
retry asdf install java
DEFAULT_JAVA_PATH="$(asdf where java)"
DEFAULT_JAVA_ROOT_DIR="$(dirname DEFAULT_JAVA_PATH)"
rm -rf $JDK_PATH
mkdir -p $JDK_PATH
# Temporary move into generically named JDK folder
mv $DEFAULT_JAVA_PATH "${DEFAULT_JAVA_ROOT_DIR}/JDK"
# Move into JDK_PATH so that it can be referenced by JAVA_HOME env var
mv "${DEFAULT_JAVA_ROOT_DIR}/JDK" $CI_DERIVED_DATA_PATH

# Install ruby and bundler dependencies
retry brew install ruby@3.2
export PATH="/usr/local/opt/ruby@3.2/bin:$PATH"
retry gem install bundler:2.5.3 # match Gemfile.lock
retry bundle install

# Run cocoapods
cd "${CI_PRIMARY_REPOSITORY_PATH}"
retry ./gradlew :shared:generateDummyFramework
cd "${CI_PRIMARY_REPOSITORY_PATH}/iosApp"
retry bundle exec pod install
cd ..
retry bundle exec ./gradlew :shared:podInstallSyntheticIos

# Install Node.js and GitHub CLI for codegen
retry brew install node gh

# Configure Mapbox token for installation
cd $CI_PRIMARY_REPOSITORY_PATH
touch ~/.netrc
echo "machine api.mapbox.com" > ~/.netrc
echo "login mapbox" >> ~/.netrc
echo "password ${MAPBOX_SECRET_TOKEN}" >> ~/.netrc


# Run tests from shared directory
if [ $CI_XCODEBUILD_ACTION == "build-for-testing" ]; then
  echo "Running shared tests"
  cd $CI_PRIMARY_REPOSITORY_PATH
  RETRIES=2 retry bundle exec ./gradlew shared:iosX64Test
fi

echo "Adding build environment variables"
cd ${CI_PRIMARY_REPOSITORY_PATH}
touch .envrc
echo "export SENTRY_DSN_IOS=${SENTRY_DSN}" >> .envrc
echo "export SENTRY_ENVIRONMENT=${SENTRY_ENVIRONMENT}" >> .envrc
echo "export FIREBASE_KEY=${FIREBASE_KEY}" >> .envrc
echo "export GOOGLE_APP_ID=${GOOGLE_APP_ID}" >> .envrc
