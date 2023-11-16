# mobile_app

The MBTA mobile app.

## Project Setup

### Prerequisites

Development for this project requires Ruby and Flutter, as described in .tool-versions. You can use [asdf](https://asdf-vm.com/) to help manage the required versions.

Follow the [official Flutter](https://docs.flutter.dev/get-started/install/macos) docs for directions on installing other requirements, including XCode and Android Studio.

Run `flutter doctor -v` to check that your environment is properly set up.

### Editor

You can use any editor, though we recommend using VSCode with the [flutter extension](https://marketplace.visualstudio.com/items?itemName=Dart-Code.flutter).
See the official Flutter [Set up an editor](https://docs.flutter.dev/get-started/editor?tab=vscode) docs for more details and alternatives, including Android Studio and IntelliJ.

#### Gotchas

- Be sure to install the Android SDK Command-line Tools via Android Studio > Settings Android SDK > SDK Tool Tabs > Android SDK Command Line Tools.

### Project Dependencies

Update dependencies via `flutter pub get`

### Code Generation

Run `dart run build_runner watch` to automatically run code generation as files are edited. For one-time generation, run `dart run build_runner build`.

## Running Locally

Run `flutter run -d <Device ID>`, where Device ID is a local emulator or connected device.
You can check what devices are available with `flutter devices`.

## Running Tests

### Unit Tests

`flutter test`

### Integration Tests

`flutter test integration_test -d <Device ID>`

## Team Conventions

### Editing Code

- Create each new feature in its own branch named with the following naming format: initials-description (for example, Jane Smith writing a search function might create a branch called js-search-function).
- This repo uses [pre-commit hooks](https://pre-commit.com/), which will automatically run and update files before committing. Install with `brew install pre-commit` and set up the git hook scripts by running `pre-commit install`.
- Ensure code is properly formatted before commiting. This can be done by running `dart format` before committing. For automatic formatting in VSCode, set the `formatOnSave` setting to true. For more details and alternative editor settings, see the official Flutter [Code formatting](https://docs.flutter.dev/tools/formatting#automatically-formatting-code-in-vs-code) docs.
- Use meaningfully descriptive commit messages to help reviewers understand the changes. Consider following [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0-beta.2/) guidelines.

### Code Review

All new features are required to be reviewed by a team member. Department-wide code review practices can be found [here](https://www.notion.so/mbta-downtown-crossing/Code-Reviews-df7d4d6bb6aa4831a81bc8cef1bebbb5).

Some specifics for this repo:

- Follow [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0-beta.2/) for pull request titles.
- New pull requests will automatically kick off testing and request a review from the [mobile-app team](https://github.com/orgs/mbta/teams/mobile-app). If you aren't yet ready for a review, create a draft PR first.
- When adding commits after an initial review has been performed, avoid force-pushing to help reviewers follow the updated changes.
- Once a PR has been approved and all outstanding comments acknowledged, squash merge it to the main branch.

## Deploying

### Development Deploys

Merging to main will automatically kick off deploys that are visible for internal testing (TestFlight for ios, internal track for android).

To upload the code signing key if it needs to be updated (which is unlikely):

```
$ aws secretsmanager put-secret-value --secret-id mobile-app-android-upload-key --secret-binary fileb://upload-keystore.jks
$ cat key.properties | grep storePassword | cut -f2 -d= | tr -d '\n' > passphrase.txt
$ aws secretsmanager put-secret-value --secret-id mobile-app-android-upload-key-passphrase --secret-string file://passphrase.txt
$ shred --remove passphrase.txt
```

To download the code signing key if you need it locally (which is unlikely):

```
$ aws secretsmanager get-secret-value --secret-id mobile-app-android-upload-key --output json | jq -r '.SecretBinary' | base64 --decode > /path/to/upload-keystore.jks
$ aws secretsmanager get-secret-value --secret-id mobile-app-android-upload-key-passphrase --output json | jq -r '"storePassword=\(.SecretString)"' >> /path/to/key.properties
```

### Production Deploys

todo
