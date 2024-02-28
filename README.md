# mobile_app

The MBTA mobile app.

## Project Setup

### Prerequisites

Development for this project requires Ruby and Flutter, as described in .tool-versions. You can use [asdf](https://asdf-vm.com/) to help manage the required versions.

Install [direnv](https://direnv.net/) if you don't already have it, copy `.envrc.example` to `.envrc`, populate any required values, then run `direnv allow`.

### Editor

#### Gotchas

- Be sure to install the Android SDK Command-line Tools via Android Studio > Settings Android SDK > SDK Tool Tabs > Android SDK Command Line Tools.

### Project Dependencies

### Code Generation

## Running Locally
### iOS

The shared library dependency is managed using Cocoapods. To install the dependency and build the
ios app, you must first run a gradle sync of the project from Android Studio, or you may run
`./gradlew :shared:generateDummyFramework` from the root directory, then `pod install` from within
the `iosApp` directory.

Then, open the project from `/iosApp/iosApp.xcworkspace` in Xcode (not `iosApp.xcodeproj`).

Populate any configuration needed in your the .envrc file. These will be read by a build phase
script and set as info.plist values so that they can be read by the application.

## Running Tests

### Unit Tests

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

todo
