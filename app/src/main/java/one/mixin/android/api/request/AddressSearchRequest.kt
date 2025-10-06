package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class AddressSearchRequest(
    @SerializedName("addresses")
    val addresses: List<String>
)
