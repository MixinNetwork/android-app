package one.mixin.android.tip.wc.internal

import com.reown.walletkit.client.Wallet
import one.mixin.android.Constants
import one.mixin.android.Constants.ChainId.ETHEREUM_CHAIN_ID
import one.mixin.android.Constants.ChainId.SOLANA_CHAIN_ID
import one.mixin.android.MixinApplication
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.web3.Web3ChainId

sealed class Chain(
    val assetId: String,
    val chainNamespace: String,
    val chainReference: String,
    val hexReference: String,
    val name: String,
    val symbol: String,
    private val rpcServers: List<String>,
) {
    object Ethereum : Chain(ETHEREUM_CHAIN_ID, "eip155", "1", "0x1", "Ethereum Mainnet", "ETH", listOf("https://eth.llamarpc.com"))

    object Arbitrum : Chain(ETHEREUM_CHAIN_ID, "eip155", "42161", "0xa4b1", "Arbitrum One", "ETH", listOf("https://arbitrum.llamarpc.com"))

    object Optimism : Chain(ETHEREUM_CHAIN_ID, "eip155", "10", "0xa", "OP Mainnet", "ETH", listOf("https://optimism.llamarpc.com"))

    object Base : Chain(ETHEREUM_CHAIN_ID, "eip155", "8453", "0x2105", "Base", "ETH", listOf("https://base.llamarpc.com"))

    object Blast : Chain(ETHEREUM_CHAIN_ID, "eip155", "81457", "0x13e31", "Blast", "ETH", listOf("https://rpc.blast.io"))

    object BinanceSmartChain : Chain(Constants.ChainId.BinanceSmartChain, "eip155", "56", "0x38", "Binance Smart Chain Mainnet", "BNB", listOf("https://bsc-dataseed4.ninicoin.io"))

    object Polygon : Chain(Constants.ChainId.Polygon, "eip155", "137", "0x89", "Polygon Mainnet", "MATIC", listOf("https://polygon-rpc.com"))

    object Avalanche : Chain(Constants.ChainId.Avalanche, "eip155", "43114", "0xa86a", "Avalanche", "AVAX", listOf("https://avalanche.drpc.org"))

    object Solana : Chain(SOLANA_CHAIN_ID, "solana", "4sGjMW1sUnHzSxGspuhpqLDx6wiyjNtZ", "4sGjMW1sUnHzSxGspuhpqLDx6wiyjNtZ", "Solana Mainnet", "SOL", listOf("https://api.mainnet-beta.solana.com"))

    val chainId: String
        get() {
            return "$chainNamespace:$chainReference"
        }

    val rpcUrl: String
        get() {
            return MixinApplication.appContext.defaultSharedPreferences.getString(chainId, null) ?: rpcServers.first()
        }

    fun getWeb3ChainId(): String =
        // Optimism -> Constants.ChainId.
        // Arbitrum ->  Constants.ChainId.
        // Blast ->  Constants.ChainId.
        when (this) {
            Ethereum -> Constants.ChainId.ETHEREUM_CHAIN_ID
            BinanceSmartChain -> Constants.ChainId.BinanceSmartChain
            Polygon ->  Constants.ChainId.Polygon
            Base ->  Constants.ChainId.Base
            Avalanche ->  Constants.ChainId.Avalanche
            else ->  Constants.ChainId.Solana
        }
}
// Chain.Blast, Chain.Arbitrum, Chain.Optimism
internal val supportChainList = listOf(Chain.Ethereum, Chain.Base, Chain.BinanceSmartChain, Chain.Polygon, Chain.Avalanche, Chain.Solana)
internal val evmChainList = listOf(Chain.Ethereum, Chain.Base, Chain.BinanceSmartChain, Chain.Polygon, Chain.Avalanche)

internal fun String.getChain(): Chain? {
    return when (this) {
        Chain.Ethereum.chainReference -> Chain.Ethereum
        Chain.Base.chainReference -> Chain.Ethereum
        Chain.Blast.chainReference -> Chain.Ethereum
        Chain.Arbitrum.chainReference -> Chain.Ethereum
        Chain.Optimism.chainReference -> Chain.Ethereum
        Chain.BinanceSmartChain.chainReference -> Chain.BinanceSmartChain
        Chain.Polygon.chainReference -> Chain.Polygon
        Chain.Avalanche.chainReference -> Chain.Avalanche
        Chain.Solana.chainId -> Chain.Solana
        else -> null
    }
}

