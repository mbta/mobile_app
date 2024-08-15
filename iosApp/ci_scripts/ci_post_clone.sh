#!/bin/sh

# Fail this script if any subcommand fails.
set -e

# Install SwiftLint
brew install swiftlint

# Install Java
JDK_PATH="${CI_DERIVED_DATA_PATH}/JDK"

echo "Checking for cached JDK"
# If the JDK isn't already installed, download it JDK and move it to JDK_PATH
# JAVA_HOME env var must be manually configured to match JDK_PATH
if [ -d $JDK_PATH ]; then
  echo "JDK already found, skipping download"
  exit
fi

brew install asdf
asdf plugin-add java
asdf install java
DEFAULT_JAVA_PATH="$(asdf where java)"
DEFAULT_JAVA_ROOT_DIR="$(dirname DEFAULT_JAVA_PATH)"
rm -rf $JDK_PATH
mkdir -p $JDK_PATH
# Temporary move into generically named JDK folder
mv $DEFAULT_JAVA_PATH "${DEFAULT_JAVA_ROOT_DIR}/JDK"
# Move into JDK_PATH so that it can be referenced by JAVA_HOME env var
mv "${DEFAULT_JAVA_ROOT_DIR}/JDK" $CI_DERIVED_DATA_PATH

# Install cocoapods
brew install cocoapods
cd "${CI_PRIMARY_REPOSITORY_PATH}"
./gradlew :shared:generateDummyFramework
cd "${CI_PRIMARY_REPOSITORY_PATH}/iosApp"
pod install
cd ..

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
  ./gradlew shared:iosX64Test
fi

echo "Adding build environment variables"
cd ${CI_PRIMARY_REPOSITORY_PATH}
touch .envrc
echo "export SENTRY_DSN=${SENTRY_DSN}" >> .envrc
echo "export SENTRY_ENVIRONMENT=${SENTRY_ENVIRONMENT}" >> .envrc
echo "export FIREBASE_KEY=${FIREBASE_KEY}" >> .envrc
echo "export GOOGLE_APP_ID=${GOOGLE_APP_ID}" >> .envrc
echo "export APPCUES_ACCOUNT_ID=${APPCUES_ACCOUNT_ID}" >> .envrc
echo "export APPCUES_APP_ID=${APPCUES_APP_ID}" >> .envrc
