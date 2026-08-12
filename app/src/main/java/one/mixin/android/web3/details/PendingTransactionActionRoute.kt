package one.mixin.android.web3.details

import one.mixin.android.Constants

internal enum class PendingTransactionActionRoute {
    Utxo,
    Evm,
    Unsupported,
}

internal fun pendingTransactionActionRoute(chainId: String): PendingTransactionActionRoute =
    when {
        chainId in Constants.Web3UtxoChainIds -> PendingTransactionActionRoute.Utxo
        chainId in Constants.Web3EvmChainIds -> PendingTransactionActionRoute.Evm
        else -> PendingTransactionActionRoute.Unsupported
    }
