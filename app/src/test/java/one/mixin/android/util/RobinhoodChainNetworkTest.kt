package one.mixin.android.util

import one.mixin.android.Constants
import org.junit.Assert.assertEquals
import org.junit.Test

class RobinhoodChainNetworkTest {
    @Test
    fun robinhoodFallbacksUseDisplayName() {
        assertEquals("Robinhood", getChainNetwork("token", Constants.ChainId.Robinhood, "asset-key"))
        assertEquals("Robinhood", getChainName(Constants.ChainId.Robinhood, null, "asset-key"))
    }
}
