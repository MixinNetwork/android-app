package one.mixin.android.ui.wallet.alert.vo

import com.google.gson.annotations.SerializedName

class AlertRequest(
    @SerializedName("coin_id")
    val coinId: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("frequency")
    val frequency: String,
    @SerializedName("value")
    val value: String,
    @SerializedName("lang")
    val lang: String,
)
