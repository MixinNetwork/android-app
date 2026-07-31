@file:Suppress("ktlint:standard:enum-entry-name-case", "EnumEntryName")

package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class PaymentResponse(
    @SerializedName("amount")
    val amount: String?,
    @SerializedName("minimum")
    val minimum: String?,
    @SerializedName("maximum")
    val maximum: String?,
    @SerializedName("destination")
    val destination: String?,
    @SerializedName("asset")
    val asset: TransferAsset?,
    @SerializedName("status")
    val status: String
)

class TransferAsset(
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("chain_id")
    val chainId: String,
)


enum class PaymentStatus {
    pending,
    paid,
}
