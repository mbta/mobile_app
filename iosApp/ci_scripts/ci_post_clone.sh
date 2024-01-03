#!/bin/sh

# Fail this script if any subcommand fails.
set -e

brew install swiftlint
cd ../iosApp
swiftlint

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


# Run tests from shared directory
if [ $CI_XCODEBUILD_ACTION == "build-for-testing" ]; then
  echo "Running shared tests"
  cd $CI_PRIMARY_REPOSITORY_PATH
  ./gradlew shared:iosX64Test
fi
