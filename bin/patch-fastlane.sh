#!/usr/bin/env bash

# This is a little wacky.
#
# The existing behavior, which has been in place since 2016, only loads a service account private key,
# which is the advised-against authentication mechanism that the GCP OAuth stuff is intended to replace.
# (In 2025, support for external accounts was added, but not for authorized users or for loading application default credentials.)
# The googleauth gem supports three types of credential: service account (supported by Fastlane),
# authorized user (provisioned by gcloud auth application-default when running locally),
# and external account (created by google-github-actions/auth@v1 for workload identity federation, added in March of 2023).
# Conveniently, the whole point of the application default credentials is to be loaded by default by applications,
# so I think Google::Auth.get_application_default would have been the approach taken by Fastlane if it hadn't only been added in 2017.
#
# See also https://github.com/fastlane/fastlane/discussions/20022 and https://github.com/fastlane/fastlane/pull/16414.

sed -i.orig 's/UI.user_error!("Invalid Google Credentials file provided - no credential type found.")/auth_client = Google::Auth.get_application_default(scope: self.class::SCOPE)/' "$(bundle show fastlane)/supply/lib/supply/client.rb"

# For local authentication,
# gcloud auth application-default login --scopes=openid,https://www.googleapis.com/auth/userinfo.email,https://www.googleapis.com/auth/cloud-platform,https://www.googleapis.com/auth/androidpublisher
