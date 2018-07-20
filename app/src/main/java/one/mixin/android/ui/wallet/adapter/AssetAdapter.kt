package one.mixin.android.ui.wallet.adapter

import android.annotation.SuppressLint
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_wallet_asset.view.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import one.mixin.android.R
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.util.BigDecimalCache
import one.mixin.android.util.BigDecimalCache.Companion.FORMAT_TYPE_0
import one.mixin.android.util.BigDecimalCache.Companion.FORMAT_TYPE_2
import one.mixin.android.util.BigDecimalCache.Companion.FORMAT_TYPE_8
import one.mixin.android.vo.AssetItem
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.textColorResource
import org.jetbrains.anko.uiThread
import java.math.BigDecimal

class AssetAdapter(private var assets: List<AssetItem>, private val rv: RecyclerView) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_NORMAL = 1
    }

    private var headerView: View? = null
    private var assetsListener: AssetsListener? = null

    fun setAssetList(assets: List<AssetItem>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                if (headerView != null) {
                    if (oldItemPosition == 0 && newItemPosition == 0) {
                        return true
                    } else if (oldItemPosition == 0 || newItemPosition == 0) {
                        return false
                    }
                }

                val old = this@AssetAdapter.assets[oldItemPosition]
                val new = assets[newItemPosition]
                return old.assetId == new.assetId
            }

            override fun getOldListSize() = this@AssetAdapter.assets.size

            override fun getNewListSize() = assets.size

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                if (headerView != null) {
                    if (oldItemPosition == 0 && newItemPosition == 0) {
                        return true
                    } else if (oldItemPosition == 0 || newItemPosition == 0) {
                        return false
                    }
                }

                val old = this@AssetAdapter.assets[oldItemPosition]
                val new = assets[newItemPosition]
                return old == new
            }
        })
        this@AssetAdapter.assets = assets
        val recyclerViewState = rv.layoutManager.onSaveInstanceState()
        diffResult.dispatchUpdatesTo(this)
        rv.layoutManager.onRestoreInstanceState(recyclerViewState)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == TYPE_HEADER && headerView != null) {
            TYPE_HEADER
        } else {
            TYPE_NORMAL
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AssetHolder) {
            val asset = assets[getPos(position)]
            val ctx = holder.itemView.context
            holder.itemView.balance.text = BigDecimalCache.SINGLETON.get(asset.balance + "_" + FORMAT_TYPE_8) + " " + asset.symbol
            formatAsync(asset.balance, FORMAT_TYPE_8) {
                holder.itemView.balance.text = it + " " + asset.symbol
                BigDecimalCache.SINGLETON.put(asset.balance + "_" + FORMAT_TYPE_8, it)
            }
            holder.itemView.balance_as.text = ctx.getString(R.string.wallet_unit_usd,
                "≈ ${BigDecimalCache.SINGLETON.get(asset.usd().toPlainString() + "_" + FORMAT_TYPE_2)}")
            formatAsync(asset.usd().toPlainString(), FORMAT_TYPE_2) {
                holder.itemView.balance_as.text = ctx.getString(R.string.wallet_unit_usd, "≈ $it")
                BigDecimalCache.SINGLETON.put(asset.usd().toPlainString() + "_" + FORMAT_TYPE_2, it)
            }
            if (asset.priceUsd == "0") {
                holder.itemView.price_tv.text = "N/A"
                holder.itemView.change_tv.visibility = GONE
            } else {
                holder.itemView.change_tv.visibility = VISIBLE
                holder.itemView.price_tv.text = "$${BigDecimalCache.SINGLETON.get(asset.priceUsd + "_" + FORMAT_TYPE_0)}"
                formatAsync(asset.priceUsd, FORMAT_TYPE_0) {
                    holder.itemView.price_tv.text = "$$it"
                    BigDecimalCache.SINGLETON.put(asset.priceUsd + "_" + FORMAT_TYPE_0, it)
                }
                if (asset.changeUsd.isNotEmpty()) {
                    val changeUsd = BigDecimal(asset.changeUsd)
                    val isPositive = changeUsd > BigDecimal.ZERO
                    val s = BigDecimalCache.SINGLETON.get(asset.changeUsd + "_" + FORMAT_TYPE_2)
                    val t = if (isPositive) "+$s%" else "$s%"
                    holder.itemView.change_tv.text = t
                    formatAsync((changeUsd * BigDecimal(100)).toPlainString(), FORMAT_TYPE_2) {
                        holder.itemView.change_tv.text = if (isPositive) "+$it%" else "$it%"
                        BigDecimalCache.SINGLETON.put(asset.changeUsd + "_" + FORMAT_TYPE_2, it)
                    }
                    holder.itemView.change_tv.textColorResource = if (isPositive) R.color.colorGreen else R.color.colorRed
                }
            }
            holder.itemView.avatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
            holder.itemView.avatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
            holder.itemView.setOnClickListener { assetsListener?.onAsset(asset) }
        }
    }

    override fun getItemCount(): Int = if (headerView != null) assets.size + 1 else assets.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            HeadHolder(headerView!!)
        } else {
            AssetHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_wallet_asset, parent, false))
        }
    }

    private fun formatAsync(needFormat: String, formatType: Int, uiOperation: (String) -> Unit) {
        doAsync {
            val formatted = when (formatType) {
                FORMAT_TYPE_2 -> needFormat.numberFormat2()
                FORMAT_TYPE_8 -> needFormat.numberFormat8()
                else -> needFormat.numberFormat()
            }

            uiThread { uiOperation(formatted) }
        }
    }

    private fun getPos(position: Int): Int {
        return if (headerView != null) {
            position - 1
        } else {
            position
        }
    }

    fun setHeader(header: View) {
        headerView = header
    }

    fun setAssetListener(listener: AssetsListener) {
        assetsListener = listener
    }

    class AssetHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    class HeadHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    interface AssetsListener {
        fun onAsset(asset: AssetItem)
    }
}
