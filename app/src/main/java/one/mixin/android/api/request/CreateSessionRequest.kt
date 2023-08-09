package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

class CreateSessionRequest(
    @SerializedName("source")
    val source: Source,
    @SerializedName("currency")
    val currency: String,
    @SerializedName("completion")
    val completion: CompletionInfo,
)

class Source()

class CompletionInfo()
