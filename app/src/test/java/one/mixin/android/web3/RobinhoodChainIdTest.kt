package one.mixin.android.web3

import one.mixin.android.tip.wc.internal.Chain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RobinhoodChainIdTest {
    @Test
    fun numericChainIdResolvesToRobinhoodEvm() {
        assertEquals(4663, Web3ChainId.RobinhoodChainId)
        assertTrue(Web3ChainId.RobinhoodChainId in Web3ChainId.eip155ChainIds)
        assertEquals(ChainType.ethereum, Web3ChainId.getChainType(Web3ChainId.RobinhoodChainId))
        assertEquals(Chain.Robinhood, Web3ChainId.getChain(Web3ChainId.RobinhoodChainId))
    }
}
