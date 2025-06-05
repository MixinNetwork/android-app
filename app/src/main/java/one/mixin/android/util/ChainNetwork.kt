package one.mixin.android.util

import androidx.core.text.isDigitsOnly
import one.mixin.android.Constants

private val chainNetworks by lazy {
    mapOf(
        Constants.ChainId.ETHEREUM_CHAIN_ID to "ERC-20",
        Constants.ChainId.BinanceBeaconChain to "BEP-2",
        Constants.ChainId.BinanceSmartChain to "BEP-20",
        Constants.ChainId.EOS_CHAIN_ID to "Vaulta",
        Constants.ChainId.Polygon to "Polygon",
        Constants.ChainId.Solana to "Solana",
        Constants.ChainId.Base to "Base",
        Constants.ChainId.LIGHTNING_NETWORK_CHAIN_ID to "Lightning",
    )
}

private val bepChains by lazy {
    arrayOf(
        Constants.ChainId.BinanceBeaconChain,
        Constants.ChainId.BinanceSmartChain,
    )
}

fun getChainNetwork(
    assetId: String,
    chainId: String,
    assetKey: String?,
): String? {
    if (chainId == Constants.ChainId.MixinVirtualMachine) {
        return "MVM"
    } else if (chainId == Constants.ChainId.Base) {
        return "Base"
    }

    if (assetId == chainId && !bepChains.contains(chainId) && assetId != Constants.ChainId.LIGHTNING_NETWORK_CHAIN_ID) return null

    if (chainId == Constants.ChainId.TRON_CHAIN_ID) {
        return if (!assetKey.isNullOrBlank() && assetKey.isDigitsOnly()) {
            "TRC-10"
        } else {
            "TRC-20"
        }
    }
    return chainNetworks[chainId]
}

private val chainNames by lazy {
    mapOf(
        Constants.ChainId.ETHEREUM_CHAIN_ID to "Ethereum (ERC-20)",
        Constants.ChainId.Avalanche to "Avalanche X-Chain",
        Constants.ChainId.BinanceBeaconChain to "BNB Beacon Chain (BEP-2)",
        Constants.ChainId.BitShares to "BitShares",
        Constants.ChainId.BinanceSmartChain to "BNB Smart Chain (BEP-20)",
        Constants.ChainId.LIGHTNING_NETWORK_CHAIN_ID to "Lightning",
    )
}

fun getChainName(
    chainId: String?,
    chainName: String?,
    assetKey: String?,
): String? {
    if (chainId == Constants.ChainId.TRON_CHAIN_ID) {
        return if (!assetKey.isNullOrBlank() && assetKey.isDigitsOnly()) {
            "TRON (TRC-10)"
        } else {
            "TRON (TRC-20)"
        }
    }
    return chainNames[chainId] ?: chainName
}
