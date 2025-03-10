# mobile_app

The MBTA mobile app.

## Project Setup

This project uses [Kotlin Multiplatform Mobile (KMM)](https://kotlinlang.org/docs/multiplatform.html). Native application code can be found in the `iosApp` and `androidApp` directories. Common code is within the `shared` directory.

### Prerequisites

Install the tools specified in `.tool-versions`. You can use [asdf](https://asdf-vm.com/) to help manage the required versions.

Install [direnv](https://direnv.net/) if you don't already have it, copy `.envrc.example` to `.envrc`, populate any required values, then run `direnv allow`.

Install and set up [the GitHub CLI](https://cli.github.com/manual/).

For Android development, `brew install librsvg`.

For iOS development, `brew install swiftlint`.

### External Dependencies

#### Mapbox - [docs](https://docs.mapbox.com/#maps)

We use mapbox for custom interactive maps.

##### ios - [guide](https://docs.mapbox.com/ios/maps/guides/) - [docs](https://docs.mapbox.com/ios/maps/api/11.2.0/documentation/mapboxmaps/)

Mapbox requires a public key for rendering map tiles.
The public key is fetched dynamically from the backend. Be sure to follow the Firebase App Check instructions for access to the protected endpoint while developing locally.

##### android - [guide](https://docs.mapbox.com/android/maps/guides/) - [docs](https://docs.mapbox.com/android/maps/api/11.3.0/)

Like on iOS, Mapbox for Android requires a public key, fetched dynamically from the backend.

#### Sentry - [docs](https://docs.sentry.io/platforms/kotlin-multiplatform/) - [keys](https://mbtace.sentry.io/settings/projects/mobile_app_ios/keys/)

Sentry is used for error logging and aggregation.

### Editor

The recommendation for KMM projects is to use Android Studio for editing & running the android app or shared code and XCode for only editing & running the ios app. See this [KMM guide](https://www.jetbrains.com/help/kotlin-multiplatform-dev/multiplatform-setup.html#install-the-necessary-tools) for installation instructions.

#### Gotchas

- Be sure to install the Android SDK Command-line Tools via Android Studio > Settings Android SDK > SDK Tool Tabs > Android SDK Command Line Tools.
- If you're seeing the error "undefined method 'map' for nil:NilClass" when running pod installs locally, you likely need to run `bundle exec gem uninstall ffi` then `bundle install` to get a cocoapods requirement to be installed properly on M1 Macs.
- If an Xcode build fails because of CocoaPods, try `bin/fix-cocoapods.sh`.
- If some piece of BOM generation fails in Xcode, try quitting Xcode and then running `open /Applications/Xcode.app` from a terminal with a good `PATH`.
- If iOS asset conversion fails in Android Studio, try quitting Android Studio and then running `open /Applications/Android\ Studio.app` from a terminal with a good `PATH`.
- If some piece of BOM generation still fails in Xcode even when Xcode was launched with a good `PATH`, try running `./gradlew :shared:bomCodegenIos` manually from a terminal with a good `PATH`.
- If `./gradlew :shared:bomCodegenIos` fails in a terminal with a good `PATH`, try `./gradlew --stop` to stop any daemons that have persisted a bad `PATH` and then try `./gradlew :shared:bomCodegenIos` again.
- If Android Studio can't find `rsvg-convert` even when Android Studio was launched with a good `PATH`, try `./gradlew --stop` to stop any daemons that have persisted a bad `PATH` and then try the build again.

## Running Locally

### iOS

The shared library dependency is managed using Cocoapods. To install the dependency and build the
ios app:

- Run a gradle sync of the project from Android Studio, or you may run
  `./gradlew :shared:generateDummyFramework` from the root directory
- `bundle install` to install cocoapods and fastlane
- `bundle exec pod install` from within the `iosApp` directory.
- Open the project from `/iosApp/iosApp.xcworkspace` in Xcode (not `iosApp.xcodeproj`).
- Populate any configuration needed in your the .envrc file. These will be read by a build phase
  script and set as info.plist values so that they can be read by the application.

### Android

To switch between the staging and prod app flavors, go to Build > Select Build Variant and then set the `:androidApp` Active Build Variant.

Populate any configuration needed in your the .envrc file. These will be read by a gradle build task
 set as BuildConfig values so that they can be read by the application.


## Running Tests

### Unit Tests

#### ios

Run from XCode by navigating to `Product > Test` or using the test navigator. We use [ViewInspector](https://github.com/nalexn/ViewInspector) to write unit tests for SwiftUI views.

#### android & shared

Run within Android Studio, or by running the commands `./gradlew androidApp:check` `./gradlew shared:check`

### Integration Tests

## Team Conventions

### Editing Code

- Create each new feature in its own branch named with the following naming format: initials-description (for example, Jane Smith writing a search function might create a branch called js-search-function).
- This repo uses [pre-commit hooks](https://pre-commit.com/), which will automatically run and update files before committing. Install with `brew install pre-commit` and set up the git hook scripts by running `pre-commit install`.
- Use meaningfully descriptive commit messages to help reviewers understand the changes. Consider following [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0-beta.2/) guidelines.

### Code Review

All new features are required to be reviewed by a team member. Department-wide code review practices can be found [here](https://www.notion.so/mbta-downtown-crossing/Code-Reviews-df7d4d6bb6aa4831a81bc8cef1bebbb5).

Some specifics for this repo:

- Follow [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0-beta.2/) for pull request titles.
- New pull requests will automatically kick off testing and request a review from the [mobile-app team](https://github.com/orgs/mbta/teams/mobile-app). If you aren't yet ready for a review, create a draft PR first.
- When adding commits after an initial review has been performed, avoid force-pushing to help reviewers follow the updated changes.
- Once a PR has been approved and all outstanding comments acknowledged, squash merge it to the main branch.

## CI

### iOS

XCode Cloud workflows are triggered on changes to the following directories:

- /iosApp
- /shared/common\*
- /shared/src/ios\*

If files are changed outside of those target directories but a new workflow run is reason, you can manually trigger a run through the XCode Cloud UI.

If new files or directories need to be added to the list of triggers, be sure to update the list for _each_ relevant XCode Cloud workflow

## Deploying

### Development Deploys

Merging to main will automatically kick off deploys that are visible for internal testing (TestFlight for ios, internal track for android).

To download the iOS App Store Connect key info if you need it locally (which may happen):

```
aws secretsmanager get-secret-value --secret-id mobile-app-ios-app-store-connect-api-key-id --output json | jq -r '"export APP_STORE_CONNECT_API_KEY_ID=\(.SecretString)"' >> .envrc
aws secretsmanager get-secret-value --secret-id mobile-app-ios-app-store-connect-api-key-issuer --output json | jq -r '"export APP_STORE_CONNECT_API_KEY_ISSUER=\(.SecretString)"' >> .envrc
aws secretsmanager get-secret-value --secret-id mobile-app-ios-app-store-connect-api-key-p8 --output json | jq -r '"export APP_STORE_CONNECT_API_KEY_P8=\(.SecretString | @json)"' >> .envrc
```

To upload the iOS code signing key if it needs to be updated (which is unlikely):

```
bundle exec fastlane ios cert_create
CERTID=$(basename iosApp/secrets/*.cer .cer)
echo $CERTID > iosApp/secrets/certid.txt
aws secretsmanager put-secret-value --secret-id mobile-app-ios-codesigning-id --secret-string file://iosApp/secrets/certid.txt
rm iosApp/secrets/certid.txt
aws secretsmanager put-secret-value --secret-id mobile-app-ios-codesigning-cer --secret-binary fileb://iosApp/secrets/${CERTID}.cer
aws secretsmanager put-secret-value --secret-id mobile-app-ios-codesigning-p12 --secret-binary fileb://iosApp/secrets/${CERTID}.p12
```

To download the iOS code signing key if you need it locally (which may happen):

```
CERTID=$(aws secretsmanager get-secret-value --secret-id mobile-app-ios-codesigning-id --output json | jq -r '.SecretString')
aws secretsmanager get-secret-value --secret-id mobile-app-ios-codesigning-cer --output json | jq -r '.SecretBinary' | base64 --decode > iosApp/secrets/${CERTID}.cer
aws secretsmanager get-secret-value --secret-id mobile-app-ios-codesigning-p12 --output json | jq -r '.SecretBinary' | base64 --decode > iosApp/secrets/${CERTID}.p12
bundle exec fastlane ios cert_load cert_id:$CERTID
bundle exec fastlane ios cert_check scheme:Staging
bundle exec fastlane ios cert_check scheme:Prod
```

To upload the Android code signing key if it needs to be updated (which is unlikely):

```
aws secretsmanager put-secret-value --secret-id mobile-app-android-upload-key --secret-binary fileb://upload-keystore.jks
cat key.properties | grep storePassword | cut -f2 -d= | tr -d '\n' > passphrase.txt
aws secretsmanager put-secret-value --secret-id mobile-app-android-upload-key-passphrase --secret-string file://passphrase.txt
shred --remove passphrase.txt
```

To download the Android code signing key if you need it locally (which is unlikely):

```
aws secretsmanager get-secret-value --secret-id mobile-app-android-upload-key --output json | jq -r '.SecretBinary' | base64 --decode > /path/to/upload-keystore.jks
aws secretsmanager get-secret-value --secret-id mobile-app-android-upload-key-passphrase --output json | jq -r '"storePassword=\(.SecretString)"' >> /path/to/key.properties
```

### Production Deploys

Pushing a new tag with the `ios-` prefix will automatically deploy to the iOS internal testing group, and then release notes can be added and the build can be more widely published manually.

Pushing a new tag with the `android-` prefix will automatically deploy to the Android internal testing group, and then release notes can be added and the build can be promoted to testing/prod manually.

The tag should be `<platform>-X.Y.Z` - no `v` prefix - and the version needs to be set beforehand in `iosApp/iosApp/Info.plist` and/or `androidApp/build.gradle.kts`.
