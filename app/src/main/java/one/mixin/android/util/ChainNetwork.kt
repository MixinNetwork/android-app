package one.mixin.android.util

import androidx.core.text.isDigitsOnly
import one.mixin.android.Constants

private val chainNetworks by lazy {
    mapOf(
        "43d61dcd-e413-450d-80b8-101d5e903357" to "ERC-20",
        "17f78d7c-ed96-40ff-980c-5dc62fecbc85" to "BEP-2",
        "1949e683-6a08-49e2-b087-d6b72398588f" to "BEP-20",
        "6cfe566e-4aad-470b-8c9a-2fd35b49c68d" to "EOS",
        "b7938396-3f94-4e0a-9179-d3440718156f" to "Polygon",
        "64692c23-8971-4cf4-84a7-4dd1271dd887" to "Solana",
    )
}

private val bepChains by lazy {
    arrayOf(
        "17f78d7c-ed96-40ff-980c-5dc62fecbc85",
        "1949e683-6a08-49e2-b087-d6b72398588f",
    )
}

fun getChainNetwork(
    assetId: String,
    chainId: String,
    assetKey: String?,
): String? {
    if (chainId == Constants.ChainId.MixinVirtualMachine) {
        return "MVM"
    }

    if (assetId == chainId && !bepChains.contains(chainId)) return null

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
        "43d61dcd-e413-450d-80b8-101d5e903357" to "Ethereum (ERC-20)",
        "cbc77539-0a20-4666-8c8a-4ded62b36f0a" to "Avalanche X-Chain",
        "17f78d7c-ed96-40ff-980c-5dc62fecbc85" to "BNB Beacon Chain (BEP-2)",
        "1949e683-6a08-49e2-b087-d6b72398588f" to "BNB Smart Chain (BEP-20)",
        "05891083-63d2-4f3d-bfbe-d14d7fb9b25a" to "BitShares",
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
