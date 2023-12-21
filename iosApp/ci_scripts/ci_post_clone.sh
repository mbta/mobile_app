#!/bin/sh

# Fail this script if any subcommand fails.
set -e

brew install swiftlint
cd ../iosApp
swiftlint

# Install Java
JDK_PATH="${CI_DERIVED_DATA_PATH}/JDK"

# Download the JDK and move it to JDK_PATH
# JAVA_HOME env var must be manually configured to match JDK_PATH
brew install asdf
asdf plugin-add java
asdf install java
echo "$(asdf where java)"
DEFAULT_JAVA_PATH="$(asdf where java)"
DEFAULT_JAVA_ROOT_DIR="$(dirname DEFAULT_JAVA_PATH)"
rm -rf $JDK_PATH
mkdir -p $JDK_PATH
# Temporary move into generically named JDK folder
mv $DEFAULT_JAVA_PATH "${DEFAULT_JAVA_ROOT_DIR}/JDK"
# Move into JDK_PATH so that it can be referenced by JAVA_HOME env var
mv "${DEFAULT_JAVA_ROOT_DIR}/JDK" $CI_DERIVED_DATA_PATH
cd "${CI_DERIVED_DATA_PATH}"
