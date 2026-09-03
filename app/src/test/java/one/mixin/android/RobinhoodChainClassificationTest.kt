package one.mixin.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RobinhoodChainClassificationTest {
    @Test
    fun robinhoodUsesExpectedMixinChainIdAndEvmClassification() {
        assertEquals("b304e03d-d004-3102-875b-8266f8407a1a", Constants.ChainId.Robinhood)
        assertTrue(Constants.ChainId.Robinhood in Constants.Web3EvmChainIds)
        assertTrue(Constants.ChainId.Robinhood in Constants.Web3ChainIds)
        assertFalse(Constants.ChainId.Robinhood in Constants.Web3UtxoChainIds)
        assertEquals("Robinhood", Constants.AssetId.ethAssets[Constants.ChainId.Robinhood])
    }
}
