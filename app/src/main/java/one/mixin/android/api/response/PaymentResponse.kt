@file:Suppress("ktlint:enum-entry-name-case", "EnumEntryName")

package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class PaymentResponse(
    val status: String,
    @SerializedName("asset")
    val assetPrecision: AssetPrecision?,
)

data class AssetPrecision(
    @SerializedName("chain_id")
    val chainId: String,
    val precision: Int,
)

enum class PaymentStatus {
    pending, paid
}
