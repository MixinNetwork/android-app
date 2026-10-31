package one.mixin.android.ui.address

import one.mixin.android.Constants.ChainId
import org.junit.Assert.assertEquals
import org.junit.Test

class AddressBookNetworksTest {
    @Test
    fun allNetworksDefaultsToEthereum() {
        assertEquals(ChainId.ETHEREUM_CHAIN_ID, defaultAddressBookNetwork(emptyList()).chainId)
    }

    @Test
    fun singleNetworkKeepsSelection() {
        assertEquals(ChainId.TRON_CHAIN_ID, defaultAddressBookNetwork(listOf(ChainId.TRON_CHAIN_ID)).chainId)
    }

    @Test
    fun multipleNetworksUseDesignOrderInsteadOfSelectionOrder() {
        assertEquals(
            ChainId.BITCOIN_CHAIN_ID,
            defaultAddressBookNetwork(listOf(ChainId.Dogecoin, ChainId.ETHEREUM_CHAIN_ID, ChainId.BITCOIN_CHAIN_ID)).chainId,
        )
    }

    @Test
    fun unknownSelectionFallsBackToEthereum() {
        assertEquals(ChainId.ETHEREUM_CHAIN_ID, defaultAddressBookNetwork(listOf("unknown")).chainId)
    }
}
