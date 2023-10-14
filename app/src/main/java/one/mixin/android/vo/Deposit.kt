package one.mixin.android.vo

import com.google.gson.annotations.SerializedName

data class Deposit(
    @SerializedName("entry_id")
    val entryId: String,
    @SerializedName("members_hash")
    val membersHash: String,
    @SerializedName("threshold")
    val threshold: String,
    @SerializedName("destination")
    val destination: String,
    @SerializedName("tag")
    val tag: String,
    @SerializedName("chain_id")
    val chainId: String,
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("priority")
    val priority: String,
    @SerializedName("members")
    val members: List<String>,
)