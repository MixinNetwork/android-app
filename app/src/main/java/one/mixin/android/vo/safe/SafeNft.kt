package one.mixin.android.vo.safe

import com.google.gson.annotations.SerializedName

class SafeNft(
    val amount: String,
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("inscription_hash")
    val inscriptionHash: String,
    val memo: String?,
    @SerializedName("opponent_id")
    val opponentId: String,
    @SerializedName("snapshot_id")
    val snapshotId: String,
    @SerializedName("transaction_hash")
    val transactionHash: String,
    @SerializedName("user_id")
    val userId: String,
)

