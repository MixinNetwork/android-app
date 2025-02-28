package one.mixin.android.ui.wallet.adapter

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.db.web3.vo.Web3Token
import one.mixin.android.databinding.ItemWalletAssetBinding
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.setQuoteText
import one.mixin.android.ui.common.recyclerview.HeaderAdapter
import one.mixin.android.ui.common.recyclerview.HeaderListUpdateCallback
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.util.debug.debugLongClick
import one.mixin.android.vo.Fiats
import java.math.BigDecimal

class WalletWeb3TokenAdapter(private val slideShow: Boolean) : HeaderAdapter<Web3Token>() {
    fun setAssetList(newAssets: List<Web3Token>) {
        if (data == null) {
            data = newAssets
            notifyItemRangeInserted(0, newAssets.size)
        } else {
            val diffResult =
                DiffUtil.calculateDiff(
                    object : DiffUtil.Callback() {
                        override fun areItemsTheSame(
                            oldItemPosition: Int,
                            newItemPosition: Int,
                        ): Boolean {
                            val old = data!![oldItemPosition]
                            val new = newAssets[newItemPosition]
                            return old.fungibleId == new.fungibleId
                        }

                        override fun getOldListSize() = data!!.size

                        override fun getNewListSize() = newAssets.size

                        override fun areContentsTheSame(
                            oldItemPosition: Int,
                            newItemPosition: Int,
                        ): Boolean {
                            val old = data!![oldItemPosition]
                            val new = newAssets[newItemPosition]
                            return old == new
                        }
                    },
                )
            data = newAssets
            if (headerView != null) {
                diffResult.dispatchUpdatesTo(HeaderListUpdateCallback(this))
            } else {
                diffResult.dispatchUpdatesTo(this)
            }
        }
    }

    fun removeItem(pos: Int): Web3Token? {
        val list = data?.toMutableList()
        val addr = list?.removeAt(getPosition(pos))
        data = list
        notifyItemRemoved(pos)
        return addr
    }

    fun restoreItem(
        item: Web3Token,
        pos: Int,
    ) {
        val list = data?.toMutableList()
        list?.add(getPosition(pos), item)
        data = list
        notifyItemInserted(pos)
    }

    fun getPosition(pos: Int) = getPos(pos)

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        if (holder is NormalHolder) {
            val binding = ItemWalletAssetBinding.bind(holder.itemView)
            val asset = data!![getPos(position)]
            binding.balance.text =
                try {
                    if (asset.balance.numberFormat().toFloat() == 0f) {
                        "0.00"
                    } else {
                        asset.balance.numberFormat()
                    }
                } catch (ignored: NumberFormatException) {
                    asset.balance.numberFormat()
                }
            binding.symbolTv.text = asset.symbol
            binding.balanceAs.text = "≈ ${Fiats.getSymbol()}${asset.fiat().numberFormat2()}"
            if (asset.price == "0") {
                binding.priceTv.setText(R.string.NA)
                binding.changeTv.visibility = GONE
            } else {
                binding.changeTv.visibility = VISIBLE
                binding.priceTv.text = "${Fiats.getSymbol()}${asset.priceFiat().priceFormat()}"
                if (asset.changePercent.isNotEmpty()) {
                    val changeUsd = BigDecimal(asset.changePercent)
                    val isRising = changeUsd >= BigDecimal.ZERO
                    binding.changeTv.setQuoteText("${(changeUsd).numberFormat2()}%", isRising)
                }
            }
            binding.backLeftTv.setText(if (slideShow) R.string.Shown else R.string.Hidden)
            binding.backRightTv.setText(if (slideShow) R.string.Shown else R.string.Hidden)
            binding.avatar.loadToken(asset)
            holder.itemView.setOnClickListener { onItemListener?.onNormalItemClick(asset) }
            debugLongClick(
                holder.itemView,
                {
                    holder.itemView.context?.getClipboardManager()
                        ?.setPrimaryClip(ClipData.newPlainText(null, asset.fungibleId))
                },
            )
        }
    }

    override fun getNormalViewHolder(
        context: Context,
        parent: ViewGroup,
    ): NormalHolder =
        NormalHolder(LayoutInflater.from(context).inflate(R.layout.item_wallet_asset, parent, false))
}
