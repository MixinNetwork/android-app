package one.mixin.android.api.response.web3

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import one.mixin.android.api.response.solanaNativeTokenAssetKey
import one.mixin.android.api.response.wrappedSolTokenAssetKey
import java.math.BigDecimal
import java.math.RoundingMode

@Parcelize
data class SwapToken(
    @SerializedName("address") val address: String,
    @SerializedName("decimals") val decimals: Int,
    @SerializedName("name") val name: String,
    @SerializedName("symbol") val symbol: String,
    @SerializedName("logoURI") val logoURI: String,
    @SerializedName("chain") val chain: SwapChain,
    var price: String? = null,
    var balance: String? = null,
) : Parcelable {
    fun toLongAmount(amount: String): Long {
        val a =
            try {
                BigDecimal(amount)
            } catch (e: Exception) {
                return 0
            }
        return a.multiply(BigDecimal.TEN.pow(decimals)).toLong()
    }

    fun toStringAmount(amount: Long): String {
        return realAmount(amount).stripTrailingZeros().toPlainString()
    }

    fun realAmount(amount: Long): BigDecimal {
        return BigDecimal(amount).divide(BigDecimal.TEN.pow(decimals)).setScale(9, RoundingMode.CEILING)
    }

    fun isSolToken(): Boolean = address.equals(solanaNativeTokenAssetKey, true) || address.equals(wrappedSolTokenAssetKey, true)

    override fun equals(other: Any?): Boolean {
        if (other !is SwapToken) return false

        return address == other.address
    }
}
