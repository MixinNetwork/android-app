package one.mixin.android.web3

import one.mixin.android.Constants
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.db.web3.vo.Web3TokenItem
import java.math.BigDecimal

val SOLANA_RENT_EXEMPTION: BigDecimal = BigDecimal("0.00203928")

fun isNativeSolAsset(chainId: String?, assetId: String?): Boolean {
    return chainId == Constants.ChainId.SOLANA_CHAIN_ID && assetId == Constants.ChainId.SOLANA_CHAIN_ID
}

fun SwapToken.isNativeSolAsset(): Boolean = isNativeSolAsset(chain.chainId, assetId)

fun Web3TokenItem.isNativeSolAsset(): Boolean = isNativeSolAsset(chainId, assetId)

fun nativeSolSpendableBalance(
    balance: BigDecimal,
    solFee: BigDecimal = BigDecimal.ZERO,
): BigDecimal {
    return balance
        .subtract(solFee)
        .subtract(SOLANA_RENT_EXEMPTION)
        .max(BigDecimal.ZERO)
}

fun hasSolBalanceAfterFeeAndRent(
    balance: BigDecimal,
    solFee: BigDecimal,
): Boolean {
    return balance.subtract(solFee) > SOLANA_RENT_EXEMPTION
}

fun requiredSolBalance(
    transferAmount: BigDecimal,
    solFee: BigDecimal,
    sendingNativeSol: Boolean,
): BigDecimal {
    return if (sendingNativeSol) {
        transferAmount.add(solFee).add(SOLANA_RENT_EXEMPTION)
    } else {
        solFee.add(SOLANA_RENT_EXEMPTION)
    }
}
