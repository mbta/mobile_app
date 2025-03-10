fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android build

```sh
[bundle exec] fastlane android build
```

Build an .apk and an .aab with an upcoming version code

### android internal

```sh
[bundle exec] fastlane android internal
```

Deploy a new version to Google Play for internal testing

----


## iOS

### ios cert_create

```sh
[bundle exec] fastlane ios cert_create
```

Create a new code signing certificate

### ios cert_load

```sh
[bundle exec] fastlane ios cert_load
```

Load downloaded code signing certificates

### ios cert_check

```sh
[bundle exec] fastlane ios cert_check
```

Check certificate and provisioning configuration

### ios test

```sh
[bundle exec] fastlane ios test
```

Run tests

### ios build

```sh
[bundle exec] fastlane ios build
```

Build the app

### ios internal

```sh
[bundle exec] fastlane ios internal
```

Upload the app to TestFlight for internal testing

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
