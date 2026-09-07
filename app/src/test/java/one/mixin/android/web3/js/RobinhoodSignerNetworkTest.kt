package one.mixin.android.web3.js

import one.mixin.android.tip.wc.internal.Chain
import org.junit.Assert.assertEquals
import org.junit.Test

class RobinhoodSignerNetworkTest {
    @Test
    fun robinhoodHexReferenceResolvesForPersistedChainState() {
        assertEquals(Chain.Robinhood, findChainByHexReference("0x1237"))
    }
}
