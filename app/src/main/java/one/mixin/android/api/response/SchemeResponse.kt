package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class SchemeResponse(
    @SerializedName("scheme_id")
    val schemeId: String,
    @SerializedName("target")
    val target: String,
)