internal fun String?.getChainName(): String? {
    if (this == null) return null

    return when (this) {
        Chain.Ethereum.chainId -> Chain.Ethereum.name
        Chain.Base.chainId -> Chain.Base.name
        Chain.Blast.chainId -> Chain.Blast.name
        Chain.Arbitrum.chainId -> Chain.Arbitrum.name
        Chain.Optimism.chainId -> Chain.Optimism.name
        Chain.BinanceSmartChain.chainId -> Chain.BinanceSmartChain.name
        Chain.Polygon.chainId -> Chain.Polygon.name
        Chain.Avalanche.chainId -> Chain.Avalanche.name
        Chain.Solana.chainId -> Chain.Solana.name
        else -> null
    }
}

internal fun String?.getChainSymbol(): String? {
    if (this == null) return null

    return when (this) {
        Chain.Ethereum.chainId -> Chain.Ethereum.symbol
        Chain.Base.chainId -> Chain.Base.symbol
        Chain.Blast.chainId -> Chain.Blast.symbol
        Chain.Arbitrum.chainId -> Chain.Arbitrum.symbol
        Chain.Optimism.chainId -> Chain.Optimism.symbol
        Chain.BinanceSmartChain.chainId -> Chain.BinanceSmartChain.symbol
        Chain.Polygon.chainId -> Chain.Polygon.symbol
        Chain.Avalanche.chainId -> Chain.Avalanche.symbol
        Chain.Solana.chainId -> Chain.Solana.symbol
        else -> null
    }
}

internal fun getChainByChainId(chainId: String?): Chain? {
    if (chainId == null) return null

    return when (chainId) {
        Chain.Ethereum.chainId -> Chain.Ethereum
        Chain.Base.chainId -> Chain.Base
        Chain.Blast.chainId -> Chain.Blast
        Chain.Arbitrum.chainId -> Chain.Arbitrum
        Chain.Optimism.chainId -> Chain.Optimism
        Chain.BinanceSmartChain.chainId -> Chain.BinanceSmartChain
        Chain.Polygon.chainId -> Chain.Polygon
        Chain.Avalanche.chainId -> Chain.Avalanche
        Chain.Solana.chainId -> Chain.Solana
        else -> null
    }
}

val walletConnectChainIdMap =
    mapOf(
        Chain.Ethereum.symbol to ETHEREUM_CHAIN_ID,
        Chain.Base.symbol to ETHEREUM_CHAIN_ID,
        Chain.Blast.symbol to ETHEREUM_CHAIN_ID,
        Chain.Arbitrum.symbol to ETHEREUM_CHAIN_ID,
        Chain.Optimism.symbol to ETHEREUM_CHAIN_ID,
        Chain.Polygon.symbol to Constants.ChainId.Polygon,
        Chain.BinanceSmartChain.symbol to Constants.ChainId.BinanceSmartChain,
        Chain.Solana.symbol to Constants.ChainId.Solana,
        Chain.Avalanche.symbol to Constants.ChainId.Avalanche,
    )

fun getSupportedNamespaces(
    chain: Chain,
    address: String,
): Map<String, Wallet.Model.Namespace.Session> {
    return when {
        chain == Chain.Solana -> {
            getSolanaNamespaces(address)
        }

        evmChainList.contains(chain) -> {
            getEvmNamespaces(address)
        }

        else -> {
            throw IllegalArgumentException("Not supported chain ${chain.name}")
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
                events = listOf("connect", "disconnect", "chainChanged", "accountsChanged", "message"),
                accounts = accounts,
            ),
    )
}

private fun getSolanaNamespaces(address: String): Map<String, Wallet.Model.Namespace.Session> {
    return mapOf(
        "solana" to
            Wallet.Model.Namespace.Session(
                chains = listOf("solana:4sGjMW1sUnHzSxGspuhpqLDx6wiyjNtZ"),
                methods = solanaSupporedMethods,
                events = listOf(""),
                accounts = listOf("solana:4sGjMW1sUnHzSxGspuhpqLDx6wiyjNtZ:$address"),
            ),
    )
}
