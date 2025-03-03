package one.mixin.android.web3.details

import android.annotation.SuppressLint
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.Web3Transaction
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.databinding.ItemWeb3TokenHeaderBinding
import one.mixin.android.databinding.ItemWeb3TransactionBinding
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.textColor
import one.mixin.android.extension.textColorResource
import one.mixin.android.ui.home.web3.StakeAccountSummary
import one.mixin.android.widget.GrayscaleTransformation

class Web3TransactionHolder(val binding: ItemWeb3TransactionBinding) : RecyclerView.ViewHolder(binding.root) {
    @SuppressLint("SetTextI18n")
    fun bind(transaction: Web3Transaction) {
        binding.apply {
            titleTv.text = transaction.transactionHash
            subTitleTv.text = transaction.createdAt
            
            // 根据交易类型设置图标和颜色
            if (transaction.sender == transaction.receiver) {
                avatar.bg.setImageResource(R.drawable.ic_snapshot_deposit)
                avatar.badge.isVisible = false
            } else if (transaction.sender.isEmpty()) {
                avatar.bg.setImageResource(R.drawable.ic_snapshot_deposit)
                avatar.badge.isVisible = false
            } else {
                avatar.bg.setImageResource(R.drawable.ic_snapshot_withdrawal)
                avatar.badge.isVisible = false
            }
            
            // 设置金额和符号
            if (transaction.iconUrl.isNotEmpty()) {
                avatar.bg.loadImage(
                    transaction.iconUrl,
                    holder = R.drawable.ic_avatar_place_holder
                )
            }
            
            // 设置金额和符号
            val amount = transaction.amount
            val symbol = transaction.symbol
            
            // 根据交易方向设置颜色和符号
            if (transaction.sender == transaction.receiver) {
                // 自己给自己转账
                inTv.textColorResource = R.color.wallet_green
                inTv.text = amount
                inSymbolTv.text = symbol
                outTv.isVisible = false
                outSymbolTv.text = ""
                outSymbolTv.isVisible = false
            } else if (transaction.sender.isEmpty()) {
                // 接收交易
                inTv.textColorResource = R.color.wallet_green
                inTv.text = "+$amount"
                inSymbolTv.text = symbol
                outTv.isVisible = false
                outSymbolTv.text = ""
                outSymbolTv.isVisible = false
            } else {
                // 发送交易
                inTv.textColorResource = R.color.wallet_pink
                inTv.text = "-$amount"
                inSymbolTv.text = symbol
                outTv.isVisible = false
                outSymbolTv.text = ""
                outSymbolTv.isVisible = false
            }
        }
    }
    
    @SuppressLint("SetTextI18n")
    fun bind(transaction: Web3TransactionItem) {
        binding.apply {
            titleTv.text = transaction.transactionHash
            subTitleTv.text = transaction.createdAt
            
            // 设置图标
            if (transaction.iconUrl != null && transaction.iconUrl.isNotEmpty()) {
                avatar.bg.loadImage(
                    transaction.iconUrl,
                    holder = R.drawable.ic_avatar_place_holder
                )
            } else {
                // 根据交易类型设置默认图标
                if (transaction.sender == transaction.receiver) {
                    avatar.bg.setImageResource(R.drawable.ic_snapshot_deposit)
                } else if (transaction.sender.isEmpty()) {
                    avatar.bg.setImageResource(R.drawable.ic_snapshot_deposit)
                } else {
                    avatar.bg.setImageResource(R.drawable.ic_snapshot_withdrawal)
                }
            }
            avatar.badge.isVisible = false
            
            // 设置金额和符号
            val amount = transaction.getFormattedAmount()
            val symbol = transaction.symbol
            
            // 根据交易方向设置颜色和符号
            if (transaction.sender == transaction.receiver) {
                // 自己给自己转账
                inTv.textColorResource = R.color.wallet_green
                inTv.text = amount
                inSymbolTv.text = symbol
                outTv.isVisible = false
                outSymbolTv.text = ""
                outSymbolTv.isVisible = false
            } else if (transaction.sender.isEmpty()) {
                // 接收交易
                inTv.textColorResource = R.color.wallet_green
                inTv.text = "+$amount"
                inSymbolTv.text = symbol
                outTv.isVisible = false
                outSymbolTv.text = ""
                outSymbolTv.isVisible = false
            } else {
                // 发送交易
                inTv.textColorResource = R.color.wallet_pink
                inTv.text = "-$amount"
                inSymbolTv.text = symbol
                outTv.isVisible = false
                outSymbolTv.text = ""
                outSymbolTv.isVisible = false
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
