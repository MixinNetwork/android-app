# Mixin Android app
Mixin Android messenger, crypto wallet and light node to the Mixin Network


## Summary

 * Written in [Kotlin](https://kotlinlang.org/)
 * Uses [Jetpack](https://developer.android.com/jetpack): Room, LiveData, Paging, Lifecycle and ViewModel
 * Uses [Hilt](https://developer.android.com/jetpack/androidx/releases/hilt) for dependency injection

 ## Development setup

 ### Code style

This project uses [ktlint](https://github.com/shyiko/ktlint)

## Build reproducibly

* [Docker](https://www.docker.com/) ensure has at least 5 GB of RAM
    ```shell
    mkdir -p apk
    docker build -t mixin-android .
    docker run --rm -v "$PWD":/home/source mixin-android
    ```

## Verify installed mixin APK

* [Docker](https://www.docker.com/) ensure has at least 5 GB of RAM
* [ADB](https://developer.android.com/studio/releases/platform-tools)
    ```shell
    verify-mixin-apk.sh
    ```