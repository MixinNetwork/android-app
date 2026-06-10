package one.mixin.android.ui.wallet.home

import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.vo.Fiats
import java.math.BigDecimal

internal fun calculateWalletHomeTotalFiat(
    tokenFiat: BigDecimal,
    positionFiat: BigDecimal,
): BigDecimal = tokenFiat + positionFiat

internal fun calculateWalletHomePositionMarginFiat(
    margins: Iterable<String?>,
    fiatRate: BigDecimal,
): BigDecimal =
    margins.fold(BigDecimal.ZERO) { total, margin ->
        total + (margin?.toBigDecimalOrNull() ?: BigDecimal.ZERO)
    }.multiply(fiatRate)

internal fun List<PerpsPositionItem>.positionMarginFiatTotal(): BigDecimal =
    calculateWalletHomePositionMarginFiat(
        margins = map { it.margin },
        fiatRate = BigDecimal(Fiats.getRate()),
    )
