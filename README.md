# mobile_app

The MBTA mobile app.

## Project Setup

This project uses [Kotlin Multiplatform Mobile (KMM)](https://kotlinlang.org/docs/multiplatform.html). Native application code can be found in the `iosApp` and `androidApp` directories. Common code is within the `shared` directory.

### Prerequisites

Install the tools specified in `.tool-versions`. You can use [asdf](https://asdf-vm.com/) to help manage the required versions.

Install [direnv](https://direnv.net/) if you don't already have it, copy `.envrc.example` to `.envrc`, populate any required values, then run `direnv allow`.

For iOS development, `brew install swiftlint`.

### External Dependencies

#### Firebase App Check - [docs](https://firebase.google.com/docs/app-check)

App Check is used to validate that requests to our backend are coming from real instances of our app.

##### ios - [docs](https://firebase.google.com/docs/app-check/ios/custom-resource)

For running in debug mode locally, be sure to populate `FIREBASE_APP_CHECK_CI_TOKEN` in `/iosApp/AppCheckCI.xcconfig`
with the value found in 1pass (see `/iosApp/AppCheckCI.example.xcconfig` for an example).
If you need to generate a new token, from the [App Check console](https://console.firebase.google.com/u/0/project/mbta-app-c574d/appcheck/apps),
go to "Manage debug tokens" for the relevant app.

#### Mapbox - [docs](https://docs.mapbox.com/#maps)

We use mapbox for custom interactive maps.

##### ios - [guide](https://docs.mapbox.com/ios/maps/guides/) - [docs](https://docs.mapbox.com/ios/maps/api/11.2.0/documentation/mapboxmaps/) - [keys](https://docs.mapbox.com/ios/maps/guides/install/#configure-your-secret-token)

Mapbox requires 2 keys - a private key for installing the library and a public key for rendering map tiles. Follow the above keys link for instructions on how to configure the secret key.
The public key is fetched dynamically from the backend. Be sure to follow the Firebase App Check instructions for access to the protected endpoint while developing locally.

##### android - [guide](https://docs.mapbox.com/android/maps/guides/) - [docs](https://docs.mapbox.com/android/maps/api/11.3.0/) - [keys](https://docs.mapbox.com/android/maps/guides/install/#configure-your-secret-token)

Like on iOS, Mapbox for Android requires two keys. Follow the above keys link for instructions on how to configure the secret and public key.

**Note**: The property name in `~/.gradle/gradle.properties` is `MAPBOX_SECRET_TOKEN`, not `MAPBOX_DOWNLOADS_TOKEN` as used in the Mapbox documentation. Also, the public token should be stored in `androidApp/src/main/res/values/secrets.xml`, which will be created with a placeholder value if it does not already exist at build time.

#### Sentry - [docs](https://docs.sentry.io/platforms/kotlin-multiplatform/) - [keys](https://mbtace.sentry.io/settings/projects/mobile_app_ios/keys/)

Sentry is used for error logging and aggregation.

### Editor

The recommendation for KMM projects is to use Android Studio for editing & running the android app or shared code and XCode for only editing & running the ios app. See this [KMM guide](https://www.jetbrains.com/help/kotlin-multiplatform-dev/multiplatform-setup.html#install-the-necessary-tools) for installation instructions.

#### Gotchas

- Be sure to install the Android SDK Command-line Tools via Android Studio > Settings Android SDK > SDK Tool Tabs > Android SDK Command Line Tools.

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

To upload the code signing key if it needs to be updated (which is unlikely):

```
aws secretsmanager put-secret-value --secret-id mobile-app-android-upload-key --secret-binary fileb://upload-keystore.jks
cat key.properties | grep storePassword | cut -f2 -d= | tr -d '\n' > passphrase.txt
aws secretsmanager put-secret-value --secret-id mobile-app-android-upload-key-passphrase --secret-string file://passphrase.txt
shred --remove passphrase.txt
```

To download the code signing key if you need it locally (which is unlikely):

```
aws secretsmanager get-secret-value --secret-id mobile-app-android-upload-key --output json | jq -r '.SecretBinary' | base64 --decode > /path/to/upload-keystore.jks
aws secretsmanager get-secret-value --secret-id mobile-app-android-upload-key-passphrase --output json | jq -r '"storePassword=\(.SecretString)"' >> /path/to/key.properties
```

### Production Deploys

Pushing a new tag will automatically deploy to the internal testing group (on iOS), and then release notes can be added and the build can be more widely published manually.

The tag should match the version exactly - no `v` prefix - and the version needs to be set beforehand in `iosApp/iosApp/Info.plist`.

Android production deploys aren't set up yet.
