package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

class CreateSessionResponse(
    @SerializedName("session_id")
    val sessionId:String,
    @SerializedName("session_secret")
    val sessionSecret:String,
    @SerializedName("instrument_id")
    val instrumentId:String,
    @SerializedName("scheme")
    val scheme:String,
    @SerializedName("amount")
    val amount:Int,
    @SerializedName("currency")
    val currency:String,
)
