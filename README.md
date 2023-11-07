# mbta_app

The MBTA Mobile App

## Setup

### Prerequisites

Development for the mobile app requires Ruby and Flutter, as described in .tool-versions. You can use [asdf](https://asdf-vm.com/) to help manage the required versions.

Follow the [official Flutter](https://docs.flutter.dev/get-started/install/macos) docs for directions on installing other requirements, including XCode and Android Studio.

Run `flutter doctor -v` to check that your environment is properly set up

#### Gotchas

- Be sure to install the Android SDK Command-line Tools via Android Studio > Settings Android SDK > SDK Tool Tabs

### Project Dependencies

Update dependencies via `flutter pub get`

### Code Generation

In a separate window, run `dart run build_runner watch` to automatically run code generation. For one-time generation, run `dart run build_runner build`

## Running Locally

Run `flutter run -d <Device ID>`, where Device ID is a local emulator or connected device.
You can check what devices are available with `flutter devices`

## Running Tests

### Unit Tests

`flutter test`

### Integration Tests

`flutter test integration_test -d <Device ID>`

## Team Conventions

### Editing Code

- Create each new feature in its own branch named with the following naming format: initials-description (for example, Jane Smith writing a search function might create a branch called js-search-function).
- Follow [Conventional Commit](https://www.conventionalcommits.org/en/v1.0.0-beta.2/) guidelines

### Code Review

All new features are required to be reviewed by a team member. Department-wide code review practices can be found [here](https://www.notion.so/mbta-downtown-crossing/Code-Reviews-df7d4d6bb6aa4831a81bc8cef1bebbb5).

Some specifics for this repo:

- New pull requests will automatically kick off testing. Wait for the tests to finish prior to requesting review.
- Once all the the automated tests pass, request a review from the [mobile-app team](https://github.com/orgs/mbta/teams/mobile-app)
- When adding commits after an initial review has been performed, avoid force-pushing to help reviewers follow the updated changes.
- Once a PR has been approved and all outstanding comments acknowledged, squash merge it to the main branch

## Deploying

### Development Deploys

Merging to main will automatically kick off deploys that are visible for internal testing (TestFlight for ios, internal track for android)

### Production Deploys

To do
