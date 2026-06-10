package one.mixin.android.ui.wallet.home

import one.mixin.android.api.response.perps.PerpsPositionItem
import java.math.BigDecimal

internal fun calculateWalletHomeTotalFiat(
    tokenFiat: BigDecimal,
    positionUsd: BigDecimal,
): BigDecimal = tokenFiat + positionUsd

internal fun calculateWalletHomePositionMarginUsd(
    margins: Iterable<String?>,
): BigDecimal =
    margins.fold(BigDecimal.ZERO) { total, margin ->
        total + (margin?.toBigDecimalOrNull() ?: BigDecimal.ZERO)
    }

internal fun List<PerpsPositionItem>.positionMarginUsdTotal(): BigDecimal =
    calculateWalletHomePositionMarginUsd(
        margins = map { it.margin },
    )
