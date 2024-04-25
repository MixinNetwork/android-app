package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class Web3Transaction(
    val id: String,
    @SerializedName("transaction_hash")
    val transactionHash: String,
    @SerializedName("operation_type")
    val operationType: String,
    val status: String,
    val sender: String,
    val receiver: String,
    val fee: Web3Fee,
    val transfers: List<Web3Transfer>,
    val approvals:List<Approval>,
    @SerializedName("app_metadata")
    val appMetadata: AppMetadata?,
)