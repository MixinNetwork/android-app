package one.mixin.android.ui.wallet.alert.vo

import com.google.gson.annotations.SerializedName

class AlertUpdateRequest(
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("frequency")
    val frequency: String? = null,
    @SerializedName("value")
    val value: String? = null,
    @SerializedName("action")
    val action: String,
)
