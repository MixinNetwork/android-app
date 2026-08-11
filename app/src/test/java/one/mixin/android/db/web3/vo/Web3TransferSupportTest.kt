package one.mixin.android.db.web3.vo

import one.mixin.android.Constants
import org.junit.Assert.assertTrue
import org.junit.Test

class Web3TransferSupportTest {
    @Test
    fun bitcoinTransferRemainsSupported() {
        assertTrue(isWeb3TransferSupported(Constants.ChainId.BITCOIN_CHAIN_ID))
    }

    @Test
    fun pearlTransferIsSupported() {
        assertTrue(isWeb3TransferSupported(Constants.ChainId.PEARL_CHAIN_ID))
    }
}
