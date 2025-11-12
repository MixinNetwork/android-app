package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName
import one.mixin.android.vo.route.Order

data class CreateLimitOrderResponse(
    @SerializedName("order")
    val order: Order,
    @SerializedName("tx")
    val tx: String,
    @SerializedName("deposit_destination")
    val depositDestination: String? = null,
    @SerializedName("display_user_id")
    val displayUserId: String? = null,
)