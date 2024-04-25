package one.mixin.android.web3.details

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.text.toUpperCase
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.api.response.Web3Token
import one.mixin.android.api.response.Web3Transaction
import one.mixin.android.databinding.ItemWeb3TokenBinding
import one.mixin.android.databinding.ItemWeb3TransactionBinding
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.textColor
import one.mixin.android.extension.textColorResource
import one.mixin.android.vo.Fiats
import java.math.BigDecimal
import java.util.Locale

class Web3TransactionAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    fun isEmpty() = transactions.isEmpty()

    var transactions: List<Web3Transaction> = emptyList()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    private var onClickListener: ((Web3Transaction) -> Unit)? = null
    fun setOnClickListener(onClickListener: (Web3Transaction) -> Unit) {
        this.onClickListener = onClickListener
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        return Web3TransactionHolder(ItemWeb3TransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return transactions.size
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {

        (holder as Web3TransactionHolder).bind(transactions[position])
        holder.itemView.setOnClickListener {
            onClickListener?.invoke(transactions[position])
        }
    }
}

class Web3TransactionHolder(val binding: ItemWeb3TransactionBinding) : RecyclerView.ViewHolder(binding.root) {
    @SuppressLint("SetTextI18n")
    fun bind(transaction: Web3Transaction) {
        binding.apply {
            when(transaction.operationType) {
                Web3TransactionType.Send.value ->{
                    avatar.bg.setImageResource(R.drawable.ic_snapshot_withdrawal)
                    titleTv.setText(R.string.Send_transfer)
                    subTitleTv.text = transaction.receiver.formatPublicKey(16)
                    if (transaction.transfers.isNotEmpty()) {
                        transaction.transfers.find { it.direction == Web3TransactionDirection.Out.value }?.let { outTransfer ->
                            inTv.textColorResource = R.color.wallet_pink
                            inTv.text = "-${outTransfer.amount.numberFormat8()}"
                            inSymbolTv.text = outTransfer.symbol
                            outSymbolTv.text = "${Fiats.getSymbol()}${BigDecimal(outTransfer.price).multiply(BigDecimal(Fiats.getRate())).multiply(BigDecimal(outTransfer.amount)).numberFormat2()}"
                            outSymbolTv.textColor = root.context.colorFromAttribute(R.attr.text_assist)
                            outTv.isVisible = false
                        }
                    }
                }
                Web3TransactionType.Receive.value ->{
                    avatar.bg.setImageResource(R.drawable.ic_snapshot_deposit)
                    titleTv.setText(R.string.Receive)
                    subTitleTv.text = transaction.sender.formatPublicKey(16)
                    if (transaction.transfers.isNotEmpty()) {
                        transaction.transfers.find { it.direction == Web3TransactionDirection.In.value }?.let { inTransfer ->
                            inTv.textColorResource = R.color.wallet_green
                            inTv.text = "+${inTransfer.amount.numberFormat8()}"
                            inSymbolTv.text = inTransfer.symbol
                            outSymbolTv.text = "${Fiats.getSymbol()}${BigDecimal(inTransfer.price).multiply(BigDecimal(Fiats.getRate())).multiply(BigDecimal(inTransfer.amount)).numberFormat2()}"
                            outSymbolTv.textColor = root.context.colorFromAttribute(R.attr.text_assist)
                            outTv.isVisible = false
                        }
                    }
                }
                Web3TransactionType.Withdraw.value -> {
                    avatar.bg.setImageResource(R.drawable.ic_snapshot_withdrawal)
                    titleTv.setText(R.string.Withdrawal)
                    subTitleTv.text = "${transaction.transfers.find { it.direction == Web3TransactionDirection.Out.value }?.symbol} -> ${transaction.transfers.find { it.direction == Web3TransactionDirection.In.value }?.symbol}"
                    if (transaction.transfers.isNotEmpty()) {
                        transaction.transfers.find { it.direction == Web3TransactionDirection.In.value }?.let { inTransfer ->
                            inTv.textColorResource = R.color.wallet_green
                            inTv.text = "+${inTransfer.amount.numberFormat8()}"
                            inSymbolTv.text = inTransfer.symbol
                            avatar.badge.loadImage(inTransfer.iconUrl, R.drawable.ic_avatar_place_holder)
                        }
                        transaction.transfers.find { it.direction == Web3TransactionDirection.Out.value }?.let { outTransfer ->
                            outTv.isVisible = true
                            outTv.textColorResource = R.color.wallet_pink
                            outTv.text = "-${outTransfer.amount.numberFormat8()}"
                            outSymbolTv.text = outTransfer.symbol
                            outSymbolTv.textColor = root.context.colorFromAttribute(R.attr.text_primary)
                        }
                    }
                }

                Web3TransactionType.Approve.value -> {
                    avatar.bg.loadImage(transaction.approvals.first().iconUrl, R.drawable.ic_no_dapp)
                    titleTv.text = transaction.operationType.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                }
                Web3TransactionType.Trade.value -> {
                    avatar.bg.loadImage(transaction.appMetadata?.iconUrl, R.drawable.ic_no_dapp)
                    titleTv.text = transaction.status.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    subTitleTv.text = "${transaction.transfers.find { it.direction == Web3TransactionDirection.Out.value }?.symbol} -> ${transaction.transfers.find { it.direction == Web3TransactionDirection.In.value }?.symbol}"
                    if (transaction.transfers.isNotEmpty()) {
                        transaction.transfers.find { it.direction == Web3TransactionDirection.In.value }?.let { inTransfer ->
                            inTv.textColorResource = R.color.wallet_green
                            inTv.text = "+${inTransfer.amount.numberFormat8()}"
                            inSymbolTv.text = inTransfer.symbol
                            avatar.badge.loadImage(inTransfer.iconUrl, R.drawable.ic_avatar_place_holder)
                        }
                        transaction.transfers.find { it.direction == Web3TransactionDirection.Out.value }?.let { outTransfer ->
                            outTv.isVisible = true
                            outTv.textColorResource = R.color.wallet_pink
                            outTv.text = "-${outTransfer.amount.numberFormat8()}"
                            outSymbolTv.text = outTransfer.symbol
                            outSymbolTv.textColor = root.context.colorFromAttribute(R.attr.text_primary)
                        }
                    }
                }
            }

        }
    }
}
