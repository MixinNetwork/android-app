package one.mixin.android.vo

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class Ticker(
    @SerializedName("price_usd")
    val priceUsd: String,
    @SerializedName("price_btc")
    val priceBtc: String,
) {

    fun priceFiat(): BigDecimal = if (priceUsd == "0") {
        BigDecimal.ZERO
    } else BigDecimal(priceUsd).multiply(BigDecimal(Fiats.getRate()))
}
