package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class OutputFetchRequest(
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("ids")
    val ids: List<String>
)
