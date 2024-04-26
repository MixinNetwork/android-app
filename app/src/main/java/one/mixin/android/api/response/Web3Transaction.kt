package one.mixin.android.api.response

import android.content.Context
import android.os.Parcelable
import android.text.SpannedString
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.buildAmountSymbol
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat
import one.mixin.android.vo.Fiats
import one.mixin.android.web3.details.Web3TransactionDirection
import one.mixin.android.web3.details.Web3TransactionStatus
import one.mixin.android.web3.details.Web3TransactionType
import java.math.BigDecimal
import java.util.Locale

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
    @SerializedName("created_at")
    val createdAt: String
) : Parcelable {
    val icon: String?
        get() {
            when (operationType) {
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
                    fee.iconUrl
                }
            }
            return null
        }

    val badge: String?
        get() {
            return when (operationType) {
                else -> null
            }
        }

    val title: String
        get() {
            return when (operationType) {
                Web3TransactionType.Send.value -> {
                    MixinApplication.appContext.getString(R.string.Send_transfer)
                }

                Web3TransactionType.Receive.value -> {
                    MixinApplication.appContext.getString(R.string.Receive)
                }

                Web3TransactionType.Withdraw.value -> {
                    MixinApplication.appContext.getString(R.string.Withdrawal)
                }

                Web3TransactionType.Trade.value ->{
                    MixinApplication.appContext.getString(R.string.Trade)
                }

                else -> operationType.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
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
                    transfers.find { it.direction == Web3TransactionDirection.Out.value }?.run {
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