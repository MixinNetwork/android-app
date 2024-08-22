package one.mixin.android.api.response.web3

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import one.mixin.android.api.response.Web3ChainId
import one.mixin.android.api.response.polygonNativeTokenAssetKey
import one.mixin.android.api.response.solanaNativeTokenAssetKey
import one.mixin.android.api.response.wrappedSolTokenAssetKey
import java.math.BigDecimal
import java.math.RoundingMode

@Suppress("EqualsOrHashCode")
@Parcelize
data class SwapToken(
    @SerializedName("address") val address: String,
    @SerializedName("assetId") val assetId: String,
    @SerializedName("decimals") val decimals: Int,
    @SerializedName("name") val name: String,
    @SerializedName("symbol") val symbol: String,
    @SerializedName("icon") val icon: String,
    @SerializedName("chain") val chain: SwapChain,
    @SerializedName("web3ChainId") val web3ChainId: Int,
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

    fun toStringAmount(amount: String): String {
        return if (address.isNotEmpty()) {
            realAmount(amount).stripTrailingZeros().toPlainString()
        } else {
            amount
        }
    }

    fun realAmount(amount: String): BigDecimal {
        return if (address.isNotEmpty()) {
            BigDecimal(amount).divide(BigDecimal.TEN.pow(decimals)).setScale(9, RoundingMode.CEILING)
        } else {
            BigDecimal(amount)
        }
    }

    fun isSolToken(): Boolean = address.equals(solanaNativeTokenAssetKey, true) || address.equals(wrappedSolTokenAssetKey, true)

    fun getUnique(): String {
        return address.ifEmpty {
            assetId
        }
    }

    fun getWeb3ApiAddress(): String {
        if (web3ChainId == Web3ChainId.PolygonChainId) {
            return polygonNativeTokenAssetKey
        }
        return address
    }

    fun inMixin(): Boolean = assetId != ""

    override fun equals(other: Any?): Boolean {
        if (other !is SwapToken) return false

        return if (address.isNotEmpty()) {
            address == other.address
        } else if (assetId.isNotEmpty()) {
            assetId == other.assetId
        } else {
            false
        }
    }
}

interface Swappable : Parcelable {
    fun toSwapToken(): SwapToken
    fun getUnique(): String
}
