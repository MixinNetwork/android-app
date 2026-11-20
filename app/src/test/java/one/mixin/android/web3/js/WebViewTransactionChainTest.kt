package one.mixin.android.web3.js

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import one.mixin.android.MixinApplication
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.ui.web.WebFragment.Web3Interface
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WebViewTransactionChainTest {
    @Test
    fun transactionUsesThePageNetworkAfterAnotherPageUsedArbitrum() {
        MixinApplication.appContext = ApplicationProvider.getApplicationContext<Context>()
        val sender = "0x1111111111111111111111111111111111111111"
        Web3Signer.updateAddress("ethereum", sender)
        Web3Signer.switchChain(SwitchChain(Chain.Arbitrum.hexReference)).getOrThrow()
        val transactions = mutableListOf<JsSignMessage>()
        val bridge = Web3Interface({}, { _, _, message -> error(message) }, { transactions.add(it) }, { error(it) })
        val request = JSONObject()
            .put("id", 1)
            .put("name", "signTransaction")
            .put("network", "ethereum")
            .put("object", JSONObject().put("from", sender).put("to", sender))

        for (chain in listOf(Chain.Ethereum, Chain.Arbitrum)) {
            bridge.postMessage(request.put("chainId", chain.hexReference).toString())
            assertEquals(chain, Web3Signer.currentChain)
        }
        assertEquals(listOf(Chain.Ethereum, Chain.Arbitrum), transactions.map { it.transactionChain })
    }
}
