package one.mixin.android.ui.address

import one.mixin.android.Constants.ChainId
import one.mixin.android.vo.Chain
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

    @Test
    fun privacyWalletChainsFollowPinnedNetworksByName() {
        val chains = listOf(
            Chain("ton", "TON", "TON", "", 1),
            Chain(ChainId.ETHEREUM_CHAIN_ID, "Ethereum", "ETH", "", 1),
            Chain("avax", "Avalanche X-Chain", "AVAX", "", 1),
        )
        val networks = addressBookNetworks(chains)
        assertEquals(addressBookNetworks, networks.take(addressBookNetworks.size))
        assertEquals(listOf("avax", "ton"), networks.drop(addressBookNetworks.size).map { it.chainId })
    }

    @Test
    fun selectedPrivacyWalletChainBecomesDefault() {
        val networks = addressBookNetworks(listOf(Chain("ton", "TON", "TON", "", 1)))
        assertEquals("ton", defaultAddressBookNetwork(listOf("ton"), networks).chainId)
    }
}
