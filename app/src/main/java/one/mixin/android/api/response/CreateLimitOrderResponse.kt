package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName
import one.mixin.android.api.response.LimitOrder

data class CreateLimitOrderResponse(
    @SerializedName("order")
    val order: LimitOrder,
    @SerializedName("tx")
    val tx: String,
)