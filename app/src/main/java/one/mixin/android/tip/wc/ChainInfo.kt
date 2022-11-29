package one.mixin.android.tip.wc

sealed class Chain(val chainId: Int, val rpcServers: List<String>) {
    object Ethereum : Chain(1, listOf("https://eth.public-rpc.com"))
    object Polygon : Chain(137, listOf("https://polygon-rpc.com"))
}