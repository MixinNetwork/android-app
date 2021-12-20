package one.mixin.android.vo

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class Ticker(
    @Json(name = "price_usd")
    val priceUsd: String,
    @Json(name = "price_btc")
    val priceBtc: String,
) {

    fun priceFiat(): BigDecimal = if (priceUsd == "0") {
        BigDecimal.ZERO
    } else BigDecimal(priceUsd).multiply(BigDecimal(Fiats.getRate()))
}
