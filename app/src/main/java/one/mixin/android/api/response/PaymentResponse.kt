@file:Suppress("ktlint:standard:enum-entry-name-case", "EnumEntryName")

package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName
import one.mixin.android.vo.Address
import one.mixin.android.vo.Asset
import one.mixin.android.vo.User

data class PaymentResponse(
    val amount: String?,
    val minimum: String?,
    val maximum: String?,
    val destination: String?,
    val asset: TransferAsset?,
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
