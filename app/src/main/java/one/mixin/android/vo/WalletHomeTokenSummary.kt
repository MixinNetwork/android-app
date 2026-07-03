package one.mixin.android.vo

import androidx.room3.ColumnInfo

data class WalletHomeTokenSummary(
    @ColumnInfo(name = "token_count")
    val tokenCount: Int = 0,
    @ColumnInfo(name = "total_usd")
    val totalUsd: Double = 0.0,
    @ColumnInfo(name = "total_btc")
    val totalBtc: Double = 0.0,
    @ColumnInfo(name = "bitcoin_price_usd")
    val bitcoinPriceUsd: String? = null,
    @ColumnInfo(name = "hidden_token_count")
    val hiddenTokenCount: Int = 0,
) {
    fun hasOnlyHiddenTokens(): Boolean = tokenCount == 0 && hiddenTokenCount > 0
}
