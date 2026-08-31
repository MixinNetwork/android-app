package one.mixin.android.web3.js

import one.mixin.android.tip.wc.internal.Chain
import org.junit.Assert.assertEquals
import org.junit.Test

class Web3SignerNetworkTest {
    @Test
    fun pearlUsesStableNetworkName() {
        assertEquals("pearl", Web3Signer.JsSignerNetwork.Pearl.name)
    }

    @Test
    fun xLayerHexReferenceResolvesForPersistedChainState() {
        assertEquals(Chain.XLayer, findChainByHexReference("0xc4"))
    }
}
