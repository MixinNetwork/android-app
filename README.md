# Mixin Android app
Mixin Android messenger, crypto wallet and light node to the Mixin Network


## Summary

 * Written in [Kotlin](https://kotlinlang.org/)
 * Uses [Jetpack](https://developer.android.com/jetpack): Room, LiveData, Paging, Lifecycle and ViewModel
 * Uses [Hilt](https://developer.android.com/jetpack/androidx/releases/hilt) for dependency injection

 ## Development setup

 ### Code style

This project uses [ktlint](https://github.com/pinterest/ktlint)

## Build reproducibly

* [Docker](https://www.docker.com/) ensure has at least 6 GB of RAM
    ```shell
    mkdir -p ./output-apk
    docker run --rm \
      -v $(pwd):/project \
      -v $(pwd)/output-apk:/home/gradle/app/build/outputs/apk/release \
      mingc/android-build-box bash -c 'cd /project; ./gradlew assembleRelease'
    ```

## Verify installed mixin APK

* [Docker](https://www.docker.com/) ensure has at least 6 GB of RAM
* [ADB](https://developer.android.com/studio/releases/platform-tools)
* Android SDK Build Tools with `ANDROID_HOME` configured
* The trusted release signing certificate SHA-256 digest
    ```shell
    EXPECTED_CERT_SHA256=<certificate-sha256> ./verify-mixin-apk.sh
    ```

The verification script builds the Google Play app bundle, generates the APK
set for the connected device, and compares every installed base and ABI split
APK. It downloads a pinned Bundletool release and verifies its checksum before
use.
