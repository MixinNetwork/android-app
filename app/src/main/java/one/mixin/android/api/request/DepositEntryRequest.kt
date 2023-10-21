package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName
import one.mixin.android.session.Session

data class DepositEntryRequest(
    @SerializedName("chain_id")
    val chainId: String,
)
