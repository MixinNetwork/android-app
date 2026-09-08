package one.mixin.android.web3

import one.mixin.android.tip.wc.internal.Chain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XLayerChainIdTest {
    @Test
    fun numericChainIdResolvesToXLayerEvm() {
        assertEquals(196, Web3ChainId.XLayerChainId)
        assertTrue(Web3ChainId.XLayerChainId in Web3ChainId.eip155ChainIds)
        assertEquals(ChainType.ethereum, Web3ChainId.getChainType(Web3ChainId.XLayerChainId))
        assertEquals(Chain.XLayer, Web3ChainId.getChain(Web3ChainId.XLayerChainId))
    }
}
