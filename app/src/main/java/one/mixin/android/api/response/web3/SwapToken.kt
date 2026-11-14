package one.mixin.android.api.response.web3

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import one.mixin.android.Constants
import one.mixin.android.util.assetListComparator
import java.math.BigDecimal

@Suppress("EqualsOrHashCode")
@Parcelize
data class SwapToken(
    @SerializedName("wallet_id") var walletId: String?,
    @SerializedName("address") val address: String,
    @SerializedName("assetId") val assetId: String,
    @SerializedName("decimals") val decimals: Int,
    @SerializedName("name") val name: String,
    @SerializedName("symbol") val symbol: String,
    @SerializedName("icon") val icon: String,
    @SerializedName("chain") val chain: SwapChain,
    @SerializedName("category")
    val category: String? = null,
    @SerializedName("price")
    var price: String? = null,
    @SerializedName("balance")
    var balance: String? = null,
    @SerializedName("collectionHash")
    var collectionHash: String? = null,
    @SerializedName("changeUsd")
    var changeUsd: String? = null,
    @SerializedName("isWeb3")
    var isWeb3: Boolean = false,
    @SerializedName("level") val level: Int? = null
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
            BigDecimal(amount).stripTrailingZeros().toPlainString()
        } else {
            amount
        }
    }

    fun toStringAmount(amount: Long): String {
        return if (address.isNotEmpty()) {
            amount.toBigDecimal().stripTrailingZeros().toPlainString()
        } else {
            amount.toBigDecimal().toPlainString()
        }
    }

    fun getUnique(): String {
        return assetId
    }

    private val assetKey: String
        get() = address

    override fun equals(other: Any?): Boolean {
        if (other !is SwapToken) return false

        return if (assetId.isNotEmpty()) {
            assetId == other.assetId && walletId == other.walletId
        } else if (address.isNotEmpty()) {
            assetKey == other.assetKey && walletId == other.walletId
        } else {
            name == other.name && chain.chainId == other.chain.chainId && walletId == other.walletId
        }
    }

    override fun hashCode(): Int {
        return if (assetId.isNotEmpty()) {
            (assetId + walletId).hashCode()
        } else if (address.isNotEmpty()) {
            (assetKey + walletId).hashCode()
        } else {
            (name + chain.chainId + walletId).hashCode()
        }
    }

    @IgnoredOnParcel
    val priceValue: BigDecimal
        get() = price?.toBigDecimalOrNull() ?: BigDecimal.ZERO

    @IgnoredOnParcel
    val balanceValue: BigDecimal
        get() = balance?.toBigDecimalOrNull() ?: BigDecimal.ZERO

    fun isSpam(): Boolean {
        level ?: return false
        return level <= Constants.AssetLevel.SPAM
    }
}

interface Swappable : Parcelable {
    fun toSwapToken(): SwapToken
    fun getUnique(): String
}

fun List<SwapToken>.sortByKeywordAndBalance(query: String? = null): List<SwapToken> =
    sortedWith(
        assetListComparator(
            query = query,
            symbol = { it.symbol },
            balance = { it.balanceValue },
            price = { it.priceValue },
            level = { it.level },
            name = { it.name },
            chainName = { it.chain.name },
        ),
    )
