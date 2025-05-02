package one.mixin.android.web3.details

import android.annotation.SuppressLint
import android.util.TypedValue
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemWeb3TokenHeaderBinding
import one.mixin.android.databinding.ItemWeb3TransactionsBinding
import one.mixin.android.db.web3.vo.TransactionStatus
import one.mixin.android.db.web3.vo.TransactionType
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.extension.colorAttr
import one.mixin.android.extension.numberFormat12
import one.mixin.android.extension.textColorResource
import one.mixin.android.ui.home.web3.StakeAccountSummary

class Web3TransactionHolder(val binding: ItemWeb3TransactionsBinding) : RecyclerView.ViewHolder(binding.root) {

    private fun formatAmountWithSign(amount: String, positive: Boolean): String {
        val formattedAmount = amount.numberFormat12()
        return if (positive) {
            if (formattedAmount.startsWith("+")) formattedAmount else "+$formattedAmount"
        } else {
            if (formattedAmount.startsWith("-")) formattedAmount else "-$formattedAmount"
        }
    }

    @SuppressLint("SetTextI18s")
    fun bind(transaction: Web3TransactionItem) {
        binding.apply {
            val hash = transaction.transactionHash
            name.text = if (hash.length > 14) {
                "${hash.substring(0, 8)}...${hash.substring(hash.length - 6)}"
            } else {
                hash
            }

            val amount = transaction.getFormattedAmount()
            when {
                transaction.status == TransactionStatus.PENDING.value || transaction.status == TransactionStatus.NOT_FOUND.value -> {
                    value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                    amountAnimator.displayedChild = 0
                    value.setTextColor(root.context.colorAttr(R.attr.text_assist))
                    value.text = ""
                    symbolTv.text =
                        itemView.context.getString(if (transaction.status == TransactionStatus.NOT_FOUND.value) R.string.Expired else R.string.Pending)
                    avatar.loadUrl(transaction)
                }
                transaction.transactionType == TransactionType.UNKNOWN.value -> {
                    value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                    amountAnimator.displayedChild = 0
                    value.setTextColor(root.context.colorAttr(R.attr.text_assist))
                    value.text = ""
                    symbolTv.text = itemView.context.getString(R.string.Unknown)
                    avatar.loadUrl(transaction)
                }
                transaction.transactionType == TransactionType.TRANSFER_IN.value -> {
                    value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                    amountAnimator.displayedChild = 0
                    value.textColorResource = R.color.wallet_green
                    value.text = formatAmountWithSign(amount, true)
                    symbolTv.text = transaction.receiveAssetSymbol ?: ""
                    avatar.loadUrl(transaction)
                }
                transaction.transactionType == TransactionType.TRANSFER_OUT.value -> {
                    value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                    amountAnimator.displayedChild = 0
                    value.textColorResource = R.color.wallet_pink
                    value.text = formatAmountWithSign(amount, false)
                    symbolTv.text = transaction.sendAssetSymbol ?: ""
                    avatar.loadUrl(transaction)
                }
                transaction.transactionType == TransactionType.SWAP.value -> {
                    value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                    if (transaction.senders.isNotEmpty()) {
                        amountAnimator.displayedChild = 1
                        
                        receiveValue.textColorResource = R.color.wallet_green
                        receiveValue.text = formatAmountWithSign(amount, true)
                        receiveSymbolTv.text = transaction.receiveAssetSymbol ?: ""
                        
                        val sendAmount = try {
                            transaction.senders[0].amount.numberFormat12()
                        } catch (e: Exception) {
                            transaction.senders[0].amount
                        }
                        sendValue.textColorResource = R.color.wallet_pink
                        sendValue.text = formatAmountWithSign(sendAmount, false)
                        sendSymbolTv.text = transaction.sendAssetSymbol ?: ""
                    } else {
                        amountAnimator.displayedChild = 0
                        value.textColorResource = R.color.wallet_green
                        value.text = formatAmountWithSign(amount, true)
                        symbolTv.text = transaction.receiveAssetSymbol ?: ""
                    }
                    avatar.loadUrl(transaction)
                }
                transaction.transactionType == TransactionType.APPROVAL.value -> {
                    amountAnimator.displayedChild = 0
                    value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    avatar.loadUrl(transaction)
                    
                    val approvals = transaction.approvals
                    if (approvals != null && approvals.isNotEmpty()) {
                        val approvalAssetChange = approvals[0]
                        val isUnlimited = approvalAssetChange.type == "unlimited"
                        
                        if (isUnlimited) {
                            value.textColorResource = R.color.wallet_pink
                            value.text = itemView.context.getString(R.string.unlimited)
                            symbolTv.text = transaction.sendAssetSymbol ?: ""
                        } else {
                            value.textColorResource = R.color.wallet_pink
                            value.text = itemView.context.getString(R.string.Approved)
                            symbolTv.text = "${approvalAssetChange.amount} ${transaction.sendAssetSymbol ?: ""}"
                        }
                    } else {
                        value.textColorResource = R.color.wallet_pink
                        value.text = itemView.context.getString(R.string.Approved)
                        symbolTv.text = transaction.sendAssetSymbol ?: ""
                    }
                }
                else -> {
                    value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                    amountAnimator.displayedChild = 0
                    avatar.loadUrl(transaction)
                    value.setTextColor(root.context.colorAttr(R.attr.text_primary))
                    value.text = ""
                    symbolTv.text = ""
                }
            }
            when (transaction.status) {
                TransactionStatus.SUCCESS.value -> {
                    badge.setImageResource(R.drawable.ic_web3_status_success)
                }

                TransactionStatus.PENDING.value -> {
                    badge.setImageResource(R.drawable.ic_web3_status_pending)
                }

                else -> {
                    badge.setImageResource(R.drawable.ic_web3_status_failed)
                }
            }
        }
    }
}

class Web3HeaderHolder(val binding: ItemWeb3TokenHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(
        token: Web3TokenItem,
        summary: StakeAccountSummary?,
        onClickListener: ((Int) -> Unit)?,
    ) {
        binding.header.setToken(token)
        binding.header.setOnClickAction(onClickListener)
        binding.header.showStake(summary)
    }

    fun enableSwap() {
        binding.header.enableSwap()
    }
}
