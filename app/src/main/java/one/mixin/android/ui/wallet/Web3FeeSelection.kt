package one.mixin.android.ui.wallet

import one.mixin.android.web3.hasSolBalanceAfterFeeAndRent
import one.mixin.android.web3.isNativeSolAsset
import java.math.BigDecimal

internal fun selectPreferredGaslessFeeOption(
    options: List<NetworkFee>,
    preferredAssetId: String?,
    selectedKey: String?,
): NetworkFee? {
    if (options.isEmpty()) return null

    return options.firstOrNull { it.token.assetId == preferredAssetId && it.canCoverSelectionFee() }
        ?: options.firstOrNull { it.selectionKey == selectedKey && it.canCoverSelectionFee() }
        ?: options.firstOrNull(NetworkFee::canCoverSelectionFee)
        ?: options.first()
}

internal fun NetworkFee.canCoverSelectionFee(): Boolean {
    val balance = token.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val feeAmount = fee.toBigDecimalOrNull() ?: BigDecimal.ZERO
    return if (isNativeSolAsset(token.chainId, token.assetId)) {
        hasSolBalanceAfterFeeAndRent(
            balance = balance,
            solFee = feeAmount,
            allowZeroBalance = source == NetworkFee.Source.GASLESS,
        )
    } else {
        balance >= feeAmount
    }
}
