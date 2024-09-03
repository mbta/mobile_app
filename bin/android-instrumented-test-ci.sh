#!/usr/bin/env bash

# three cheers for missing features upstream
# https://github.com/ReactiveCircus/android-emulator-runner/issues/316#issuecomment-1665866511

set +e
./gradlew connectedCheck --no-daemon
GRADLE_EXIT_CODE=$?

if [ $GRADLE_EXIT_CODE -ne 0 ]; then
  # Will print anything logged with the tag `ci-keep` if the tests failed.
  /usr/local/lib/android/sdk/platform-tools/adb -s emulator-5554 logcat -d '*:S' ci-keep
fi

exit $GRADLE_EXIT_CODE
