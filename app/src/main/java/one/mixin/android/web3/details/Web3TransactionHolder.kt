package one.mixin.android.web3.details

import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemWeb3TokenHeaderBinding
import one.mixin.android.databinding.ItemWeb3TransactionsBinding
import one.mixin.android.db.web3.vo.TransactionType
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.extension.colorAttr
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.textColorResource
import one.mixin.android.ui.home.web3.StakeAccountSummary
import one.mixin.android.widget.BadgeAvatarView

class Web3TransactionHolder(val binding: ItemWeb3TransactionsBinding) : RecyclerView.ViewHolder(binding.root) {

    @SuppressLint("SetTextI18s")
    fun bind(transaction: Web3TransactionItem) {
        binding.apply {
            name.text = transaction.transactionHash

            val amount = transaction.getFormattedAmount()
            val symbol = transaction.symbol
            avatar.loadUrl(transaction)
            when (transaction.status) {
                TransactionType.TxSuccess.value -> {
                    badge.setImageResource(R.drawable.ic_web3_status_success)
                }

                TransactionType.TxPending.value -> {
                    badge.setImageResource(R.drawable.ic_web3_status_pending)
                }

                else -> {
                    badge.setImageResource(R.drawable.ic_web3_status_failed)
                }
            }
            when (transaction.transactionType) {
                Web3TransactionType.Receive.value -> {
                    value.textColorResource = R.color.wallet_green
                    value.text = "+${amount.numberFormat8()}"
                    symbolTv.text = symbol
                }
                Web3TransactionType.Send.value -> {
                    value.textColorResource = R.color.wallet_pink
                    value.text = "-${amount.numberFormat8()}"
                    symbolTv.text = symbol
                }
                else -> {
                    avatar.loadUrl(url = transaction.iconUrl, holder = R.drawable.ic_avatar_place_holder)
                    value.setTextColor(root.context.colorAttr(R.attr.text_primary))
                    value.text = amount.numberFormat8()
                    symbolTv.text = symbol
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
