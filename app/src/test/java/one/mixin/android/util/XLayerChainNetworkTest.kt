package one.mixin.android.util

import one.mixin.android.Constants
import org.junit.Assert.assertEquals
import org.junit.Test

class XLayerChainNetworkTest {
    @Test
    fun xLayerFallbacksUseDisplayName() {
        assertEquals("X Layer", getChainNetwork("token", Constants.ChainId.XLayer, "asset-key"))
        assertEquals("X Layer", getChainName(Constants.ChainId.XLayer, null, "asset-key"))
    }
}
