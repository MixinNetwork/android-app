package one.mixin.android.api.response.web3

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import one.mixin.android.db.web3.vo.solanaNativeTokenAssetKey
import one.mixin.android.db.web3.vo.wrappedSolTokenAssetKey
import one.mixin.android.extension.equalsIgnoreCase
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
    var price: String? = null,
    var balance: String? = null,
    var collectionHash: String? = null,
    var changeUsd: String? = null,
    var isWeb3: Boolean = false
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
            realAmount(BigDecimal(amount)).stripTrailingZeros().toPlainString()
        } else {
            amount
        }
    }

    fun toStringAmount(amount: Long): String {
        return if (address.isNotEmpty()) {
            realAmount(amount.toBigDecimal()).stripTrailingZeros().toPlainString()
        } else {
            amount.toBigDecimal().toPlainString()
        }
    }

    fun realAmount(amount: BigDecimal): BigDecimal {
        return if (address.isNotEmpty()) {
            amount.divide(BigDecimal.TEN.pow(decimals)).setScale(9, RoundingMode.CEILING)
        } else {
            amount
        }
    }

    fun getUnique(): String {
        return if (isWeb3 || assetId.isEmpty()) assetKey
        else assetId
    }

    private val assetKey: String
        get() = if (address == solanaNativeTokenAssetKey) wrappedSolTokenAssetKey else address

    override fun equals(other: Any?): Boolean {
        if (other !is SwapToken) return false

        return if (address.isNotEmpty()) {
            assetKey == other.assetKey
        } else if (assetId.isNotEmpty()) {
            assetId == other.assetId
        } else {
            name == other.name && chain.chainId == other.chain.chainId
        }
    }

    override fun hashCode(): Int {
        return if (address.isNotEmpty()) {
            assetKey.hashCode()
        } else if (assetId.isNotEmpty()) {
            assetId.hashCode()
        } else {
            (name + chain.chainId).hashCode()
        }
    }

}

interface Swappable : Parcelable {
    fun toSwapToken(): SwapToken
    fun getUnique(): String
}

fun List<SwapToken>.sortByKeywordAndBalance(query: String?): List<SwapToken> {
    return this.sortedWith(
        Comparator { o1, o2 ->
            if (o1 == null && o2 == null) return@Comparator 0
            if (o1 == null) return@Comparator 1
            if (o2 == null) return@Comparator -1

            val equal2Keyword1 = o1.symbol.equalsIgnoreCase(query)
            val equal2Keyword2 = o2.symbol.equalsIgnoreCase(query)
            if (equal2Keyword1 && !equal2Keyword2) {
                return@Comparator -1
            } else if (!equal2Keyword1 && equal2Keyword2) {
                return@Comparator 1
            }

            val priceFiat1 = calculateTokenValue(o1)
            val priceFiat2 = calculateTokenValue(o2)
            val capitalization1 = priceFiat1 * runCatching { BigDecimal(o1.balance) }.getOrDefault(BigDecimal.ZERO)
            val capitalization2 = priceFiat2 * runCatching { BigDecimal(o2.balance) }.getOrDefault(BigDecimal.ZERO)
            if (capitalization1 != capitalization2) {
                if (capitalization2 > capitalization1) {
                    return@Comparator 1
                } else if (capitalization2 < capitalization1) {
                    return@Comparator -1
                }
            }

            if (priceFiat1 == BigDecimal.ZERO && priceFiat2 != BigDecimal.ZERO) {
                return@Comparator 1
            } else if (priceFiat1 != BigDecimal.ZERO && priceFiat2 == BigDecimal.ZERO) {
                return@Comparator -1
            }

            val hasIcon1 = o1.icon != defaultIcon
            val hasIcon2 = o2.icon != defaultIcon
            if (hasIcon1 && !hasIcon2) {
                return@Comparator -1
            } else if (!hasIcon1 && hasIcon2) {
                return@Comparator 1
            }

            return@Comparator o1.name.compareTo(o2.name)
        }
    )
}

private const val defaultIcon = "https://images.mixin.one/yH_I5b0GiV2zDmvrXRyr3bK5xusjfy5q7FX3lw3mM2Ryx4Dfuj6Xcw8SHNRnDKm7ZVE3_LvpKlLdcLrlFQUBhds=s128"

private fun calculateTokenValue(token: SwapToken): BigDecimal {
    if (token.balance.isNullOrBlank() || token.price.isNullOrBlank()) {
        return BigDecimal.ZERO
    }
    return try {
        BigDecimal(token.balance).multiply(BigDecimal(token.price))
    } catch (e: Exception) {
        BigDecimal.ZERO
    }
}