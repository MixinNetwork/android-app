package one.mixin.android.ui.address

import one.mixin.android.Constants.ChainId
import one.mixin.android.ui.wallet.network.NetworkOption
import one.mixin.android.vo.Chain

val addressBookNetworks: List<NetworkOption> = listOf(
    NetworkOption(ChainId.BITCOIN_CHAIN_ID, "Bitcoin"),
    NetworkOption(ChainId.LIGHTNING_NETWORK_CHAIN_ID, "Lightning Network"),
    NetworkOption(ChainId.ETHEREUM_CHAIN_ID, "Ethereum"),
    NetworkOption(ChainId.SOLANA_CHAIN_ID, "Solana"),
    NetworkOption(ChainId.Litecoin, "Litecoin"),
    NetworkOption(ChainId.BinanceSmartChain, "BNB Smart Chain"),
    NetworkOption(ChainId.RIPPLE_CHAIN_ID, "Ripple"),
    NetworkOption(ChainId.TRON_CHAIN_ID, "TRON"),
    NetworkOption(ChainId.Dogecoin, "Dogecoin"),
)

// Pinned networks keep the design order; the remaining privacy wallet chains follow by name.
fun addressBookNetworks(chains: List<Chain>): List<NetworkOption> {
    val pinnedChainIds = addressBookNetworks.mapTo(HashSet()) { it.chainId }
    return addressBookNetworks + chains
        .filter { it.chainId !in pinnedChainIds }
        .sortedBy { it.name.lowercase() }
        .map { NetworkOption(it.chainId, it.name) }
}

fun defaultAddressBookNetwork(
    selectedChainIds: List<String>,
    networks: List<NetworkOption> = addressBookNetworks,
): NetworkOption =
    networks.firstOrNull { it.chainId in selectedChainIds }
        ?: addressBookNetworks.first { it.chainId == ChainId.ETHEREUM_CHAIN_ID }
