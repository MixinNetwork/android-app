package one.mixin.android.web3.details

import one.mixin.android.Constants
import one.mixin.android.web3.send.BtcTransactionBuilder

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

internal fun shouldHideUtxoPendingActions(
    chainId: String,
    rawTransactionHex: String,
    hasSignedChange: Boolean,
): Boolean {
    val replacementUnavailable: Boolean =
        chainId in Constants.Web3UtxoChainIds && !BtcTransactionBuilder.isReplaceable(rawTransactionHex)
    return replacementUnavailable || hasSignedChange
}
