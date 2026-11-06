package one.mixin.android.ui.web

internal data class WebBridgePolicy(
    val mixinContext: Boolean,
    val wallet: Boolean,
)

internal fun webBridgePolicy(
    trustedAppUrl: Boolean,
    dappBrowser: Boolean,
): WebBridgePolicy =
    WebBridgePolicy(
        mixinContext = trustedAppUrl,
        wallet = trustedAppUrl || dappBrowser,
    )
