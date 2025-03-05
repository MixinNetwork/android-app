package one.mixin.android.web3.details

import android.annotation.SuppressLint
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemWalletTransactionsBinding
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.databinding.ItemWeb3TokenHeaderBinding
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.textColorResource
import one.mixin.android.ui.home.web3.StakeAccountSummary

class Web3TransactionHolder(val binding: ItemWalletTransactionsBinding) : RecyclerView.ViewHolder(binding.root) {
    @SuppressLint("SetTextI18s")
    fun bind(transaction: Web3TransactionItem) {
        binding.apply {
            name.text = transaction.transactionHash

            avatar.loadUrl(url = transaction.iconUrl, holder = R.drawable.ic_avatar_place_holder)

            val amount = transaction.getFormattedAmount()
            val symbol = transaction.symbol
            
            if (transaction.transactionType == Web3TransactionType.Receive.value) {
                value.textColorResource = R.color.wallet_green
                value.text = "+${amount.numberFormat8()}"
                symbolTv.text = symbol
                symbolIv.isVisible = false
            } else if (transaction.transactionType == Web3TransactionType.Send.value) {
                value.textColorResource = R.color.wallet_green
                value.text = "-${amount.numberFormat8()}"
                symbolTv.text = symbol
                symbolIv.isVisible = false
            } else {
                value.setTextColor(root.context.colorFromAttribute(R.attr.text_primary))
                value.text = amount.numberFormat8()
                symbolTv.text = symbol
                symbolIv.isVisible = false
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
