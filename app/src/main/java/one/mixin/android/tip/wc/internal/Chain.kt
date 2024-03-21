package one.mixin.android.tip.wc.internal

import com.walletconnect.web3.wallet.client.Wallet
import one.mixin.android.Constants

sealed class Chain(
    val chainNamespace: String,
    val chainReference: Int,
    val name: String,
    val symbol: String,
    val rpcServers: List<String>,
    val chainId: String = "$chainNamespace:$chainReference",
) {
    object Ethereum : Chain("eip155", 1, "Ethereum Mainnet", "ETH", listOf("https://cloudflare-eth.com"))

    object BinanceSmartChain : Chain("eip155", 56, "Binance Smart Chain Mainnet", "BNB", listOf("https://bsc-dataseed4.ninicoin.io"))

    object Polygon : Chain("eip155", 137, "Polygon Mainnet", "MATIC", listOf("https://polygon-rpc.com"))
}

internal val supportChainList = listOf(Chain.Ethereum, Chain.BinanceSmartChain, Chain.Polygon)

internal fun String.getChain(): Chain? {
    return when (this) {
        Chain.Ethereum.chainId -> Chain.Ethereum
        Chain.BinanceSmartChain.chainId -> Chain.BinanceSmartChain
        Chain.Polygon.chainId -> Chain.Polygon
        else -> null
    }
}

internal fun Int.getChain(): Chain? {
    return when (this) {
        Chain.Ethereum.chainReference -> Chain.Ethereum
        Chain.BinanceSmartChain.chainReference -> Chain.BinanceSmartChain
        Chain.Polygon.chainReference -> Chain.Polygon
        else -> null
    }
}

internal fun String?.getChainName(): String? {
    if (this == null) return null

    return when (this) {
        Chain.Ethereum.chainId -> Chain.Ethereum.name
        Chain.BinanceSmartChain.chainId -> Chain.BinanceSmartChain.name
        Chain.Polygon.chainId -> Chain.Polygon.name
        else -> null
    }
}

internal fun String?.getChainSymbol(): String? {
    if (this == null) return null

    return when (this) {
        Chain.Ethereum.chainId -> Chain.Ethereum.symbol
        Chain.BinanceSmartChain.chainId -> Chain.BinanceSmartChain.symbol
        Chain.Polygon.chainId -> Chain.Polygon.symbol
        else -> null
    }
}

val walletConnectChainIdMap =
    mapOf(
        Chain.Ethereum.symbol to Constants.ChainId.ETHEREUM_CHAIN_ID,
        Chain.Polygon.symbol to Constants.ChainId.Polygon,
        Chain.BinanceSmartChain.symbol to Constants.ChainId.BinanceSmartChain,
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
