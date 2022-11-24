package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

class DeauthorRequest(
    @SerializedName("client_id")
    val clientId: String,
)
