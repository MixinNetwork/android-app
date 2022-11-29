package one.mixin.android.tip.wc

sealed class Chain(val chainId: Int, val name: String, val rpcServers: List<String>) {
    object Ethereum : Chain(1, "Ethereum Mainnet", listOf("https://eth.public-rpc.com"))
    object Polygon : Chain(137, "Polygon Mainnet", listOf("https://polygon-rpc.com"))
}
