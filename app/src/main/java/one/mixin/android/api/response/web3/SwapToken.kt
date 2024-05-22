package one.mixin.android.api.response.web3

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.math.RoundingMode

@Parcelize
data class SwapToken(
    @SerializedName("address") val address: String,
    @SerializedName("chainId") val chainId: Int,
    @SerializedName("decimals") val decimals: Int,
    @SerializedName("name") val name: String,
    @SerializedName("symbol") val symbol: String,
    @SerializedName("logoURI") val logoURI: String,
    @SerializedName("tags") val tags: List<String>,
    @SerializedName("chainLogoURI") val chainLogoURI: String,
    var price: String? = null,
) : Parcelable {
    fun toLongAmount(amount: String): Long {
        val a = try {
            BigDecimal(amount)
        } catch (e: Exception) {
            return 0
        }
        return a.multiply(BigDecimal.TEN.pow(decimals)).toLong()
    }

    fun toStringAmount(amount: Long): String {
        return BigDecimal(amount).divide(BigDecimal.TEN.pow(decimals)).setScale(9, RoundingMode.CEILING).toPlainString()
    }
}
