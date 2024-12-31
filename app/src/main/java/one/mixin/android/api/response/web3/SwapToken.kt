package one.mixin.android.api.response.web3

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
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
    var price: String? = null,
    var balance: String? = null,
    var collectionHash: String? = null,
    var changeUsd: String? = null,
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
        return assetId.ifEmpty {
            assetKey
        }
    }

    val assetKey: String
        get() = if (address == solanaNativeTokenAssetKey) wrappedSolTokenAssetKey else address

    fun inMixin(): Boolean = assetId != ""

    override fun equals(other: Any?): Boolean {
        if (other !is SwapToken) return false

        return if (address.isNotEmpty()) {
            assetKey == other.assetKey
        } else if (assetId.isNotEmpty()) {
            assetId == other.assetId
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return if (address.isNotEmpty()) {
            assetKey.hashCode()
        } else if (assetId.isNotEmpty()) {
            assetId.hashCode()
        } else {
            super.hashCode()
        }
    }

}

interface Swappable : Parcelable {
    fun toSwapToken(): SwapToken
    fun getUnique(): String
}

fun List<SwapToken>.sortByKeywordAndBalance(keyword: String?): List<SwapToken> {
    return sortedWith { a, b ->
        when {
            !keyword.isNullOrBlank() -> {
                val aMatchLevel = getMatchLevel(a, keyword)
                val bMatchLevel = getMatchLevel(b, keyword)

                when {
                    aMatchLevel != bMatchLevel -> bMatchLevel.compareTo(aMatchLevel)
                    else -> a.symbol.compareTo(b.symbol)
                }
            }

            else -> {
                compareByBalanceAndPrice(a, b)
            }
        }
    }
}

private fun getMatchLevel(token: SwapToken, keyword: String): Int {
    val symbolLower = token.symbol.lowercase()
    val keywordLower = keyword.lowercase()

    return when {
        symbolLower == keywordLower -> 2
        symbolLower.startsWith(keywordLower) -> 1
        else -> 0
    }
}

private fun compareByBalanceAndPrice(a: SwapToken, b: SwapToken): Int {
    val aValue = calculateTokenValue(a)
    val bValue = calculateTokenValue(b)

    return when {
        aValue != BigDecimal.ZERO || bValue != BigDecimal.ZERO -> bValue.compareTo(aValue)
        else -> {
            when {
                !a.balance.isNullOrBlank() && !b.balance.isNullOrBlank() ->
                    b.balance!!.compareTo(a.balance!!)

                !a.balance.isNullOrBlank() -> -1
                !b.balance.isNullOrBlank() -> 1
                else -> a.symbol.compareTo(b.symbol)
            }
        }
    }
}

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