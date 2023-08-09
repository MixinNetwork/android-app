package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

class CreateSessionRequest(
    @SerializedName("source")
    val source: Source,
    @SerializedName("currency")
    val currency: String,
    @SerializedName("completion")
    val completion: CompletionInfo = CompletionInfo(),
)

class Source(
    @SerializedName("type")
    val type: String,
    @SerializedName("token")
    val token: String,
)

class CompletionInfo(
    @SerializedName("type")
    val type: String = "non_hosted",
    @SerializedName("callback_url")
    val callbackUrl: String = "https://mixin.one/",
)
