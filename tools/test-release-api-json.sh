#!/usr/bin/env bash
set -euo pipefail

# Run every Retrofit JSON contract against the shipped DEX without installing the app.
# Usage: tools/test-release-api-json.sh DEVICE_SERIAL [googlePlay|otherChannel]
cd "$(dirname "$0")/.."
serial=${1:?Pass an adb device serial}
flavor=${2:-googlePlay}
case "$flavor" in
  googlePlay) variant=GooglePlay ;;
  otherChannel) variant=OtherChannel ;;
  *) echo "Unsupported flavor: $flavor" >&2; exit 1 ;;
esac
sdk=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$(sed -n 's/^sdk.dir=//p' local.properties)}}
build_tools="$sdk/build-tools/${BUILD_TOOLS_VERSION:-36.0.0}"
android_jar="$sdk/platforms/android-36/android.jar"
adb=("$sdk/platform-tools/adb" -s "$serial")
"${adb[@]}" get-state

./gradlew ":app:test${variant}DebugUnitTest" --rerun --tests one.mixin.android.ApiJsonContractTest --tests one.mixin.android.ReleaseKeepRulesTest
./gradlew ":app:assemble${variant}Release" \
  -x ":app:uploadCrashlyticsMappingFile${variant}Release" \
  -x ":app:bugsnagUpload${variant}ReleaseProguardMapping" \
  -x ":app:bugsnagCreate${variant}ReleaseBuild"

scratch=$(mktemp -d "$PWD/app/build/release-api-json.XXXXXX")
remote="/data/local/tmp/${scratch##*/}"
trap '"${adb[@]}" shell rm -rf "$remote"; rm -rf "$scratch"' EXIT
mkdir -p "$scratch/classes"
javac --release 8 -cp "$android_jar" -d "$scratch/classes" app/src/test/java/one/mixin/android/ReleaseApiJsonProbe.java
"$build_tools/d8" --release --min-api 26 --lib "$android_jar" --output "$scratch/probe.jar" "$scratch"/classes/one/mixin/android/*.class
awk '
  /^[^ #]/ { keep = /^one[.]mixin[.]android[.]/ }
  keep && (/^[^ #]/ || (/ -> / && !/[(]/)) { print }
' "app/build/outputs/mapping/${flavor}Release/mapping.txt" > "$scratch/mapping.txt"
apks=(app/build/outputs/apk/"$flavor"/release/*.apk)
if [ "${#apks[@]}" -ne 1 ]; then echo 'Expected exactly one Release APK' >&2; exit 1; fi
"${adb[@]}" shell mkdir -p "$remote"
"${adb[@]}" push "${apks[0]}" "$remote/app.apk"
"${adb[@]}" push "$scratch/probe.jar" "$scratch/mapping.txt" app/build/reports/api-json-contracts.json "$remote/"
"${adb[@]}" shell chmod 444 "$remote/app.apk" "$remote/probe.jar"
"${adb[@]}" shell "CLASSPATH=$remote/probe.jar" app_process /system/bin one.mixin.android.ReleaseApiJsonProbe \
  "$remote/app.apk" "$remote" "$remote/mapping.txt" "$remote/api-json-contracts.json" \
  | tee app/build/reports/release-api-json.log
