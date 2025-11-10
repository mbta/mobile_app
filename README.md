# mobile_app

[![App Store](https://img.shields.io/badge/App_Store-0D96F6?logo=app-store&logoColor=white "iOS App Store")](https://apps.apple.com/th/app/mbta-go-official/id6472726821) [![Play Store](https://img.shields.io/badge/Google_Play-414141?logo=google-play&logoColor=white "Android Play Store")](https://play.google.com/store/apps/details?id=com.mbta.tid.mbta_app)

Source code for MBTA Go.

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
- If your Gradle dependency tree has problems you need to visualize, try `./gradlew :shared:dependencies --configuration iosMainImplementationDependenciesMetadata` or `./gradlew :shared:dependencies --configuration releaseRuntimeClasspath` or `./gradlew :androidApp:dependencies --configuration stagingReleaseRuntimeClasspath`. Gradle sometimes just lies about what dependencies it’ll use, though.

## Running Locally

### iOS

The entire Xcode project is generated with [XcodeGen](https://github.com/yonaskolb/XcodeGen).
To generate the project, potentially overwriting local changes, use `bin/generate-xcodeproj.sh`.
If you need to make changes that you only understand in Xcode, make the changes and then use `bin/diff-xcodeproj.sh` to determine what the actual settings need to be.

When you switch branches or merge, pre-commit will automatically run `bin/generate-xcodeproj.sh`; this will clobber existing changes you may have made to your Xcode project, so don't change branches if your Xcode project is dirty.
When you commit, pre-commit will automatically run `bin/diff-xcodeproj.sh` to check that your Xcode project is not dirty; you'll need to parse through the signal vs noise and make any necessary changes to project.yml before running `bin/generate-xcodeproj.sh` manually to synchronize the Xcode project with what XcodeGen thinks it should look like.

The shared library dependency is managed using Cocoapods. To install the dependency and build the
ios app:

- Run a gradle sync of the project from Android Studio, or you may run
  `./gradlew :shared:generateDummyFramework` from the root directory
- `bundle install` to install cocoapods and fastlane
- `bin/generate-xcodeproj.sh` to generate the Xcode project and then integrate CocoaPods with it
- Open the project from `/iosApp/iosApp.xcworkspace` in Xcode (not `iosApp.xcodeproj`).
- Populate any configuration needed in your the .envrc file. These will be read by a build phase
  script and set as info.plist values so that they can be read by the application.

Running release builds on a real device (e.g. when profiling) may fail because of the code signing setup.
The simplest solution is to open the iosApp project, select the iosApp target, go to "Signing & Capabilities", and turn on "Automatically manage signing" for the variant you're interested in.
Make sure you don't commit this change, though, because that will break deployments in CI.

### Android

To switch between the staging and prod app flavors, go to Build > Select Build Variant and then set the `:androidApp` Active Build Variant.

Populate any configuration needed in your the .envrc file. These will be read by a gradle build task
 set as BuildConfig values so that they can be read by the application.

## i18n

The source of truth for our translations is
[Localizable.xcstrings](iosApp/iosApp/Localizable.xcstrings), any `en` strings added to
[strings.xml](androidApp/src/main/res/values/strings.xml) which match an equivalent `en` string in
`Localizable.xcstrings` will automatically have all existing translations imported to Android by the
[convertIosLocalization](buildSrc/src/main/kotlin/com/mbta/tid/mbta_app/gradle/ConvertIosLocalizationTask.kt)
gradle task on Android build.

If there are distinct strings that have the same English translation, give them an iOS key of
`key/<Android ID>` and then the Android resources will be written with that key automatically.

### Temporary machine translations

Any time we add new user facing strings to the app, we add temporary machine translations of that
text, while we're waiting to get translations back from our vendor. Any machine translations that
are added must be marked as "Needs review" in XCode so that the translators know to audit them.

To mostly-automatically fill in temporary machine translations, use [`placeholder-translations.py`](bin/placeholder-translations.py),
which will require you to paste in and copy out of Google Sheets, but will automatically determine
which new strings need translations, set up Google Sheets `=GOOGLETRANSLATE()` formulas, and then
save the results directly in `Localizable.xcstrings` marked as “Needs review”.

### Importing from `.xliff`

When we get translations from our vendor, they're generally in `.xliff` format, which we can import
into XCode by selecting `Product > Import Localizations...`, selecting `iOSApp Project` in the
dialog, then reviewing the imported strings and hitting `Import`. It's expected that many strings
will be missing, since we generally only get a partial batch of translations at a time.

### Importing from a spreadsheet

Sometimes, we need to batch import translations from a spreadsheet if the vendor has not provided us
with `.xliff` files. For this, you can use the [csv-to-xliff.py](bin/csv-to-xliff.py) script to
convert a CSV file into individual `.xliff`s for each language. The CSV must be formatted with a
header row of all of the language codes exactly matching the language codes that XCode expects, and
the first column must be `en`. The strings will also not be imported properly if the `en` string
doesn't _exactly_ match the strings in `Localizable.xcstrings`, we've had issues before where some
markdown formatting was removed in the provided spreadsheet, which resulted in XCode not importing
them as the same string.

To run `csv-to-xliff.py`, you should be able to just run `./bin/csv-to-xliff.py <csv file path>`
from the directory root. By default, it will put the `.xliff` files in a `translations-YYYY-MM-DD`
directory in the same directory you ran the script, but you can also add `-o <output directory>` to
specify a different directory. The generated files should never be commited directly, only imported.
This isn't a particularly robust or well tested script, so expect issues and be cautious about the
output.

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
bundle exec fastlane ios cert_check scheme:DevOrange
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
