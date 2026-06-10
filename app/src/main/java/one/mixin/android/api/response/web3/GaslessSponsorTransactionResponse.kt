package one.mixin.android.api.response.web3

import com.google.gson.annotations.SerializedName
import one.mixin.android.db.web3.vo.TransactionStatus

data class GaslessSponsorTransactionResponse(
    @SerializedName("sponsor_tx_id")
    val sponsorTxId: String,
    @SerializedName("chain_id")
    val chainId: String,
    val account: String,
    @SerializedName("web3_chain_id")
    val web3ChainId: Int,
    val state: String,
    @SerializedName("broadcast_tx_hash")
    val broadcastTxHash: String? = null,
    val reason: String? = null,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
)

fun GaslessSponsorTransactionResponse.hasBroadcastTxHash(): Boolean =
    !broadcastTxHash.isNullOrBlank()

fun GaslessSponsorTransactionResponse.toPendingStatusOrNull(): String? {
    if (hasBroadcastTxHash()) return TransactionStatus.PENDING.value
    if (!reason.isNullOrBlank()) return TransactionStatus.FAILED.value
    return when (state.lowercase()) {
        "failed", "rejected", "cancelled", "canceled", "expired" -> TransactionStatus.FAILED.value
        else -> null
    }
}
