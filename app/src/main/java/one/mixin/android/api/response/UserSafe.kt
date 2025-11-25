package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class UserSafe(
    @SerializedName("type")
    val type: String,
    @SerializedName("operation_id")
    val operationId: String,
    @SerializedName("account_id")
    val accountId: String,
    @SerializedName("chain_id")
    val chainId: Int,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("public")
    val public: String,
    @SerializedName("owners")
    val owners: List<String>,
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("threshold")
    val threshold: Int,
    @SerializedName("address")
    val address: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("role")
    val role: String,
    @SerializedName("account_type")
    val accountType: String,
    @SerializedName("keys")
    val keys: List<String>,
    @SerializedName("script")
    val script: String,
    @SerializedName("hmac")
    val hmac: String,
    @SerializedName("timelock")
    val timelock: Int,
    @SerializedName("lock_status")
    val lockStatus: String,
    @SerializedName("latest_utxo")
    val latestUtxo: String,
    @SerializedName("assets")
    val assets: List<SafeAsset>,
    @SerializedName("uri")
    val uri: String,
    @SerializedName("created_at")
    val createdAt: String,
)
