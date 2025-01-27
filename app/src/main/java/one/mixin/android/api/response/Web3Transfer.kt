package one.mixin.android.api.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import one.mixin.android.extension.numberFormat2
import one.mixin.android.vo.Fiats
import java.math.BigDecimal

@Parcelize
data class Web3Transfer(
    @SerializedName("asset_key")
    val assetKey: String,
    @SerializedName("chain_id")
    val chainId: String,
    val name: String,
    val symbol: String,
    @SerializedName("icon_url")
    val iconUrl: String,
    val direction: String,
    val sender: String,
    val amount: String,
    val price: String,
) : Parcelable {
    val tokenId: String
        get() {
            return chainId + assetKey
        }

    val amountFormat: String
        get() {
            return runCatching {
                "${Fiats.getSymbol()}${
                    BigDecimal(price).multiply(BigDecimal(Fiats.getRate()))
                        .multiply(BigDecimal(amount)).numberFormat2()
                }"
            }.getOrDefault("N/A")
        }
}