#!/bin/sh

# Fail this script if any subcommand fails.
set -e

brew install swiftlint
cd ../iosApp
swiftlint

# Install Java
# Adapted from https://stackoverflow.com/a/76092736
ROOT_PATH=$CI_PRIMARY_REPOSITORY_PATH
JDK_PATH="${CI_DERIVED_DATA_PATH}/JDK"

 if [[ $(uname -m) == "arm64" ]]; then
      echo " - Detected M1"
      ARCH_TYPE="aarch64"
  else
      echo " - Detected Intel"
      ARCH_TYPE="x64"
  fi


# Download the JDK  and add it to path
curl -Lo jdk17.tar.gz "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.7%2B7/OpenJDK17U-jdk_${ARCH_TYPE}_mac_hotspot_17.0.7_7.tar.gz"
tar xvf jdk17.tar.gz -C $ROOT_PATH && rm jdk17.tar.gz

# Move the JDK into root directory.
# JAVA_HOME env var must be configured to JDK_PATH
rm -rf $JDK_PATH
mkdir -p $JDK_PATH
echo "root path ls"
cd $ROOT_PATH
ls
echo "jdk-17.0.7+7 ls"

cd "${ROOT_PATH}/jdk-17.0.7+7"
ls
echo "JDK path ls"
cd $JDK_PATH
ls

mv "${ROOT_PATH}/jdk-17.0.7+7" "${ROOT_PATH}/JDK"
mv "${ROOT_PATH}/JDK" $CI_DERIVED_DATA_PATH
echo "updated jdk path ls"
cd "${CI_DERIVED_DATA_PATH}/JDK"
ls
