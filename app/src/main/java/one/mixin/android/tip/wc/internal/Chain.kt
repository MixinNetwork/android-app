package one.mixin.android.tip.wc.internal

import com.walletconnect.web3.wallet.client.Wallet
import one.mixin.android.Constants
import one.mixin.android.Constants.ChainId.ETHEREUM_CHAIN_ID
import one.mixin.android.Constants.ChainId.SOLANA_CHAIN_ID
import one.mixin.android.tip.privateKeyToAddress

sealed class Chain(
    val chainNamespace: String,
    val chainReference: String,
    val name: String,
    val symbol: String,
    val rpcServers: List<String>,
    val chainId: String = "$chainNamespace:$chainReference",
) {
    object Ethereum : Chain("eip155", "1", "Ethereum Mainnet", "ETH", listOf("https://cloudflare-eth.com"))

    object BinanceSmartChain : Chain("eip155", "56", "Binance Smart Chain Mainnet", "BNB", listOf("https://bsc-dataseed4.ninicoin.io"))

    object Polygon : Chain("eip155", "137", "Polygon Mainnet", "MATIC", listOf("https://polygon-rpc.com"))

    object Solana : Chain("solana", "4sGjMW1sUnHzSxGspuhpqLDx6wiyjNtZ", "Solana Mainnet", "SOL", listOf("https://api.mainnet-beta.solana.com"))
}

internal val supportChainList = listOf(Chain.Ethereum, Chain.BinanceSmartChain, Chain.Polygon, Chain.Solana)
internal val evmChainList = listOf(Chain.Ethereum, Chain.BinanceSmartChain, Chain.Polygon)

internal fun String.getChain(): Chain? {
    return when (this) {
        Chain.Ethereum.chainId -> Chain.Ethereum
        Chain.BinanceSmartChain.chainId -> Chain.BinanceSmartChain
        Chain.Polygon.chainId -> Chain.Polygon
        Chain.Solana.chainId -> Chain.Solana
        else -> null
    }
}

internal fun String?.getChainName(): String? {
    if (this == null) return null

    return when (this) {
        Chain.Ethereum.chainId -> Chain.Ethereum.name
        Chain.BinanceSmartChain.chainId -> Chain.BinanceSmartChain.name
        Chain.Polygon.chainId -> Chain.Polygon.name
        Chain.Solana.chainId -> Chain.Solana.name
        else -> null
    }
}

internal fun String?.getChainSymbol(): String? {
    if (this == null) return null

    return when (this) {
        Chain.Ethereum.chainId -> Chain.Ethereum.symbol
        Chain.BinanceSmartChain.chainId -> Chain.BinanceSmartChain.symbol
        Chain.Polygon.chainId -> Chain.Polygon.symbol
        Chain.Solana.chainId -> Chain.Solana.symbol
        else -> null
    }
}

val walletConnectChainIdMap =
    mapOf(
        Chain.Ethereum.symbol to Constants.ChainId.ETHEREUM_CHAIN_ID,
        Chain.Polygon.symbol to Constants.ChainId.Polygon,
        Chain.BinanceSmartChain.symbol to Constants.ChainId.BinanceSmartChain,
        Chain.Solana.symbol to Constants.ChainId.Solana,
    )

fun getSupportedNamespaces(chain: Chain, priv: ByteArray): Map<String, Wallet.Model.Namespace.Session> {
    return when (chain) {
        is Chain.Solana -> {
            val address = privateKeyToAddress(priv, SOLANA_CHAIN_ID)
            getSolanaNamespaces(address)
        }

        is Chain.Polygon, is Chain.Ethereum, is Chain.BinanceSmartChain -> {
            val address = privateKeyToAddress(priv, ETHEREUM_CHAIN_ID)
            getEvmNamespaces(address)
        }

        else -> {
            throw IllegalArgumentException("No support")
        }
    }
}

private fun getEvmNamespaces(address: String): Map<String, Wallet.Model.Namespace.Session> {
    val chainIds = evmChainList.map { chain -> chain.chainId }
    val accounts = evmChainList.map { chain -> "${chain.chainNamespace}:${chain.chainReference}:$address" }
    return mapOf(
        "eip155" to
            Wallet.Model.Namespace.Session(
                chains = chainIds,
                methods = evmSupportedMethods,
                events = listOf("chainChanged", "accountsChanged"),
                accounts = accounts,
            )
    )
}

private fun getSolanaNamespaces(address: String): Map<String, Wallet.Model.Namespace.Session> {
    return mapOf(
        "solana" to
            Wallet.Model.Namespace.Session(
                chains = listOf("solana:4sGjMW1sUnHzSxGspuhpqLDx6wiyjNtZ"),
                methods = solanaSupporedMethods,
                events = listOf(""),
                accounts = listOf("solana:4sGjMW1sUnHzSxGspuhpqLDx6wiyjNtZ:AcYW4VmviQPp9q6uYeiDfQaFdXaxH3BuPxu8zWoibGLf"),
            )
    )
}
