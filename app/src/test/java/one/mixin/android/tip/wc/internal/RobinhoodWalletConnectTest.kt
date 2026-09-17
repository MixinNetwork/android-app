package one.mixin.android.tip.wc.internal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RobinhoodWalletConnectTest {
    @Test
    fun robinhoodUsesExpectedWalletConnectMetadata() {
        assertEquals("b304e03d-d004-3102-875b-8266f8407a1a", Chain.Robinhood.assetId)
        assertEquals("eip155:4663", Chain.Robinhood.chainId)
        assertEquals("0x1237", Chain.Robinhood.hexReference)
        assertEquals("Robinhood", Chain.Robinhood.name)
        assertEquals("ETH", Chain.Robinhood.symbol)
        assertEquals(Chain.Robinhood.assetId, Chain.Robinhood.getWeb3ChainId())
        assertEquals(Chain.Robinhood, getChainByChainId("eip155:4663"))
        assertEquals(Chain.Robinhood, "4663".getChain())
        assertTrue(Chain.Robinhood in evmChainList)
    }
}
