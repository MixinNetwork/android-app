package one.mixin.android.tip.wc

sealed class Chain(val chainId: Int, val name: String, val symbol: String, val rpcServers: List<String>) {
    object Ethereum : Chain(1, "Ethereum Mainnet", "ETH", listOf("https://eth.public-rpc.com"))
    object BinanceSmartChain : Chain(56, "Binance Smart Chain Mainnet", "BSC", listOf("https://bsc-dataseed4.ninicoin.io"))
    object Polygon : Chain(137, "Polygon Mainnet", "MATIC", listOf("https://polygon-rpc.com"))
    object AvalancheCChain : Chain(43114, "Avalanche C-Chain", "AVAX", listOf("https://1rpc.io/avax/c"))
}

internal fun Int.getChain(): Chain? {
    return when (this) {
        Chain.Ethereum.chainId -> Chain.Ethereum
        Chain.BinanceSmartChain.chainId -> Chain.BinanceSmartChain
        Chain.Polygon.chainId -> Chain.Polygon
        Chain.AvalancheCChain.chainId -> Chain.AvalancheCChain
        else -> null
    }
}

internal fun String?.getChainName(): String? {
    if (this == null) return null

    return when (this) {
        Chain.Ethereum.chainId.toString() -> Chain.Ethereum.name
        Chain.BinanceSmartChain.chainId.toString() -> Chain.BinanceSmartChain.name
        Chain.Polygon.chainId.toString() -> Chain.Polygon.name
        Chain.AvalancheCChain.chainId.toString() -> Chain.AvalancheCChain.name
        else -> null
    }
}

internal fun String?.getChainSymbol(): String? {
    if (this == null) return null

    return when (this) {
        Chain.Ethereum.chainId.toString() -> Chain.Ethereum.symbol
        Chain.BinanceSmartChain.chainId.toString() -> Chain.BinanceSmartChain.symbol
        Chain.Polygon.chainId.toString() -> Chain.Polygon.symbol
        Chain.AvalancheCChain.chainId.toString() -> Chain.AvalancheCChain.symbol
        else -> null
    }
}
