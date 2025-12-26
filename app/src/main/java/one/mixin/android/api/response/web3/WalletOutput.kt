package one.mixin.android.api.response.web3

import com.google.gson.annotations.SerializedName

data class WalletOutput(
    @SerializedName("output_id")
    val outputId: String,
    @SerializedName("transaction_hash")
    val transactionHash: String,
    @SerializedName("output_index")
    val outputIndex: Long,
    @SerializedName("amount")
    val amount: String,
    @SerializedName("address")
    val address: String,
    @SerializedName("pubkey_hex")
    val pubkeyHex: String,
    @SerializedName("pubkey_type")
    val pubkeyType: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
)
