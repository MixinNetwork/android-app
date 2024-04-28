package one.mixin.android.tip.wc.internal

import com.walletconnect.web3.wallet.client.Wallet
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.extension.defaultSharedPreferences

sealed class Chain(
    val assetId: String,
    val chainNamespace: String,
    val chainReference: Int,
    val name: String,
    val symbol: String,
    private val rpcServers: List<String>,
) {
    object Ethereum : Chain(Constants.ChainId.ETHEREUM_CHAIN_ID, "eip155", 1, "Ethereum Mainnet", "ETH", listOf("https://eth.llamarpc.com"))

    object Arbitrum : Chain(Constants.ChainId.ETHEREUM_CHAIN_ID, "eip155", 0xa4b1, "Arbitrum One", "ETH", listOf("https://arbitrum.llamarpc.com"))

    object Optimism : Chain(Constants.ChainId.ETHEREUM_CHAIN_ID, "eip155", 0xa, "OP Mainnet", "ETH", listOf("https://optimism.llamarpc.com"))

    object Base : Chain(Constants.ChainId.ETHEREUM_CHAIN_ID, "eip155", 0x2105, "Base", "ETH", listOf("https://base.llamarpc.com"))

    object BinanceSmartChain : Chain(Constants.ChainId.BinanceSmartChain, "eip155", 56, "Binance Smart Chain Mainnet", "BNB", listOf("https://bsc-dataseed4.ninicoin.io"))

    object Polygon : Chain(Constants.ChainId.Polygon, "eip155", 137, "Polygon Mainnet", "MATIC", listOf("https://polygon-rpc.com"))

    object Avalanche : Chain(Constants.ChainId.Avalanche, "eip155", 0xa86a, "Avalanche", "AVAX", listOf("https://avalanche.drpc.org"))

    val hexReference:String
        get() {
            return "0x%x".format(chainReference)
        }

    val chainId: String
        get() {
            return "$chainNamespace:$chainReference"
        }
    val rpcUrl: String
        get() {
            return MixinApplication.appContext.defaultSharedPreferences.getString(chainId, null) ?: rpcServers.first()
        }
}

internal val supportChainList = listOf(Chain.Ethereum, Chain.Base, Chain.Arbitrum, Chain.Optimism, Chain.BinanceSmartChain, Chain.Polygon, Chain.Avalanche)

internal fun String.getChain(): Chain? {
    return when (this) {
        Chain.Ethereum.chainId -> Chain.Ethereum
        Chain.Base.chainId -> Chain.Ethereum
        Chain.Arbitrum.chainId -> Chain.Ethereum
        Chain.Optimism.chainId -> Chain.Ethereum
        Chain.BinanceSmartChain.chainId -> Chain.BinanceSmartChain
        Chain.Polygon.chainId -> Chain.Polygon
        Chain.Avalanche.chainId -> Chain.Avalanche
        else -> null
    }
}

internal fun Int.getChain(): Chain? {
    return when (this) {
        Chain.Ethereum.chainReference -> Chain.Ethereum
        Chain.Base.chainReference -> Chain.Ethereum
        Chain.Arbitrum.chainReference -> Chain.Ethereum
        Chain.Optimism.chainReference -> Chain.Ethereum
        Chain.BinanceSmartChain.chainReference -> Chain.BinanceSmartChain
        Chain.Polygon.chainReference -> Chain.Polygon
        Chain.Avalanche.chainReference -> Chain.Avalanche
        else -> null
    }
}

internal fun String?.getChainName(): String? {
    if (this == null) return null

    return when (this) {
        Chain.Ethereum.chainId -> Chain.Ethereum.name
        Chain.Base.chainId -> Chain.Base.name
        Chain.Arbitrum.chainId -> Chain.Arbitrum.name
        Chain.Optimism.chainId -> Chain.Optimism.name
        Chain.BinanceSmartChain.chainId -> Chain.BinanceSmartChain.name
        Chain.Polygon.chainId -> Chain.Polygon.name
        Chain.Avalanche.chainId -> Chain.Avalanche.name
        else -> null
    }
}

internal fun String?.getChainSymbol(): String? {
    if (this == null) return null

    return when (this) {
        Chain.Ethereum.chainId -> Chain.Ethereum.symbol
        Chain.Base.chainId -> Chain.Base.symbol
        Chain.Arbitrum.chainId -> Chain.Arbitrum.symbol
        Chain.Optimism.chainId -> Chain.Optimism.symbol
        Chain.BinanceSmartChain.chainId -> Chain.BinanceSmartChain.symbol
        Chain.Polygon.chainId -> Chain.Polygon.symbol
        Chain.Avalanche.chainId -> Chain.Avalanche.symbol
        else -> null
    }
}

val walletConnectChainIdMap =
    mapOf(
        Chain.Ethereum.symbol to Constants.ChainId.ETHEREUM_CHAIN_ID,
        Chain.Base.symbol to Constants.ChainId.ETHEREUM_CHAIN_ID,
        Chain.Arbitrum.symbol to Constants.ChainId.ETHEREUM_CHAIN_ID,
        Chain.Optimism.symbol to Constants.ChainId.ETHEREUM_CHAIN_ID,
        Chain.Polygon.symbol to Constants.ChainId.Polygon,
        Chain.BinanceSmartChain.symbol to Constants.ChainId.BinanceSmartChain,
        Chain.Avalanche.symbol to Constants.ChainId.Avalanche,
    )

fun getSupportedNamespaces(address: String): Map<String, Wallet.Model.Namespace.Session> {
    val chainIds = supportChainList.map { chain -> chain.chainId }
    val accounts = supportChainList.map { chain -> "${chain.chainNamespace}:${chain.chainReference}:$address" }
    return mapOf(
        "eip155" to
            Wallet.Model.Namespace.Session(
                chains = chainIds,
                methods = supportedMethods,
                events = listOf("chainChanged", "accountsChanged"),
                accounts = accounts,
            ),
    )
}
