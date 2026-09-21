#!/bin/bash

set -euo pipefail

repo_root=$(cd "$(dirname "$0")" && pwd)
ndk="${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME to Android NDK 28.2.13676358}"
if ! grep -Eq '^Pkg.Revision *= *28\.2\.13676358$' "$ndk/source.properties"; then
  echo "Android NDK 28.2.13676358 is required"
  exit 1
fi

# curve25519-android 0.5.0, matching the Signal dependency.
source_commit=70fae57d6dccff7e78a46203c534314b07dfdd98
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

git init -q "$tmp/source"
git -C "$tmp/source" fetch -q --depth=1 https://github.com/signalapp/curve25519-java.git "$source_commit"
git -C "$tmp/source" checkout -q --detach FETCH_HEAD
test "$(git -C "$tmp/source" rev-parse HEAD)" = "$source_commit"

cd "$tmp/source/android/jni"
"$ndk/ndk-build" \
  NDK_PROJECT_PATH=.. APP_BUILD_SCRIPT=Android.mk NDK_APPLICATION_MK=Application.mk \
  APP_ABI=arm64-v8a APP_PLATFORM=android-26 APP_SUPPORT_FLEXIBLE_PAGE_SIZES=true \
  'APP_LDFLAGS=-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384' \
  NDK_OUT="$tmp/obj" NDK_LIBS_OUT="$tmp/lib" -j4

library="$tmp/lib/arm64-v8a/libcurve25519.so"
readelf=("$ndk"/toolchains/llvm/prebuilt/*/bin/llvm-readelf)
"${readelf[0]}" -l "$library" > "$tmp/headers"
awk '$1 == "LOAD" { print $NF }' "$tmp/headers" > "$tmp/alignments"
test -s "$tmp/alignments"
while read -r alignment; do
  test "$((alignment))" -ge 16384
done < "$tmp/alignments"
read -r relro_address relro_size < <(awk '$1 == "GNU_RELRO" { print $3, $6 }' "$tmp/headers")
test "$(((relro_address + relro_size) % 16384))" -eq 0

destination="$repo_root/app/src/main/jniLibs/arm64-v8a/libcurve25519.so"
mkdir -p "$(dirname "$destination")"
cp "$library" "$destination"
echo "Built curve25519 0.5.0 with NDK r28c and verified 16 KB LOAD/RELRO alignment: $destination"
