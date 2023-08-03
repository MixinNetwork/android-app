package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

class PayTokenRequest(
    @SerializedName("type")
    val type: String,
    @SerializedName("token_data")
    val tokenData: TokenData,
)

class TokenData(

    val protocolVersion: String,
    val signature: String,
    val signedMessage: String,
)
