package one.mixin.android.vo.checkout

import com.google.gson.annotations.SerializedName

class TraceRequest(
    @SerializedName("token")
    val token: String,
    @SerializedName("currency")
    val currency: String,
    @SerializedName("user_id")
    val userID: String,
    @SerializedName("amount")
    val amount: Long,
    @SerializedName("asset_id")
    val assetID: String,
    @SerializedName("session_id")
    val sessionID: String,
    @SerializedName("session_secret")
    val sessionSecret: String,
)
