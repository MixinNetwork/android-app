package one.mixin.android.ui.wallet.adapter

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemWalletAssetBinding
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.dp
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.setQuoteText
import one.mixin.android.ui.common.recyclerview.HeaderAdapter
import one.mixin.android.ui.common.recyclerview.HeaderListUpdateCallback
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.util.debug.debugLongClick
import one.mixin.android.vo.Fiats
import java.math.BigDecimal

class WalletWeb3TokenAdapter(private val slideShow: Boolean) : HeaderAdapter<Web3TokenItem>() {
    fun setAssetList(newAssets: List<Web3TokenItem>) {
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
                            return old.assetId == new.assetId
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

    fun removeItem(pos: Int): Web3TokenItem? {
        val list = data?.toMutableList()
        val addr = list?.removeAt(getPosition(pos))
        data = list
        notifyItemRemoved(pos)
        return addr
    }

    fun restoreItem(
        item: Web3TokenItem,
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
                    if (asset.balance.isBlank()) {
                        "0.00"
                    } else if (asset.balance.numberFormat8().toFloat() == 0f) {
                        "0.00"
                    } else {
                        asset.balance.numberFormat8()
                    }
                } catch (ignored: NumberFormatException) {
                    asset.balance.numberFormat8()
                }
            binding.symbolTv.text = asset.symbol
            binding.icSpam.isVisible = asset.isSpam()
            if (asset.isSpam()) {
                binding.balance.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    marginStart = 2.dp
                }
            } else {
                binding.balance.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    marginStart = 16.dp
                }
            }
            binding.balanceAs.text = "≈ ${Fiats.getSymbol()}${asset.fiat().numberFormat2()}"
            if (asset.priceUsd == "0") {
                binding.naTv.visibility = View.VISIBLE
                binding.priceTv.visibility = View.GONE
                binding.changeTv.visibility = View.GONE
            } else {
                binding.naTv.visibility = View.GONE
                binding.priceTv.visibility = View.VISIBLE
                binding.changeTv.visibility = View.VISIBLE
                binding.priceTv.text = "${Fiats.getSymbol()}${asset.priceFiat().priceFormat()}"
                if (asset.changeUsd.isNotEmpty()) {
                    val changeUsd = BigDecimal(asset.changeUsd)
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
                        ?.setPrimaryClip(ClipData.newPlainText(null, asset.assetId))
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
