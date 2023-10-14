package one.mixin.android.vo

import com.google.gson.annotations.SerializedName

data class Deposit(
    @SerializedName("entry_id")
    val entryId: String,
    @SerializedName("threshold")
    val threshold: Int,
    @SerializedName("destination")
    val destination: String,
    @SerializedName("tag")
    val tag: String,
    @SerializedName("chain_id")
    val chainId: String,
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("priority")
    val priority: Int,
    @SerializedName("members")
    val members: List<String>,
)