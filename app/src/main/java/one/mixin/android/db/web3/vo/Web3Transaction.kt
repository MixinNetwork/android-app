package one.mixin.android.db.web3.vo

import android.content.Context
import android.os.Parcelable
import android.text.SpannedString
import androidx.recyclerview.widget.DiffUtil
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import one.mixin.android.R
import one.mixin.android.api.response.AppMetadata
import one.mixin.android.api.response.Approval
import one.mixin.android.api.response.Web3Fee
import one.mixin.android.api.response.Web3Transfer
import one.mixin.android.event.TokenEvent
import one.mixin.android.extension.buildAmountSymbol
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.util.needsSpaceBetweenWords
import one.mixin.android.vo.Fiats
import one.mixin.android.web3.details.Web3TransactionDirection
import one.mixin.android.web3.details.Web3TransactionStatus
import one.mixin.android.web3.details.Web3TransactionType
import java.math.BigDecimal
import java.util.Locale

@Entity(tableName = "web3_transactions")
@Parcelize
data class Web3Transaction(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @SerializedName("transaction_hash")
    @ColumnInfo(name = "transaction_hash")
    val transactionHash: String,
    @SerializedName("operation_type")
    @ColumnInfo(name = "operation_type")
    val operationType: String,
    @SerializedName("status")
    @ColumnInfo(name = "status")
    val status: String,
    @SerializedName("sender")
    @ColumnInfo(name = "sender")
    val sender: String,
    @SerializedName("receiver")
    @ColumnInfo(name = "receiver")
    val receiver: String,
    @SerializedName("fee")
    @ColumnInfo(name = "fee")
    val fee: Web3Fee,
    @SerializedName("transfers")
    @ColumnInfo(name = "transfers")
    val transfers: List<Web3Transfer>,
    @SerializedName("approvals")
    @ColumnInfo(name = "approvals")
    val approvals: List<Approval>,
    @SerializedName("app_metadata")
    @ColumnInfo(name = "app_metadata")
    val appMetadata: AppMetadata?,
    @SerializedName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String,
) : Parcelable {

    companion object {
        val DIFF_CALLBACK =
            object : DiffUtil.ItemCallback<Web3Transaction>() {
                override fun areItemsTheSame(
                    oldItem: Web3Transaction,
                    newItem: Web3Transaction,
                ) =
                    oldItem.id == newItem.id

                override fun areContentsTheSame(
                    oldItem: Web3Transaction,
                    newItem: Web3Transaction,
                ) =
                    oldItem == newItem
            }
    }

    val icon: String?
        get() {
            when (operationType) {
                Web3TransactionType.Withdraw.value -> {
                    return transfers.firstOrNull { it.direction == Web3TransactionDirection.In.value }?.iconUrl
                }

                Web3TransactionType.Send.value -> {
                    return (transfers.firstOrNull { it.direction == Web3TransactionDirection.Out.value } ?: transfers.firstOrNull { it.direction == Web3TransactionDirection.Self.value })?.iconUrl
                }

                Web3TransactionType.Receive.value -> {
                    return transfers.firstOrNull { it.direction == Web3TransactionDirection.In.value }?.iconUrl
                }

                Web3TransactionType.Trade.value -> {
                    return transfers.firstOrNull { it.direction == Web3TransactionDirection.Out.value }?.iconUrl
                }

                Web3TransactionType.Execute.value -> {
                    return transfers.firstOrNull { it.direction == Web3TransactionDirection.Out.value }?.iconUrl
                }

                Web3TransactionType.Deposit.value -> {
                    return transfers.firstOrNull { it.direction == Web3TransactionDirection.Out.value }?.iconUrl
                }

                Web3TransactionType.Approve.value -> {
                    return fee.iconUrl
                }

                else -> {
                    return fee.iconUrl
                }
            }
        }


    val event: TokenEvent?
        get() {
            when (operationType) {
                Web3TransactionType.Withdraw.value -> {
                    return transfers.firstOrNull { it.direction == Web3TransactionDirection.In.value }?.let {
                        TokenEvent(it.chainId, it.assetKey)
                    }
                }
                Web3TransactionType.Send.value -> {
                    return (transfers.firstOrNull { it.direction == Web3TransactionDirection.Out.value } ?: transfers.firstOrNull { it.direction == Web3TransactionDirection.Self.value })?.let {
                        TokenEvent(it.chainId, it.assetKey)
                    }
                }
                Web3TransactionType.Receive.value -> {
                    return transfers.firstOrNull { it.direction == Web3TransactionDirection.In.value }?.let {
                        TokenEvent(it.chainId, it.assetKey)
                    }
                }

                Web3TransactionType.Trade.value -> {
                    return transfers.firstOrNull { it.direction == Web3TransactionDirection.Out.value }?.let {
                        TokenEvent(it.chainId, it.assetKey)
                    }
                }

                Web3TransactionType.Execute.value -> {
                    return transfers.firstOrNull { it.direction == Web3TransactionDirection.Out.value }?.let {
                        TokenEvent(it.chainId, it.assetKey)
                    }
                }

                Web3TransactionType.Deposit.value -> {
                    return transfers.firstOrNull { it.direction == Web3TransactionDirection.Out.value }?.let {
                        TokenEvent(it.chainId, it.assetKey)
                    }
                }

                Web3TransactionType.Approve.value -> {
                    // Todo
                    return null
                }

                else -> {
                    // Todo
                    return null
                }
            }
        }

    val badge: String?
        get() {
            return when (operationType) {
                else -> null
            }
        }

    fun title(context: Context): String {
        return when (operationType) {
            Web3TransactionType.Send.value -> {
                if (sender == receiver) {
                    context.getString(R.string.web3_receive)
                } else {
                    context.getString(R.string.web3_send)
                }
            }

            Web3TransactionType.Receive.value -> {
                context.getString(R.string.web3_receive)
            }

            Web3TransactionType.Withdraw.value -> {
                context.getString(R.string.web3_withdraw)
            }

            Web3TransactionType.Trade.value -> {
                context.getString(R.string.web3_trade)
            }

            Web3TransactionType.Approve.value -> {
                context.getString(R.string.web3_approve)
            }

            Web3TransactionType.Borrow.value -> {
                context.getString(R.string.web3_borrow)
            }

            Web3TransactionType.Burn.value -> {
                context.getString(R.string.web3_burn)
            }

            Web3TransactionType.Cancel.value -> {
                context.getString(R.string.web3_cancel)
            }

            Web3TransactionType.Claim.value -> {
                context.getString(R.string.web3_claim)
            }

            Web3TransactionType.Deploy.value -> {
                context.getString(R.string.web3_deploy)
            }

            Web3TransactionType.Deposit.value -> {
                context.getString(R.string.web3_deposit)
            }

            Web3TransactionType.Execute.value -> {
                context.getString(R.string.web3_execute)
            }

            Web3TransactionType.Mint.value -> {
                context.getString(R.string.web3_mint)
            }

            Web3TransactionType.Repay.value -> {
                context.getString(R.string.web3_repay)
            }

            Web3TransactionType.Stake.value -> {
                context.getString(R.string.web3_stake)
            }

            Web3TransactionType.Unstake.value -> {
                context.getString(R.string.web3_unstake)
            }

            Web3TransactionType.NftMint.value -> {
                context.getString(R.string.web3_nft_mint)
            }

            Web3TransactionType.NftTransfer.value -> {
                context.getString(R.string.web3_nft_transfer)
            }

            Web3TransactionType.NftBurn.value -> {
                context.getString(R.string.web3_nft_burn)
            }

            else ->
                operationType.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
        }.run {
            if (status == Web3TransactionStatus.Failed.value) {
                "$this${
                    if (needsSpaceBetweenWords()) {
                        " "
                    } else {
                        ""
                    }
                }${context.getString(R.string.Failed)}"
            } else {
                this
            }
        }
    }

    val subTitle: String
        get() {
            return when (operationType) {
                Web3TransactionType.Send.value -> {
                    receiver
                }

                Web3TransactionType.Receive.value -> {
                    sender
                }

                Web3TransactionType.Withdraw.value -> {
                    "${transfers.find { it.direction == Web3TransactionDirection.Out.value }?.symbol} -> ${transfers.find { it.direction == Web3TransactionDirection.In.value }?.symbol}"
                }

                Web3TransactionType.Trade.value -> {
                    "${transfers.find { it.direction == Web3TransactionDirection.Out.value }?.symbol} -> ${transfers.find { it.direction == Web3TransactionDirection.In.value }?.symbol}"
                }

                Web3TransactionType.Deposit.value -> {
                    "${transfers.find { it.direction == Web3TransactionDirection.Out.value }?.symbol} -> ${transfers.find { it.direction == Web3TransactionDirection.In.value }?.symbol}"
                }

                else -> transactionHash
            }
        }

    fun value(context: Context): SpannedString {
        return when (operationType) {
            Web3TransactionType.Receive.value -> {
                transfers.find { it.direction == Web3TransactionDirection.In.value }?.run {
                    buildAmountSymbol(context, "+${amount.numberFormat()}", symbol, context.resources.getColor(if (status == Web3TransactionStatus.Pending.value) R.color.wallet_text_gray else R.color.wallet_green, null), context.colorFromAttribute(R.attr.text_primary))
                }
            }

            Web3TransactionType.Deposit.value -> {
                transfers.find { it.direction == Web3TransactionDirection.Out.value }?.run {
                    buildAmountSymbol(context, "-${amount.numberFormat()}", symbol, context.resources.getColor(if (status == Web3TransactionStatus.Pending.value) R.color.wallet_text_gray else R.color.wallet_pink, null), context.colorFromAttribute(R.attr.text_primary))
                }
            }

            Web3TransactionType.Trade.value -> {
                transfers.find { it.direction == Web3TransactionDirection.Out.value }?.run {
                    buildAmountSymbol(context, "-${amount.numberFormat()}", symbol, context.resources.getColor(if (status == Web3TransactionStatus.Pending.value) R.color.wallet_text_gray else R.color.wallet_pink, null), context.colorFromAttribute(R.attr.text_primary))
                }
            }

            Web3TransactionType.Send.value -> {
                transfers.find { it.direction == Web3TransactionDirection.Out.value }?.run {
                    buildAmountSymbol(context, "-${amount.numberFormat()}", symbol, context.resources.getColor(if (status == Web3TransactionStatus.Pending.value) R.color.wallet_text_gray else R.color.wallet_pink, null), context.colorFromAttribute(R.attr.text_primary))
                } ?: transfers.find { it.direction == Web3TransactionDirection.Self.value }?.run {
                    buildAmountSymbol(context, "+${amount.numberFormat()}", symbol, context.resources.getColor(if (status == Web3TransactionStatus.Pending.value) R.color.wallet_text_gray else R.color.wallet_green, null), context.colorFromAttribute(R.attr.text_primary))
                }
            }

            Web3TransactionType.Execute.value -> {
                transfers.find { it.direction == Web3TransactionDirection.Out.value }?.run {
                    buildAmountSymbol(context, "-${amount.numberFormat()}", symbol, context.resources.getColor(if (status == Web3TransactionStatus.Pending.value) R.color.wallet_text_gray else R.color.wallet_pink, null), context.colorFromAttribute(R.attr.text_primary))
                }
            }

            else -> {
                buildAmountSymbol(context, "-${fee.amount.numberFormat()}", fee.symbol, context.resources.getColor(if (status == Web3TransactionStatus.Pending.value) R.color.wallet_text_gray else R.color.wallet_pink, null), context.colorFromAttribute(R.attr.text_primary))
            }
        }
            ?: SpannedString(operationType.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
    }

    val valueAs: String
        get() {
            return when (operationType) {
                Web3TransactionType.Receive.value -> {
                    transfers.find { it.direction == Web3TransactionDirection.In.value }?.run {
                        "≈ ${Fiats.getSymbol()}${BigDecimal(price).multiply(BigDecimal(Fiats.getRate())).multiply(BigDecimal(amount)).numberFormat2()}"
                    }
                }

                Web3TransactionType.Deposit.value -> {
                    transfers.find { it.direction == Web3TransactionDirection.Out.value }?.run {
                        "≈ ${Fiats.getSymbol()}${BigDecimal(price).multiply(BigDecimal(Fiats.getRate())).multiply(BigDecimal(amount)).numberFormat2()}"
                    }
                }

                Web3TransactionType.Trade.value -> {
                    transfers.find { it.direction == Web3TransactionDirection.Out.value }?.run {
                        "≈ ${Fiats.getSymbol()}${BigDecimal(price).multiply(BigDecimal(Fiats.getRate())).multiply(BigDecimal(amount)).numberFormat2()}"
                    }
                }

                Web3TransactionType.Send.value -> {
                    (transfers.find { it.direction == Web3TransactionDirection.Out.value } ?: transfers.find { it.direction == Web3TransactionDirection.Self.value })?.run {
                        "≈ ${Fiats.getSymbol()}${BigDecimal(price).multiply(BigDecimal(Fiats.getRate())).multiply(BigDecimal(amount)).numberFormat2()}"
                    }
                }

                Web3TransactionType.Execute.value -> {
                    transfers.find { it.direction == Web3TransactionDirection.Out.value }?.run {
                        "≈ ${Fiats.getSymbol()}${BigDecimal(price).multiply(BigDecimal(Fiats.getRate())).multiply(BigDecimal(amount)).numberFormat2()}"
                    }
                }

                else -> {
                    "≈ ${Fiats.getSymbol()}${BigDecimal(fee.price).multiply(BigDecimal(Fiats.getRate())).multiply(BigDecimal(fee.amount)).numberFormat2()}"
                }
            } ?: operationType.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
}
