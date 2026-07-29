#!/bin/bash

set -euo pipefail

repo_root=$(git rev-parse --show-toplevel)
cd "$repo_root"

: "${EXPECTED_CERT_SHA256:?Set EXPECTED_CERT_SHA256 to the trusted release signing certificate SHA-256 digest}"

apksigner_bin="${APKSIGNER:-}"
if [ -z "$apksigner_bin" ]; then
  apksigner_bin=$(find "${ANDROID_HOME:?ANDROID_HOME is required}/build-tools" -type f -name apksigner | sort -V | tail -1)
fi
if [ ! -x "$apksigner_bin" ]; then
  echo "apksigner was not found"
  exit 2
fi

echo "Obtain mixin APK you installed from your device"

device_apk=$(adb shell pm path one.mixin.messenger | sed -n 's/^package:\\(.*\\/base\\.apk\\)$/\\1/p' | tr -d '\r' | head -1)
if [ -z "$device_apk" ]; then
  echo "Installed base APK was not found"
  exit 2
fi
adb pull "$device_apk" mixin-store.apk

apk_to_verify=mixin-store.apk

if [ ! -f "$apk_to_verify" ]; then
    echo "$apk_to_verify is not an existing APK"
    exit 2
fi

tmp=$(mktemp -d)
trap 'rm -rf "$tmp"; rm -f mixin-store.apk' EXIT

# Prepare paths to extract APKs
mkdir -p "$tmp/to_verify" "$tmp/baseline"

echo "Building mixin APK from source code. This might take a while (20-30 minutes)..."

docker run --rm \
  -v "$repo_root":/project \
  mingc/android-build-box@sha256:de4a27cbc13a22563f82e93fde01c7366a5cda2c8ff113353d3e24e75c9ae8b6 \
  bash -c 'cd /project; ./gradlew :app:assembleGooglePlayRelease'

unzip -q -d "$tmp/to_verify" "$apk_to_verify"
unzip -q -d "$tmp/baseline" "app/build/outputs/apk/googlePlay/release/app-googlePlay-release-unsigned.apk"

actual_cert=$("$apksigner_bin" verify --print-certs "$apk_to_verify" | sed -n 's/^Signer #1 certificate SHA-256 digest: //p' | head -1)
normalized_actual_cert=$(printf '%s' "$actual_cert" | tr '[:lower:]' '[:upper:]' | tr -d ':')
normalized_expected_cert=$(printf '%s' "$EXPECTED_CERT_SHA256" | tr '[:lower:]' '[:upper:]' | tr -d ':')
if [ "$normalized_actual_cert" != "$normalized_expected_cert" ]; then
  echo "Verification failed: unexpected signing certificate"
  exit 1
fi

rm -rf "$tmp/to_verify/META-INF" "$tmp/baseline/META-INF"

if ! diff -r "$tmp/to_verify" "$tmp/baseline"; then
  echo "Verification failed"
  exit 1
fi

echo "Verification success"
