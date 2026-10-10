# Mixin Android app
Mixin Android messenger, crypto wallet and light node to the Mixin Network


## Summary

 * Written in [Kotlin](https://kotlinlang.org/)
 * Uses [Jetpack](https://developer.android.com/jetpack): Room, LiveData, Paging, Lifecycle and ViewModel
 * Uses [Hilt](https://developer.android.com/jetpack/androidx/releases/hilt) for dependency injection

 ## Development setup

 ### Code style

This project uses [ktlint](https://github.com/pinterest/ktlint)

### WebClip bridge regression checkpoint

WebClip restore preserves the existing WebView/document without reloading.
`_mw_` and `MixinContext` are stable per `MixinWebView`; restoring a clip
atomically rebinds both interfaces to the new `WebFragment`. Lifecycle teardown
removes every host callback before screenshot capture, while an owner token
prevents queued work from reaching either an old or superseding Fragment.
Detached wallet requests fail closed with error 4100. Existing permission,
address, network and signing validation remain in the protocol implementation.
Wallet results retain their request id and normalized Ethereum/Solana network;
queued success drops are rejected through the original provider namespace,
and signing results retain their network while the same owner remains active,
without deriving context from generated scripts.
Unknown network names are rejected through a fixed supported namespace and are
never interpolated into JavaScript.

`WebClipBridgeLifecycleTest` covers the original retained-`MixinContext` defect.
`WebViewBridgeBindingTest` covers retained Java interface identity routing to a
new host, detached failure, stale queued owner callbacks, per-WebView isolation,
queued-success rejection, mixed Ethereum/Solana routing, metadata rebind, and
the production `WebFragment` bind/reuse path. Its WebViews are attached to a
Robolectric Activity so real `View.post` work is executable. These are
Robolectric/JVM lifecycle regressions, not claims about actual JavaScript,
Promise settlement, DOM/scroll preservation, or a device WebView.

Run with JDK 17:

```shell
./gradlew :app:testGooglePlayDebugUnitTest --tests 'one.mixin.android.ui.web.*' \
  :app:compileGooglePlayDebugKotlin -Pksp.incremental=false --console=plain
```

On 2026-10-10 the baseline regression failed with old-owner calls expected `1`,
actual `2`; after the fix, Kotlin compilation and all 67 selected Web/Web3 JVM
tests passed. The broader verification command also adds
`--tests 'one.mixin.android.web3.*' --tests 'one.mixin.android.ui.home.web3.BrowserWallet*'`.

Known boundary: if a signing dialog is already open when its owner is detached,
late completion is discarded rather than sent through a different owner. The
original Promise may remain pending; migrating/cancelling in-flight signing is
not covered by this fix for new requests after restore.

Device verification must cover repeated float/restore, isolated windows,
detached/late calls, and unchanged form/scroll/provider state without reload.
Use a local test page and reject signing prompts; do not send real transactions.
No instrumentation, device, JavaScript provider, or Futures end-to-end
verification has been run.

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
