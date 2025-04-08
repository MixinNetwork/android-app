package one.mixin.android.web3.details

import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemWeb3TokenHeaderBinding
import one.mixin.android.databinding.ItemWeb3TransactionsBinding
import one.mixin.android.db.web3.vo.TransactionStatus
import one.mixin.android.db.web3.vo.TransactionType
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.extension.colorAttr
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.textColorResource
import one.mixin.android.ui.conversation.adapter.MenuType
import one.mixin.android.ui.home.web3.StakeAccountSummary
import one.mixin.android.widget.BadgeAvatarView

class Web3TransactionHolder(val binding: ItemWeb3TransactionsBinding) : RecyclerView.ViewHolder(binding.root) {

    @SuppressLint("SetTextI18s")
    fun bind(transaction: Web3TransactionItem) {
        binding.apply {
            name.text = transaction.transactionHash

            val amount = transaction.getFormattedAmount()
            when  {
                transaction.status == TransactionStatus.PENDING.value || transaction.status == TransactionStatus.NOT_FOUND.value -> {
                    value.setTextColor(root.context.colorAttr(R.attr.text_assist))
                    value.text = ""
                    symbolTv.text = itemView.context.getString(R.string.Pending)
                }
                transaction.transactionType == TransactionType.TRANSFER_IN.value -> {
                    value.textColorResource = R.color.wallet_green
                    value.text = "+${amount.numberFormat8()}"
                    symbolTv.text = transaction.receiveAssetSymbol ?: ""
                    avatar.loadUrl(url = transaction.receiveAssetIconUrl ?: transaction.chainIconUrl, holder = R.drawable.ic_avatar_place_holder)
                }
                transaction.transactionType == TransactionType.TRANSFER_OUT.value -> {
                    value.textColorResource = R.color.wallet_pink
                    value.text = "-${amount.numberFormat8()}"
                    symbolTv.text = transaction.sendAssetSymbol ?: ""
                    avatar.loadUrl(url = transaction.sendAssetIconUrl ?: transaction.chainIconUrl, holder = R.drawable.ic_avatar_place_holder)
                }
                transaction.transactionType == TransactionType.SWAP.value -> {
                    value.textColorResource = R.color.wallet_green
                    value.text = "+${amount.numberFormat8()}"
                    symbolTv.text = transaction.receiveAssetSymbol ?: ""
                    avatar.loadUrl(url = transaction.receiveAssetIconUrl ?: transaction.chainIconUrl, holder = R.drawable.ic_avatar_place_holder)
                }
                transaction.transactionType == TransactionType.APPROVAL.value -> {
                    avatar.loadUrl(url = transaction.chainIconUrl, holder = R.drawable.ic_avatar_place_holder)
                    value.setTextColor(root.context.colorAttr(R.attr.text_primary))
                    value.text = ""
                    symbolTv.text = itemView.context.getString(R.string.Approval)
                }
                else -> {
                    avatar.loadUrl(url = transaction.chainIconUrl, holder = R.drawable.ic_avatar_place_holder)
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
