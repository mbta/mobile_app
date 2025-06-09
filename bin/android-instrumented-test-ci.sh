#!/usr/bin/env bash

# three cheers for missing features upstream
# https://github.com/ReactiveCircus/android-emulator-runner/issues/316#issuecomment-1665866511

set +e

# Our Android instrumented tests are very flaky, so retry them.
# https://unix.stackexchange.com/a/137639
function retry() {
  local n=1
  # for some reason, the syntax for "$foo or bar if $foo is unset" is ${foo-bar}
  local max=${RETRIES-5}
  local delay=5
  while true; do
    if "$@"; then
      break
    else
      if [[ $n -lt $max ]]; then
        ((n++))
        echo "Command failed. Attempt $n/$max:"
        sleep $delay
      else
        return 1
      fi
    fi
  done
}

./gradlew :androidApp:connectedStagingDebugAndroidTest --no-daemon
GRADLE_EXIT_CODE=$?

if [ $GRADLE_EXIT_CODE -ne 0 ]; then
  # Will print anything logged with the tag `ci-keep` if the tests failed.
  /usr/local/lib/android/sdk/platform-tools/adb -s emulator-5554 logcat -d '*:S' ci-keep
fi

exit $GRADLE_EXIT_CODE
