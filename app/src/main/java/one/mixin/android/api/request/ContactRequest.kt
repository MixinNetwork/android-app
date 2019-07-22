package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class ContactRequest(
    val phone: String,
    @SerializedName("full_name")
    val fullName: String
)
