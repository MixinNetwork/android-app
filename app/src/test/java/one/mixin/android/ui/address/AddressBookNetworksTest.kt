package one.mixin.android.ui.address

import one.mixin.android.Constants
import one.mixin.android.Constants.ChainId
import one.mixin.android.vo.Chain
import one.mixin.android.vo.WalletCategory
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
        assertEquals(listOf(ChainId.ETHEREUM_CHAIN_ID, "avax", "ton"), networks.map { it.chainId })
        assertEquals("Ethereum", networks.first().name)
    }

    @Test
    fun selectedPrivacyWalletChainBecomesDefault() {
        val networks = addressBookNetworks(listOf(Chain("ton", "TON", "TON", "", 1)))
        assertEquals("ton", defaultAddressBookNetwork(listOf("ton"), networks).chainId)
    }

    @Test
    fun commonWalletKeepsOnlySupportedNetworks() {
        val chains = listOf(
            Chain("ton", "TON", "TON", "", 1),
            Chain("avax", "Avalanche X-Chain", "AVAX", "", 1),
        )
        val networks = addressBookNetworks(chains, setOf(ChainId.SOLANA_CHAIN_ID, "ton"))
        assertEquals(listOf(ChainId.SOLANA_CHAIN_ID, "ton"), networks.map { it.chainId })
    }

    @Test
    fun walletWithoutEthereumDefaultsToFirstNetwork() {
        val networks = addressBookNetworks(emptyList(), setOf(ChainId.TRON_CHAIN_ID, ChainId.SOLANA_CHAIN_ID))
        assertEquals(ChainId.SOLANA_CHAIN_ID, defaultAddressBookNetwork(emptyList(), networks).chainId)
    }

    @Test
    fun commonWalletSupportsAllWeb3ChainsWithoutAddresses() {
        val expected = (Constants.Web3EvmChainIds + ChainId.SOLANA_CHAIN_ID + Constants.Web3UtxoChainIds).toSet()
        listOf(WalletCategory.CLASSIC, WalletCategory.IMPORTED_MNEMONIC).forEach { category ->
            val supported = addressBookWalletChainIds(category.value, emptySet())
            assertEquals(expected, supported)
            assertEquals(expected, addressBookNetworks(emptyList(), supported).map { it.chainId }.toSet())
            assertEquals(supported, addressBookWalletChainIds(category.value, setOf(ChainId.ETHEREUM_CHAIN_ID)))
        }
    }

    @Test
    fun importedAndWatchEvmWalletsSupportNetworksWithoutSeparateAddresses() {
        listOf(WalletCategory.IMPORTED_PRIVATE_KEY, WalletCategory.WATCH_ADDRESS).forEach { category ->
            assertEquals(
                Constants.Web3EvmChainIds.toSet(),
                addressBookWalletChainIds(category.value, setOf(ChainId.ETHEREUM_CHAIN_ID)),
            )
        }
    }

    @Test
    fun importedAndWatchSingleChainWalletsKeepTheirNetwork() {
        listOf(WalletCategory.IMPORTED_PRIVATE_KEY, WalletCategory.WATCH_ADDRESS).forEach { category ->
            listOf(ChainId.SOLANA_CHAIN_ID, ChainId.BITCOIN_CHAIN_ID, ChainId.Litecoin, ChainId.PEARL_CHAIN_ID).forEach { chainId ->
                val supported = addressBookWalletChainIds(category.value, setOf(chainId))
                assertEquals(setOf(chainId), supported)
                assertEquals(listOf(chainId), addressBookNetworks(emptyList(), supported).map { it.chainId })
            }
        }
    }

    @Test
    fun walletsWithoutKnownCapabilitiesDoNotFallBackToPrivacyNetworks() {
        listOf(WalletCategory.IMPORTED_PRIVATE_KEY.value, WalletCategory.WATCH_ADDRESS.value, null).forEach { category ->
            val supported = addressBookWalletChainIds(category, emptySet())
            assertEquals(emptySet<String>(), supported)
            assertEquals(emptyList<Any>(), addressBookNetworks(emptyList(), supported))
        }
    }

    @Test
    fun safeWalletUsesItsConfiguredChainWithoutAddresses() {
        assertEquals(
            setOf(ChainId.Polygon),
            addressBookWalletChainIds(WalletCategory.MIXIN_SAFE.value, emptySet(), ChainId.Polygon),
        )
    }

    @Test
    fun privacyNetworksAreEmptyWhenChainsAreEmpty() {
        assertEquals(emptyList<Any>(), addressBookNetworks(emptyList()))
    }

    @Test
    fun privacyNetworksKeepUnpinnedChainsOutsideCommonWalletSupport() {
        val chains = listOf(
            Chain("ton", "TON", "TON", "", 1),
            Chain(ChainId.BITCOIN_CHAIN_ID, "Server Bitcoin", "BTC", "", 1),
            Chain("avax", "Avalanche X-Chain", "AVAX", "", 1),
        )
        val networks = addressBookNetworks(chains, null)
        assertEquals(listOf(ChainId.BITCOIN_CHAIN_ID, "avax", "ton"), networks.map { it.chainId })
        assertEquals("Bitcoin", networks.first().name)
        val supported = addressBookWalletChainIds(WalletCategory.CLASSIC.value, emptySet())
        assertEquals(emptyList<Any>(), addressBookNetworks(chains, supported).filter { it.chainId == "ton" || it.chainId == "avax" })
    }

    @Test
    fun walletNetworkFallbackKeepsServerNamesAndAvoidsDuplicates() {
        val supported = addressBookWalletChainIds(WalletCategory.CLASSIC.value, emptySet())
        val chains = listOf(Chain(ChainId.Base, "Base Mainnet", "ETH", "", 1))
        val networks = addressBookNetworks(chains, supported)
        assertEquals("Base Mainnet", networks.single { it.chainId == ChainId.Base }.name)
        assertEquals(networks.size, networks.map { it.chainId }.toSet().size)
    }
}
