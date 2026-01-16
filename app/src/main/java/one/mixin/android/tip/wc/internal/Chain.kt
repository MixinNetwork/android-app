package one.mixin.android.tip.wc.internal

import com.reown.walletkit.client.Wallet
import one.mixin.android.Constants
import one.mixin.android.Constants.ChainId.BITCOIN_CHAIN_ID
import one.mixin.android.Constants.ChainId.ETHEREUM_CHAIN_ID
import one.mixin.android.Constants.ChainId.SOLANA_CHAIN_ID
import one.mixin.android.MixinApplication
import one.mixin.android.extension.defaultSharedPreferences

sealed class Chain(
    val assetId: String,
    val chainNamespace: String,
    val chainReference: String,
    val hexReference: String,
    val name: String,
    val symbol: String,
    private val rpcServers: List<String>,
) {
    object Ethereum : Chain(ETHEREUM_CHAIN_ID, "eip155", "1", "0x1", "Ethereum", "ETH", listOf("https://eth.llamarpc.com"))

    object Arbitrum : Chain(Constants.ChainId.Arbitrum, "eip155", "42161", "0xa4b1", "Arbitrum One", "ETH", listOf("https://arbitrum.llamarpc.com"))

    object Optimism : Chain(Constants.ChainId.Optimism, "eip155", "10", "0xa", "OP Mainnet", "ETH", listOf("https://optimism.llamarpc.com"))

    object Base : Chain(Constants.ChainId.Base, "eip155", "8453", "0x2105", "Base", "ETH", listOf("https://base.llamarpc.com"))

    // Todo replace id
    object Blast : Chain(ETHEREUM_CHAIN_ID, "eip155", "81457", "0x13e31", "Blast", "ETH", listOf("https://rpc.blast.io"))

    object BinanceSmartChain : Chain(Constants.ChainId.BinanceSmartChain, "eip155", "56", "0x38", "BNB Smart Chain", "BNB", listOf("https://bsc-dataseed4.ninicoin.io"))

    object Polygon : Chain(Constants.ChainId.Polygon, "eip155", "137", "0x89", "Polygon", "MATIC", listOf("https://polygon-rpc.com"))

    object Avalanche : Chain(Constants.ChainId.Avalanche, "eip155", "43114", "0xa86a", "Avalanche C-Chain", "AVAX", listOf("https://api.avax.network/ext/bc/C/rpc"))

    object Solana : Chain(SOLANA_CHAIN_ID, "solana", "4sGjMW1sUnHzSxGspuhpqLDx6wiyjNtZ", "4sGjMW1sUnHzSxGspuhpqLDx6wiyjNtZ", "Solana", "SOL", listOf("https://api.mainnet-beta.solana.com"))

    object Bitcoin : Chain(BITCOIN_CHAIN_ID, "BTC", "", "", "Bitcoin", "BTC", listOf(""))

    val chainId: String
        get() {
            return "$chainNamespace:$chainReference"
        }

    val rpcUrl: String
        get() {
            return MixinApplication.appContext.defaultSharedPreferences.getString(chainId, null) ?: rpcServers.first()
        }

    fun getWeb3ChainId(): String =
        // Blast ->  Constants.ChainId.
        when (this) {
            Ethereum -> ETHEREUM_CHAIN_ID
            BinanceSmartChain -> Constants.ChainId.BinanceSmartChain
            Optimism -> Constants.ChainId.Optimism
            Arbitrum ->  Constants.ChainId.Arbitrum
            Polygon ->  Constants.ChainId.Polygon
            Base ->  Constants.ChainId.Base
            Avalanche -> Constants.ChainId.Avalanche
            else ->  Constants.ChainId.Solana
        }
}
// Chain.Blast
internal val supportChainList = listOf(Chain.Solana, Chain.Ethereum, Chain.Base, Chain.BinanceSmartChain, Chain.Polygon, Chain.Optimism, Chain.Arbitrum, Chain.Avalanche)
internal val evmChainList = listOf(Chain.Ethereum, Chain.Base, Chain.BinanceSmartChain, Chain.Polygon, Chain.Optimism, Chain.Arbitrum, Chain.Avalanche)

internal fun String.getChain(): Chain? {
    return when (this) {
        Chain.Ethereum.chainReference -> Chain.Ethereum
        Chain.Base.chainReference -> Chain.Base
        Chain.Arbitrum.chainReference -> Chain.Arbitrum
        Chain.Optimism.chainReference -> Chain.Optimism
        Chain.Avalanche.chainReference -> Chain.Avalanche
        Chain.BinanceSmartChain.chainReference -> Chain.BinanceSmartChain
        Chain.Polygon.chainReference -> Chain.Polygon
        Chain.Solana.chainId -> Chain.Solana
        else -> null
    }
}

internal fun getChainByChainId(chainId: String?): Chain? {
    if (chainId == null) return null

    return when (chainId) {
        Chain.Ethereum.chainId -> Chain.Ethereum
        Chain.Base.chainId -> Chain.Base
        Chain.Arbitrum.chainId -> Chain.Arbitrum
        Chain.Optimism.chainId -> Chain.Optimism
        Chain.Avalanche.chainId -> Chain.Avalanche
        Chain.BinanceSmartChain.chainId -> Chain.BinanceSmartChain
        Chain.Polygon.chainId -> Chain.Polygon
        Chain.Solana.chainId -> Chain.Solana
        else -> null
    }
}

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
