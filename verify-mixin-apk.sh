#!/bin/bash

set -e

cd $(git rev-parse --show-toplevel)

echo "Obtain mixin APK you installed from your device"

adb pull $(adb shell pm path one.mixin.messenger | grep "/base.apk" | sed 's/^package://') mixin-store.apk
wait

apk_to_verify=mixin-store.apk

if [ ! -f "$apk_to_verify" ]; then
    echo "$apk_to_verify is not an existing APK"
    exit 2
fi

tmp=$(mktemp -d)

# Prepare paths to extract APKs
mkdir -p "$tmp/to_verify" "$tmp/baseline"

echo "Building mixin APK from source code. This might take a while (20-30 minutes)..."

mkdir -p ./output-apk
docker run --rm \
  -v $(pwd):/project \
  -v $(pwd)/output-apk:/home/gradle/app/build/outputs/apk/release \
  mingc/android-build-box bash -c 'cd /project; ./gradlew :app:assembleRelease'

unzip -q -d "$tmp/to_verify" "$apk_to_verify"
unzip -q -d "$tmp/baseline" "output-apk/app-release-unsigned.apk"

# Remove the signature since OSS users won't have Mixin private signing key
rm -r "$tmp"/{to_verify,baseline}/{META-INF,resources.arsc}

diff -r "$tmp/to_verify" "$tmp/baseline" && echo "Verification success!" || echo "Verification failed :("

rm mixin-store.apk