package one.mixin.android.ui.address

import one.mixin.android.Constants.ChainId
import one.mixin.android.ui.wallet.network.NetworkOption

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

fun defaultAddressBookNetwork(selectedChainIds: List<String>): NetworkOption =
    addressBookNetworks.firstOrNull { it.chainId in selectedChainIds }
        ?: addressBookNetworks.first { it.chainId == ChainId.ETHEREUM_CHAIN_ID }
