package one.mixin.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XLayerChainClassificationTest {
    @Test
    fun xLayerUsesExpectedMixinChainIdAndEvmClassification() {
        assertEquals("37f5a4d1-905f-3b34-8291-c37438c7dcfc", Constants.ChainId.XLayer)
        assertTrue(Constants.ChainId.XLayer in Constants.Web3EvmChainIds)
        assertTrue(Constants.ChainId.XLayer in Constants.Web3ChainIds)
        assertFalse(Constants.ChainId.XLayer in Constants.Web3UtxoChainIds)
    }
}
