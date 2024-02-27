#!/bin/sh
set -e

if [ $CI_XCODEBUILD_ACTION == "archive" ]; then
  if [[ $(command -v sentry-cli) == "" ]]; then
      echo "Installing Sentry CLI"
      brew install getsentry/tools/sentry-cli
  fi

  echo "Uploading dSYM to Sentry"
  sentry-cli --auth-token $SENTRY_UPLOAD_TOKEN \
      upload-dif --org 'mbtace' \
      --project 'mobile_app_ios' \
      $CI_ARCHIVE_PATH
fi
