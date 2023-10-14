package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName
import one.mixin.android.session.Session

data class DepositEntryRequest(
    @SerializedName("members_hash")
    val membersHash: String,
    @SerializedName("threshold")
    val threshold: Int,
    @SerializedName("members")
    val members: List<String>,
    @SerializedName("chain_id")
    val chainId: String,
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("user_id")
    val user: String? = Session.getAccountId()
)