package one.mixin.android.vo.safe

import java.math.BigDecimal

data class UnifiedAssetItem(
    val symbol: String,
    val iconUrl: String?,
    val balance: String,
    val priceUsd: String
)