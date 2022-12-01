package one.mixin.android.tip.wc

sealed class Chain(val chainId: Int, val name: String, val symbol: String, val rpcServers: List<String>) {
    object Ethereum : Chain(1, "Ethereum Mainnet", "ETH", listOf("https://eth.public-rpc.com"))
    object Polygon : Chain(137, "Polygon Mainnet", "MATIC", listOf("https://polygon-rpc.com"))
}
