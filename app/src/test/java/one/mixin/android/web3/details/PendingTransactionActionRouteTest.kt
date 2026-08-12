package one.mixin.android.web3.details

import one.mixin.android.Constants
import org.junit.Assert.assertEquals
import org.junit.Test

class PendingTransactionActionRouteTest {
    @Test
    fun pearlUsesUtxoPendingActions() {
        assertEquals(
            PendingTransactionActionRoute.Utxo,
            pendingTransactionActionRoute(Constants.ChainId.PEARL_CHAIN_ID),
        )
    }

    @Test
    fun bitcoinUsesUtxoPendingActions() {
        assertEquals(
            PendingTransactionActionRoute.Utxo,
            pendingTransactionActionRoute(Constants.ChainId.BITCOIN_CHAIN_ID),
        )
    }

    @Test
    fun ethereumUsesEvmPendingActions() {
        assertEquals(
            PendingTransactionActionRoute.Evm,
            pendingTransactionActionRoute(Constants.ChainId.ETHEREUM_CHAIN_ID),
        )
    }
}
