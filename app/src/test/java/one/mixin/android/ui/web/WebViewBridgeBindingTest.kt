package one.mixin.android.ui.web

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Looper
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.google.mlkit.common.internal.CommonComponentRegistrar
import com.google.mlkit.common.sdkinternal.MlKitContext
import com.google.mlkit.vision.barcode.internal.BarcodeRegistrar
import com.google.mlkit.vision.common.internal.VisionCommonRegistrar
import one.mixin.android.MixinApplication
import one.mixin.android.ui.qr.QRCodeProcessor
import one.mixin.android.vo.App
import one.mixin.android.web3.js.Web3Signer
import one.mixin.android.widget.MixinWebView
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE, sdk = [28])
class WebViewBridgeBindingTest {
    private lateinit var activity: Activity
    private lateinit var webViewContainer: FrameLayout
    private lateinit var webView: MixinWebView
    private lateinit var previousEvmAddress: String
    private lateinit var previousSolanaAddress: String
    private lateinit var previousNetwork: String

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        MixinApplication.appContext = context
        MlKitContext.initializeIfNeeded(
            context,
            listOf(CommonComponentRegistrar(), VisionCommonRegistrar(), BarcodeRegistrar()),
        )
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        webViewContainer = FrameLayout(activity)
        webView = MixinWebView(activity)
        webViewContainer.addView(webView)
        activity.setContentView(webViewContainer)
        previousEvmAddress = Web3Signer.evmAddress
        previousSolanaAddress = Web3Signer.solanaAddress
        previousNetwork = Web3Signer.currentNetwork
        Web3Signer.updateAddress(Web3Signer.JsSignerNetwork.Ethereum.name, TEST_ADDRESS)
        Web3Signer.updateAddress(Web3Signer.JsSignerNetwork.Solana.name, TEST_SOLANA_ADDRESS)
        Web3Signer.useEvm()
    }

    @After
    fun tearDown() {
        Web3Signer.updateAddress(Web3Signer.JsSignerNetwork.Ethereum.name, previousEvmAddress)
        Web3Signer.updateAddress(Web3Signer.JsSignerNetwork.Solana.name, previousSolanaAddress)
        if (previousNetwork == Web3Signer.JsSignerNetwork.Solana.name) {
            Web3Signer.useSolana()
        } else {
            Web3Signer.useEvm()
        }
        webView.destroy()
        activity.finish()
    }

    @Test
    fun retainedJavaInterfacesRouteToNewHostAfterRebind() {
        val oldMixinCalls = mutableListOf<String>()
        val oldWalletCalls = mutableListOf<Long>()
        val newMixinCalls = mutableListOf<String>()
        val newWalletCalls = mutableListOf<Long>()
        val binding = webView.bridgeBinding
        val first = binding.bind(metadata()) { host(oldMixinCalls, oldWalletCalls) }
        val retainedMixinContext = binding.mixinContextInterface
        val retainedWallet = binding.walletInterface

        retainedMixinContext.close()
        retainedWallet.postMessage(personalSignRequest(1))
        idleMainLooper()
        assertEquals(listOf("close"), oldMixinCalls)
        assertEquals(listOf(1L), oldWalletCalls)

        binding.detach(first)
        binding.bind(metadata()) { host(newMixinCalls, newWalletCalls) }
        retainedMixinContext.close()
        retainedWallet.postMessage(personalSignRequest(2))
        idleMainLooper()

        assertEquals("The retained Java object must stop routing to the old host", listOf("close"), oldMixinCalls)
        assertEquals(listOf(1L), oldWalletCalls)
        assertEquals(listOf("close"), newMixinCalls)
        assertEquals(listOf(2L), newWalletCalls)
        assertSame(retainedMixinContext, binding.mixinContextInterface)
        assertSame(retainedWallet, binding.walletInterface)
    }

    @Test
    fun detachedWalletRequestFailsClosedWithoutCallingPreviousOwner() {
        val walletCalls = mutableListOf<Long>()
        val detached = mutableListOf<Pair<Long, String>>()
        val binding = WebViewBridgeBinding(webView) { id, network -> detached += id to network }
        val owner = binding.bind(metadata()) { host(mutableListOf(), walletCalls) }
        val retainedWallet = binding.walletInterface

        binding.detach(owner)
        retainedWallet.postMessage(personalSignRequest(3))
        idleMainLooper()

        assertTrue(walletCalls.isEmpty())
        assertEquals(listOf(3L to Web3Signer.JsSignerNetwork.Ethereum.name), detached)
    }

    @Test
    fun queuedCallbackFromSupersededOwnerIsNotDeliveredToEitherHost() {
        val firstWalletCalls = mutableListOf<Long>()
        val secondWalletCalls = mutableListOf<Long>()
        val dropped = mutableListOf<Pair<Long, String>>()
        val binding = WebViewBridgeBinding(webView) { id, network -> dropped += id to network }
        val first = binding.bind(metadata()) { host(mutableListOf(), firstWalletCalls) }

        binding.walletInterface.postMessage(personalSignRequest(4))
        binding.detach(first)
        binding.bind(metadata()) { host(mutableListOf(), secondWalletCalls) }
        idleMainLooper()

        assertTrue(firstWalletCalls.isEmpty())
        assertTrue(secondWalletCalls.isEmpty())
        assertEquals(listOf(4L to Web3Signer.JsSignerNetwork.Ethereum.name), dropped)
    }

    @Test
    fun lateDetachAndQueuedMixinCallbackCannotAffectNewOwner() {
        val firstCalls = mutableListOf<String>()
        val secondCalls = mutableListOf<String>()
        val binding = webView.bridgeBinding
        val first = binding.bind(metadata()) { host(firstCalls, mutableListOf()) }

        binding.mixinContextInterface.close()
        val second = binding.bind(metadata()) { host(secondCalls, mutableListOf()) }
        binding.detach(first)
        idleMainLooper()

        assertTrue(firstCalls.isEmpty())
        assertTrue(secondCalls.isEmpty())
        assertTrue(binding.isCurrent(second))
    }

    @Test
    fun detachedMixinContextDisablesEveryHostCapability() {
        val calls = mutableListOf<String>()
        val binding = webView.bridgeBinding
        val owner =
            binding.bind(metadata()) {
                WebViewBridgeBinding.Host(
                    mixin =
                        WebViewBridgeBinding.MixinHost(
                            reloadThemeAction = { calls += "theme" },
                            playlistAction = { calls += "playlist" },
                            closeAction = { calls += "close" },
                            getTipAddressAction = { _, _ -> calls += "tip-address" },
                            tipSignAction = { _, _, _ -> calls += "tip-sign" },
                            getAssetAction = { _, _ -> calls += "assets" },
                            signBotSignature = { _, _, _, _, _, _ -> calls += "bot-sign" },
                            openInBrowserAction = { calls += "browser"; true },
                            verifyPinAction = { calls += "pin" },
                        ),
                    wallet = WebViewBridgeBinding.WalletHost(),
                )
            }
        val retained = binding.mixinContextInterface

        binding.detach(owner)
        retained.reloadTheme()
        retained.playlist(arrayOf("track"))
        retained.close()
        retained.getTipAddress("chain", "callback")
        retained.tipSign("chain", "message", "callback")
        retained.getAssets(emptyArray(), "callback")
        retained.signBotSignature(arrayOf("app", "false", "GET", "/", "", "callback"))
        assertFalse(retained.openInBrowser("https://example.com"))
        retained.verifyPin("callback")
        idleMainLooper()

        assertTrue(calls.isEmpty())
    }

    @Test
    fun ownersAreIsolatedPerWebView() {
        val otherWebView = MixinWebView(activity)
        val firstCalls = mutableListOf<Long>()
        val secondCalls = mutableListOf<Long>()
        try {
            webViewContainer.addView(otherWebView)
            webView.bridgeBinding.bind(metadata()) { host(mutableListOf(), firstCalls) }
            otherWebView.bridgeBinding.bind(metadata()) { host(mutableListOf(), secondCalls) }

            webView.bridgeBinding.walletInterface.postMessage(personalSignRequest(5))
            otherWebView.bridgeBinding.walletInterface.postMessage(personalSignRequest(6))
            idleMainLooper()

            assertEquals(listOf(5L), firstCalls)
            assertEquals(listOf(6L), secondCalls)
            assertNotSame(webView.bridgeBinding.walletInterface, otherWebView.bridgeBinding.walletInterface)
        } finally {
            otherWebView.destroy()
        }
    }

    @Test
    fun queuedSuccessRejectedForOriginalRequestAfterOwnerChanges() {
        val successes = mutableListOf<Triple<Long, String, String>>()
        val dropped = mutableListOf<Pair<Long, String>>()
        val binding = WebViewBridgeBinding(webView) { id, network -> dropped += id to network }
        val first =
            binding.bind(metadata()) {
                WebViewBridgeBinding.Host(
                    mixin = WebViewBridgeBinding.MixinHost(),
                    wallet =
                        WebViewBridgeBinding.WalletHost(
                            onWalletActionSuccessful = { id, network, script -> successes += Triple(id, network, script) },
                        ),
                )
            }

        binding.walletInterface.postMessage(requestAccountsRequest(7, "ethereum"))
        binding.detach(first)
        binding.bind(metadata()) { host(mutableListOf(), mutableListOf()) }
        idleMainLooper()

        assertTrue(successes.isEmpty())
        assertEquals(listOf(7L to Web3Signer.JsSignerNetwork.Ethereum.name), dropped)
    }

    @Test
    fun mixedNetworkRequestsKeepTheirNormalizedNetworkWhileQueued() {
        val errors = mutableListOf<Pair<Long, String>>()
        val otherWebView = MixinWebView(activity)
        try {
            webViewContainer.addView(otherWebView)
            val walletHost =
                WebViewBridgeBinding.WalletHost(
                    onWalletActionError = { id, network, _, _ -> errors += id to network },
                )
            webView.bridgeBinding.bind(metadata()) {
                WebViewBridgeBinding.Host(WebViewBridgeBinding.MixinHost(), walletHost)
            }
            otherWebView.bridgeBinding.bind(metadata()) {
                WebViewBridgeBinding.Host(WebViewBridgeBinding.MixinHost(), walletHost)
            }

            webView.bridgeBinding.walletInterface.postMessage(unsupportedRequest(8, "EtHeReUm"))
            otherWebView.bridgeBinding.walletInterface.postMessage(unsupportedRequest(9, "SoLaNa"))
            idleMainLooper()

            assertEquals(
                listOf(
                    8L to Web3Signer.JsSignerNetwork.Ethereum.name,
                    9L to Web3Signer.JsSignerNetwork.Solana.name,
                ),
                errors,
            )
        } finally {
            otherWebView.destroy()
        }
    }

    @Test
    fun mixedNetworkSigningRequestsCarryTheirPinnedNetwork() {
        val signs = mutableListOf<Pair<Long, String?>>()
        webView.bridgeBinding.bind(metadata()) {
            WebViewBridgeBinding.Host(
                mixin = WebViewBridgeBinding.MixinHost(),
                wallet =
                    WebViewBridgeBinding.WalletHost(
                        onBrowserSign = { signs += it.callbackId to it.network },
                    ),
            )
        }

        webView.bridgeBinding.walletInterface.postMessage(personalSignRequest(11))
        webView.bridgeBinding.walletInterface.postMessage(solanaSignRequest(12))
        idleMainLooper()

        assertEquals(
            listOf(
                11L to Web3Signer.JsSignerNetwork.Ethereum.name,
                12L to Web3Signer.JsSignerNetwork.Solana.name,
            ),
            signs,
        )
    }

    @Test
    fun unknownNetworkIsRejectedThroughAConstantSafeNamespace() {
        val errors = mutableListOf<Pair<Long, String>>()
        webView.bridgeBinding.bind(metadata()) {
            WebViewBridgeBinding.Host(
                mixin = WebViewBridgeBinding.MixinHost(),
                wallet =
                    WebViewBridgeBinding.WalletHost(
                        onWalletActionError = { id, network, _, _ -> errors += id to network },
                    ),
            )
        }

        webView.bridgeBinding.walletInterface.postMessage(unsupportedRequest(10, "ethereum;attack()"))
        idleMainLooper()

        assertEquals(listOf(10L to Web3Signer.JsSignerNetwork.Ethereum.name), errors)
    }

    @Test
    fun ownerIsNotPublishedUntilTokenAwareHostIsConstructed() {
        val binding = webView.bridgeBinding
        var reachableDuringConstruction = true
        val token =
            binding.bind(metadata()) { ownerToken ->
                reachableDuringConstruction = binding.mixinContextInterface.openInBrowser("https://example.com")
                WebViewBridgeBinding.Host(
                    mixin = WebViewBridgeBinding.MixinHost(openInBrowserAction = { binding.isCurrent(ownerToken) }),
                    wallet = WebViewBridgeBinding.WalletHost(),
                )
            }

        assertFalse(reachableDuringConstruction)
        assertTrue(binding.isCurrent(token))
        assertTrue(binding.mixinContextInterface.openInBrowser("https://example.com"))
    }

    @Test
    fun retainedMixinContextReadsMetadataFromLatestBinding() {
        val previousAppContext = MixinApplication.appContext
        MixinApplication.appContext =
            object : MixinApplication() {
                override fun getResources() = previousAppContext.resources
                override fun getSharedPreferences(name: String, mode: Int) = previousAppContext.getSharedPreferences(name, mode)
            }
        try {
            val binding = webView.bridgeBinding
            binding.bind(WebViewBridgeBinding.Metadata("first", immersive = false)) {
                host(mutableListOf(), mutableListOf())
            }
            val retained = binding.mixinContextInterface

            binding.bind(WebViewBridgeBinding.Metadata("second", immersive = true)) {
                host(mutableListOf(), mutableListOf())
            }
            val context = JSONObject(requireNotNull(retained.getContext()))

            assertEquals("second", context.getString("conversation_id"))
            assertTrue(context.getBoolean("immersive"))
        } finally {
            MixinApplication.appContext = previousAppContext
        }
    }

    @Test
    fun webFragmentInstallsAndReusesHolderWithoutReloadingDocument() {
        val firstFragment = fragment()
        val secondFragment = fragment()
        try {
            setWebView(firstFragment, webView)
            setWebView(secondFragment, webView)

            invokeBridgeMethod(firstFragment, "bindWebViewBridges", false)
            val firstToken = ownerToken(firstFragment)
            val retainedMixinContext = webView.bridgeBinding.mixinContextInterface
            val retainedWallet = webView.bridgeBinding.walletInterface
            webView.loadUrl(EXISTING_URL)
            val shadowWebView = shadowOf(webView)
            val reloadsBeforeRestore = shadowWebView.reloadInvocations
            setField(firstFragment, "reusingWebDocument", true)
            invokeBridgeMethod(firstFragment, "loadWebDocument", emptyMap<String, String>())

            assertEquals(reloadsBeforeRestore, shadowWebView.reloadInvocations)
            assertEquals(EXISTING_URL, shadowWebView.lastLoadedUrl)
            invokeBridgeMethod(firstFragment, "detachWebViewBridges")
            invokeBridgeMethod(secondFragment, "bindWebViewBridges", false)
            val secondToken = ownerToken(secondFragment)

            assertTrue(webView.bridgeBinding.isInstalled)
            assertFalse(webView.bridgeBinding.isCurrent(firstToken))
            assertTrue(webView.bridgeBinding.isCurrent(secondToken))
            assertNotSame(firstToken, secondToken)
            assertSame(retainedMixinContext, webView.bridgeBinding.mixinContextInterface)
            assertSame(retainedWallet, webView.bridgeBinding.walletInterface)
        } finally {
            closeProcessor(firstFragment)
            closeProcessor(secondFragment)
        }
    }

    @Test
    fun generateWebClipDetachesTheInstalledHolderBeforeKeepingTheWebView() {
        val fragment = fragment()
        try {
            ReflectionHelpers.setField(webView, "mLeft", 0)
            ReflectionHelpers.setField(webView, "mTop", 0)
            ReflectionHelpers.setField(webView, "mRight", 100)
            ReflectionHelpers.setField(webView, "mBottom", 100)
            setWebView(fragment, webView)
            setField(fragment, "app", BOT_APP)
            invokeBridgeMethod(fragment, "bindWebViewBridges", false)
            val token = ownerToken(fragment)
            val retainedMixinContext = webView.bridgeBinding.mixinContextInterface

            val clip =
                WebFragment::class.java.getDeclaredMethod("generateWebClip").run {
                    isAccessible = true
                    invoke(fragment) as WebClip?
                }

            assertNotNull(clip)
            assertSame(webView, clip?.webView)
            assertFalse(webView.bridgeBinding.isCurrent(token))
            retainedMixinContext.close()
            idleMainLooper()
            assertFalse(webView.bridgeBinding.isCurrent(token))
        } finally {
            closeProcessor(fragment)
        }
    }

    private fun metadata() = WebViewBridgeBinding.Metadata("conversation", immersive = false)

    private fun host(
        mixinCalls: MutableList<String>,
        walletCalls: MutableList<Long>,
    ) =
        WebViewBridgeBinding.Host(
            mixin =
                WebViewBridgeBinding.MixinHost(
                    closeAction = { mixinCalls += "close" },
                ),
            wallet =
                WebViewBridgeBinding.WalletHost(
                    onBrowserSign = { walletCalls += it.callbackId },
                ),
        )

    private fun personalSignRequest(id: Long) =
        """{"id":$id,"network":"ethereum","name":"signPersonalMessage","object":{"address":"$TEST_ADDRESS","data":"0x01"}}"""

    private fun requestAccountsRequest(
        id: Long,
        network: String,
    ) = """{"id":$id,"network":"$network","name":"requestAccounts"}"""

    private fun unsupportedRequest(
        id: Long,
        network: String,
    ) = """{"id":$id,"network":"$network","name":"requestPermissions"}"""

    private fun solanaSignRequest(id: Long) =
        """{"id":$id,"network":"solana","name":"signMessage","object":{"data":"0x01"}}"""

    private fun idleMainLooper() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun fragment() =
        WebFragment.newInstance(
            Bundle().apply { putString(WebFragment.URL, "https://example.com") },
        )

    private fun setWebView(fragment: WebFragment, value: MixinWebView) {
        setField(fragment, "webView", value)
    }

    private fun setField(
        fragment: WebFragment,
        name: String,
        value: Any,
    ) {
        WebFragment::class.java.getDeclaredField(name).apply {
            isAccessible = true
            set(fragment, value)
        }
    }

    private fun invokeBridgeMethod(
        fragment: WebFragment,
        name: String,
        vararg args: Any,
    ) {
        WebFragment::class.java.declaredMethods.single { it.name == name }.apply {
            isAccessible = true
            invoke(fragment, *args)
        }
    }

    private fun ownerToken(fragment: WebFragment): WebViewBridgeBinding.OwnerToken =
        WebFragment::class.java.getDeclaredField("bridgeOwnerToken").run {
            isAccessible = true
            get(fragment) as WebViewBridgeBinding.OwnerToken
        }

    private fun closeProcessor(fragment: WebFragment) {
        WebFragment::class.java.getDeclaredField("processor").run {
            isAccessible = true
            (get(fragment) as QRCodeProcessor).close()
        }
    }

    private companion object {
        const val TEST_ADDRESS = "0x1111111111111111111111111111111111111111"
        const val TEST_SOLANA_ADDRESS = "11111111111111111111111111111111"
        const val EXISTING_URL = "https://example.com/stateful-document"
        val BOT_APP =
            App(
                appId = "bridge-test",
                appNumber = "70001001000",
                homeUri = "https://example.com",
                redirectUri = "",
                name = "Bridge test",
                iconUrl = "",
                category = null,
                description = "",
                appSecret = "",
                capabilities = null,
                creatorId = "bridge-test",
                resourcePatterns = null,
                updatedAt = null,
            )
    }
}
