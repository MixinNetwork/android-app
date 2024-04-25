package one.mixin.android.api.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import one.mixin.android.web3.details.Web3TransactionDirection
import one.mixin.android.web3.details.Web3TransactionType

@Parcelize
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
    val approvals: List<Approval>,
    @SerializedName("app_metadata")
    val appMetadata: AppMetadata?,
) : Parcelable {
    val icon: String?
        get() {
            when (operationType) {
                Web3TransactionType.Approve.value -> {
                    return approvals.firstOrNull()?.iconUrl
                }

                Web3TransactionType.Withdraw.value -> {
                    return transfers.firstOrNull { it.direction == Web3TransactionDirection.In.value }?.iconUrl
                }

                Web3TransactionType.Send.value -> {
                    return transfers.firstOrNull { it.direction == Web3TransactionDirection.Out.value }?.iconUrl
                }

                Web3TransactionType.Receive.value -> {
                    return transfers.firstOrNull { it.direction == Web3TransactionDirection.In.value }?.iconUrl
                }

                Web3TransactionType.Trade.value -> {
                    return transfers.firstOrNull { it.direction == Web3TransactionDirection.In.value }?.iconUrl
                }
            }
            return null
        }

    val badge: String?
        get() {
            return when (operationType) {
                Web3TransactionType.Approve.value -> {
                    approvals.firstOrNull()?.iconUrl
                }
                Web3TransactionType.Withdraw.value,
                Web3TransactionType.Receive.value,
                Web3TransactionType.Send.value,
                Web3TransactionType.Trade.value -> {
                    fee.iconUrl
                }

                else -> null
            }
        }
}