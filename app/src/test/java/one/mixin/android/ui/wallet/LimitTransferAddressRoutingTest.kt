package one.mixin.android.ui.wallet

import one.mixin.android.Constants
import kotlin.test.Test
import kotlin.test.assertEquals

class LimitTransferAddressRoutingTest {
    @Test
    fun pearlUsesUtxoSenderAddressRoute() {
        assertEquals(
            LimitSenderAddressRoute.UTXO,
            resolveLimitSenderAddressRoute(Constants.ChainId.PEARL_CHAIN_ID),
        )
    }
}
