package one.mixin.android.ui.wallet.alert.vo

import com.google.gson.annotations.SerializedName

class Alert(
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("type")
    val type: AlertType,
    @SerializedName("frequency")
    val frequency: AlertFrequency,
    @SerializedName("value")
    val value: String,
    @SerializedName("lang")
    val lang: String,
    @SerializedName("created_at")
    val createdAt: String
)
