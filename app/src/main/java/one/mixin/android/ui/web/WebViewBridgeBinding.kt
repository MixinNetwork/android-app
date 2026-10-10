package one.mixin.android.ui.web

import org.json.JSONObject
import one.mixin.android.web3.js.JsSignMessage
import one.mixin.android.web3.js.WalletErrorCode
import one.mixin.android.widget.MixinWebView
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/** Stable JavaScript interfaces whose host callbacks follow one [MixinWebView]. */
class WebViewBridgeBinding internal constructor(
    private val webView: MixinWebView,
    private val detachedWalletRequest: ((Long, String) -> Unit)? = null,
) {
    data class Metadata(
        val conversationId: String?,
        val immersive: Boolean,
    )

    data class MixinHost(
        val reloadThemeAction: (() -> Unit)? = null,
        val playlistAction: ((Array<String>) -> Unit)? = null,
        val closeAction: (() -> Unit)? = null,
        val getTipAddressAction: ((String, String) -> Unit)? = null,
        val tipSignAction: ((String, String, String) -> Unit)? = null,
        val getAssetAction: ((Array<String>, String) -> Unit)? = null,
        val signBotSignature: ((String, Boolean, String, String, String, String) -> Unit)? = null,
        val openInBrowserAction: ((String) -> Boolean)? = null,
        val verifyPinAction: ((String) -> Unit)? = null,
    )

    data class WalletHost(
        val onWalletActionSuccessful: ((Long, String, String) -> Unit)? = null,
        val onWalletActionError: ((Long, String, Int, String) -> Unit)? = null,
        val onBrowserSign: ((JsSignMessage) -> Unit)? = null,
        val onEmptyAddress: ((String) -> Unit)? = null,
    )

    data class Host(
        val mixin: MixinHost,
        val wallet: WalletHost,
    )

    class OwnerToken internal constructor(internal val id: Long)

    private data class Owner(
        val token: OwnerToken,
        val host: Host,
    )

    private data class WalletRequestOwner(val token: OwnerToken?)

    private val nextOwnerId = AtomicLong()
    private val owner = AtomicReference<Owner?>()
    private val ownerLock = Any()
    private val walletRequestOwner = ThreadLocal<WalletRequestOwner?>()

    @Volatile
    private var metadata = Metadata(null, immersive = false)

    @Volatile
    private var disposed = false

    val mixinContextInterface =
        WebFragment.WebAppInterface(
            webView.context,
            conversationId = null,
            immersive = false,
            reloadThemeAction = { dispatchMixin { it.reloadThemeAction?.invoke() } },
            playlistAction = { list -> dispatchMixin { it.playlistAction?.invoke(list) } },
            closeAction = { dispatchMixin { it.closeAction?.invoke() } },
            getTipAddressAction = { chainId, callback -> dispatchMixin { it.getTipAddressAction?.invoke(chainId, callback) } },
            tipSignAction = { chainId, message, callback -> dispatchMixin { it.tipSignAction?.invoke(chainId, message, callback) } },
            getAssetAction = { ids, callback -> dispatchMixin { it.getAssetAction?.invoke(ids, callback) } },
            signBotSignature = { appId, reloadPublicKey, method, path, body, callback ->
                dispatchMixin { it.signBotSignature?.invoke(appId, reloadPublicKey, method, path, body, callback) }
            },
            openInBrowserAction = { url -> callMixin { it.openInBrowserAction?.invoke(url) ?: false } ?: false },
            verifyPinAction = { callback -> dispatchMixin { it.verifyPinAction?.invoke(callback) } },
        ).also {
            it.metadataProvider = { metadata }
            it.isHolderManaged = true
        }

    val walletInterface =
        WebFragment.Web3Interface(
            onWalletActionSuccessful = { id, network, script ->
                dispatchWallet(
                    onDropped = { rejectDetached(id, network) },
                ) { it.onWalletActionSuccessful?.invoke(id, network, script) }
            },
            onWalletActionError = { id, network, code, message ->
                dispatchWallet(
                    onDropped = { rejectDetached(id, network) },
                ) { it.onWalletActionError?.invoke(id, network, code, message) }
            },
            onBrowserSign = { message ->
                dispatchWallet(
                    onDropped = { rejectDetached(message.callbackId, requireNotNull(message.network)) },
                ) { it.onBrowserSign?.invoke(message) }
            },
            onEmptyAddress = { network -> dispatchWallet { it.onEmptyAddress?.invoke(network) } },
            isHostAttached = {
                val requestOwner = owner.get().takeUnless { disposed }
                walletRequestOwner.set(WalletRequestOwner(requestOwner?.token))
                requestOwner != null
            },
            onDetachedRequest = ::rejectDetached,
            onRequestFinished = walletRequestOwner::remove,
        )

    val isInstalled: Boolean
        get() = !disposed

    init {
        webView.addJavascriptInterface(mixinContextInterface, MIXIN_CONTEXT_NAME)
        webView.addJavascriptInterface(walletInterface, WALLET_NAME)
    }

    fun bind(
        metadata: Metadata,
        hostFactory: (OwnerToken) -> Host,
    ): OwnerToken =
        synchronized(ownerLock) {
            check(!disposed) { "Cannot bind bridges after the WebView is destroyed" }
            val token = OwnerToken(nextOwnerId.incrementAndGet())
            val host = hostFactory(token)
            this.metadata = metadata
            owner.set(Owner(token, host))
            token
        }

    fun detach(token: OwnerToken) {
        synchronized(ownerLock) {
            val current = owner.get() ?: return
            if (current.token !== token) return
            owner.set(null)
        }
    }

    fun isCurrent(token: OwnerToken): Boolean = owner.get()?.token === token

    fun postIfCurrent(
        token: OwnerToken,
        action: () -> Unit,
    ) {
        webView.post {
            if (!disposed && isCurrent(token)) action()
        }
    }

    internal fun dispose() {
        synchronized(ownerLock) {
            disposed = true
            owner.set(null)
        }
    }

    private fun dispatchMixin(action: (MixinHost) -> Unit) {
        val expectedToken = owner.get()?.token ?: return
        webView.post {
            val current = owner.get()
            if (!disposed && current != null && current.token === expectedToken) action(current.host.mixin)
        }
    }

    private fun <T> callMixin(action: (MixinHost) -> T): T? =
        synchronized(ownerLock) {
            if (disposed) return@synchronized null
            owner.get()?.host?.mixin?.let(action)
        }

    private fun dispatchWallet(
        onDropped: (() -> Unit)? = null,
        action: (WalletHost) -> Unit,
    ) {
        val requestOwner = walletRequestOwner.get()
        val current = owner.get()
        val expectedToken = if (requestOwner == null) current?.token else requestOwner.token
        if (expectedToken == null || current?.token !== expectedToken) {
            onDropped?.invoke()
            return
        }
        webView.post {
            val postedOwner = owner.get()
            if (!disposed && postedOwner != null && postedOwner.token === expectedToken) {
                action(postedOwner.host.wallet)
            } else {
                onDropped?.invoke()
            }
        }
    }

    private fun rejectDetached(
        id: Long,
        network: String,
    ) {
        detachedWalletRequest?.invoke(id, network) ?: webView.post {
            if (!disposed) {
                val message = JSONObject.quote(DETACHED_MESSAGE)
                webView.evaluateJavascript(
                    "mixinwallet.$network.sendError($id, {code: ${WalletErrorCode.UNAUTHORIZED}, message: $message});",
                    null,
                )
            }
        }
    }

    private companion object {
        const val MIXIN_CONTEXT_NAME = "MixinContext"
        const val WALLET_NAME = "_mw_"
        const val DETACHED_MESSAGE = "Wallet UI is not available"
    }
}
