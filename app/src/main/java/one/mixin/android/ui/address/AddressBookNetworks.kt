package one.mixin.android.ui.address

import one.mixin.android.Constants
import one.mixin.android.Constants.ChainId
import one.mixin.android.tip.wc.internal.evmChainList
import one.mixin.android.ui.wallet.network.NetworkOption
import one.mixin.android.vo.Chain
import one.mixin.android.vo.WalletCategory

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

internal fun addressBookWalletChainIds(
    category: String?,
    addressChainIds: Set<String>,
    safeChainId: String? = null,
): Set<String> = when (category) {
    WalletCategory.CLASSIC.value, WalletCategory.IMPORTED_MNEMONIC.value ->
        (Constants.Web3EvmChainIds + ChainId.SOLANA_CHAIN_ID + Constants.Web3UtxoChainIds).toSet()
    WalletCategory.IMPORTED_PRIVATE_KEY.value, WalletCategory.WATCH_ADDRESS.value ->
        if (addressChainIds.any { it in Constants.Web3EvmChainIds }) {
            addressChainIds + Constants.Web3EvmChainIds
        } else {
            addressChainIds
        }
    WalletCategory.MIXIN_SAFE.value -> setOfNotNull(safeChainId)
    else -> emptySet()
}

// Pinned networks keep the design order; the remaining privacy wallet chains follow by name.
// Common wallets pass their chain ids to keep only the networks they support; null keeps all.
fun addressBookNetworks(chains: List<Chain>, supportedChainIds: Set<String>? = null): List<NetworkOption> {
    val pinnedChainIds = addressBookNetworks.mapTo(HashSet()) { it.chainId }
    val walletNetworks = if (supportedChainIds == null) emptyList() else {
        evmChainList.map { NetworkOption(it.assetId, it.name) } + NetworkOption(ChainId.PEARL_CHAIN_ID, "Pearl")
    }
    val pinnedNetworks = if (supportedChainIds == null) {
        val chainIds = chains.mapTo(HashSet()) { it.chainId }
        addressBookNetworks.filter { it.chainId in chainIds }
    } else {
        addressBookNetworks
    }
    val networks = pinnedNetworks + (chains.map { NetworkOption(it.chainId, it.name) } + walletNetworks)
        .distinctBy { it.chainId }
        .filter { it.chainId !in pinnedChainIds }
        .sortedBy { it.name.lowercase() }
    return if (supportedChainIds == null) networks else networks.filter { it.chainId in supportedChainIds }
}

// Falls back to Ethereum, or the first network when the wallet does not support Ethereum.
fun defaultAddressBookNetwork(
    selectedChainIds: List<String>,
    networks: List<NetworkOption> = addressBookNetworks,
): NetworkOption =
    networks.firstOrNull { it.chainId in selectedChainIds }
        ?: networks.firstOrNull { it.chainId == ChainId.ETHEREUM_CHAIN_ID }
        ?: networks.firstOrNull()
        ?: addressBookNetworks.first { it.chainId == ChainId.ETHEREUM_CHAIN_ID }
