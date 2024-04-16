package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

class Web3Account(
    val address: String,
    @SerializedName("change_absolute_1d")
    val changeAbsolute1d: String,
    val tokens: List<Web3Token>,
    @SerializedName("change_percent_1d")
    val changePercent1d: String,
    @SerializedName("balance")
    val balance: String,
)