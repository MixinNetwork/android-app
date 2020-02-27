package one.mixin.android.util.mention

import com.google.gson.annotations.SerializedName

data class MentionData(
    @SerializedName(value = "identity_number", alternate = ["identityNumber"])
    val identityNumber: String,
    @SerializedName(value = "full_name", alternate = ["fullName"])
    val fullName: String?
)
