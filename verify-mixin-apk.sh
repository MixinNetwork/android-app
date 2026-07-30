#!/bin/bash

set -euo pipefail

repo_root=$(git rev-parse --show-toplevel)
cd "$repo_root"

: "${EXPECTED_CERT_SHA256:?Set EXPECTED_CERT_SHA256 to the trusted release signing certificate SHA-256 digest}"

bundletool_version=1.18.3
bundletool_sha256=a099cfa1543f55593bc2ed16a70a7c67fe54b1747bb7301f37fdfd6d91028e29
bundletool_url="https://github.com/google/bundletool/releases/download/$bundletool_version/bundletool-all-$bundletool_version.jar"
build_image=mingc/android-build-box@sha256:de4a27cbc13a22563f82e93fde01c7366a5cda2c8ff113353d3e24e75c9ae8b6

for tool in adb curl docker java unzip; do
  if ! command -v "$tool" >/dev/null; then
    echo "$tool was not found"
    exit 2
  fi
done

apksigner_bin="${APKSIGNER:-}"
if [ -z "$apksigner_bin" ]; then
  apksigner_bin=$(find "${ANDROID_HOME:?ANDROID_HOME is required}/build-tools" -type f -name apksigner | sort -V | tail -1)
fi
if [ ! -x "$apksigner_bin" ]; then
  echo "apksigner was not found"
  exit 2
fi

normalized_expected_cert=$(printf '%s' "$EXPECTED_CERT_SHA256" | tr '[:lower:]' '[:upper:]' | tr -d ':')
if [ "${#normalized_expected_cert}" -ne 64 ] || [[ "$normalized_expected_cert" == *[!0-9A-F]* ]]; then
  echo "EXPECTED_CERT_SHA256 is not a valid SHA-256 digest"
  exit 1
fi

if command -v sha256sum >/dev/null; then
  sha256_command=(sha256sum)
elif command -v shasum >/dev/null; then
  sha256_command=(shasum -a 256)
else
  echo "A SHA-256 checksum tool was not found"
  exit 2
fi

run_adb() {
  if [ -n "${DEVICE_ID:-}" ]; then
    adb -s "$DEVICE_ID" "$@"
  else
    adb "$@"
  fi
}

tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

bundletool_jar="${BUNDLETOOL_JAR:-$tmp/bundletool-all-$bundletool_version.jar}"
if [ -z "${BUNDLETOOL_JAR:-}" ]; then
  echo "Download Bundletool $bundletool_version"
  curl --fail --location --retry 3 --silent --show-error "$bundletool_url" --output "$bundletool_jar"
fi
actual_bundletool_sha256=$("${sha256_command[@]}" "$bundletool_jar" | awk '{print $1}')
if [ "$actual_bundletool_sha256" != "$bundletool_sha256" ]; then
  echo "Bundletool checksum verification failed"
  exit 1
fi

mkdir -p "$tmp/installed" "$tmp/generated" "$tmp/unpacked"

echo "Obtain Mixin APKs installed on the device"

run_adb shell pm path one.mixin.messenger |
  tr -d '\r' |
  sed -n 's|^package:||p' > "$tmp/device-apk-paths"

if ! grep -q '/base\.apk$' "$tmp/device-apk-paths"; then
  echo "Installed base APK was not found"
  exit 2
fi

while IFS= read -r device_apk; do
  run_adb pull "$device_apk" "$tmp/installed/$(basename "$device_apk")"
done < "$tmp/device-apk-paths"

echo "Building the Mixin app bundle from source. This might take a while (20-30 minutes)..."

docker run --rm \
  -v "$repo_root":/project \
  "$build_image" \
  bash -c 'cd /project; ./gradlew :app:bundleGooglePlayRelease'

if [ -n "${DEVICE_ID:-}" ]; then
  java -jar "$bundletool_jar" get-device-spec \
    --output="$tmp/device-spec.json" \
    "--device-id=$DEVICE_ID"
else
  java -jar "$bundletool_jar" get-device-spec \
    --output="$tmp/device-spec.json"
fi
java -jar "$bundletool_jar" build-apks \
  --bundle="$repo_root/app/build/outputs/bundle/googlePlayRelease/app-googlePlay-release.aab" \
  --output="$tmp/generated.apks" \
  --device-spec="$tmp/device-spec.json" \
  --overwrite
unzip -q "$tmp/generated.apks" -d "$tmp/generated"

installed_count=$(wc -l < "$tmp/device-apk-paths" | tr -d ' ')
generated_count=$(find "$tmp/generated" -type f -name '*.apk' | wc -l | tr -d ' ')
if [ "$installed_count" -ne "$generated_count" ]; then
  echo "Verification failed: installed and generated APK counts differ"
  exit 1
fi

while IFS= read -r device_apk; do
  installed_name=$(basename "$device_apk")
  case "$installed_name" in
    base.apk)
      generated_name=base-master.apk
      ;;
    split_config.*.apk)
      generated_name="base-${installed_name#split_config.}"
      ;;
    *)
      echo "Verification failed: unsupported installed split $installed_name"
      exit 1
      ;;
  esac

  installed_apk="$tmp/installed/$installed_name"
  generated_apk=$(find "$tmp/generated" -type f -name "$generated_name" -print -quit)
  if [ -z "$generated_apk" ]; then
    echo "Verification failed: generated APK $generated_name was not found"
    exit 1
  fi

  actual_cert=$("$apksigner_bin" verify --print-certs "$installed_apk" | sed -n 's/^Signer #1 certificate SHA-256 digest: //p' | head -1)
  normalized_actual_cert=$(printf '%s' "$actual_cert" | tr '[:lower:]' '[:upper:]' | tr -d ':')
  if [ "$normalized_actual_cert" != "$normalized_expected_cert" ]; then
    echo "Verification failed: unexpected signing certificate for $installed_name"
    exit 1
  fi

  installed_dir="$tmp/unpacked/installed-$installed_name"
  generated_dir="$tmp/unpacked/generated-$installed_name"
  mkdir -p "$installed_dir" "$generated_dir"
  unzip -q "$installed_apk" -d "$installed_dir"
  unzip -q "$generated_apk" -d "$generated_dir"

  for unpacked_dir in "$installed_dir" "$generated_dir"; do
    if [ -d "$unpacked_dir/META-INF" ]; then
      find "$unpacked_dir/META-INF" -maxdepth 1 -type f \
        \( -iname 'MANIFEST.MF' -o -iname '*.SF' -o -iname '*.RSA' -o -iname '*.DSA' -o -iname '*.EC' \) \
        -delete
      rmdir "$unpacked_dir/META-INF" 2>/dev/null || true
    fi
  done

  if ! diff -r "$installed_dir" "$generated_dir"; then
    echo "Verification failed: $installed_name differs from $generated_name"
    exit 1
  fi
done < "$tmp/device-apk-paths"

echo "Verification success"
