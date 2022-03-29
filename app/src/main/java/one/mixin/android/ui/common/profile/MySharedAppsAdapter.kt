package one.mixin.android.ui.common.profile

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemSharedAppBinding
import one.mixin.android.databinding.ItemSharedLocalAppBinding
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.ui.common.profile.holder.FooterHolder
import one.mixin.android.ui.common.profile.holder.ItemViewHolder
import one.mixin.android.ui.common.profile.holder.LocalAppHolder
import one.mixin.android.ui.common.profile.holder.SharedAppHolder
import one.mixin.android.vo.App

class MySharedAppsAdapter(
    private val onAddSharedApp: (app: App) -> Unit,
    private val onRemoveSharedApp: (app: App) -> Unit
) : RecyclerView.Adapter<ItemViewHolder>() {
    private var favoriteApps: List<App>? = null
    private var unFavoriteApps: List<App>? = null

    @SuppressLint("NotifyDataSetChanged")
    fun setData(favoriteApps: List<App>, unFavoriteApps: List<App>) {
        this.favoriteApps = favoriteApps
        this.unFavoriteApps = unFavoriteApps
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ItemViewHolder {
        return when (viewType) {
            0 -> {
                SharedAppHolder(ItemSharedAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            1 -> {
                LocalAppHolder(ItemSharedLocalAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            else -> {
                val view =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_shared_footer, parent, false)
                FooterHolder(view)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(
        holder: ItemViewHolder,
        position: Int
    ) {
        if (getItemViewType(position) == 0) {
            holder.bind(getItem(position), onRemoveSharedApp)
        } else if (getItemViewType(position) == 1) {
            holder.itemView.tag = position == favoriteApps.notNullWithElse({ it.size }, 0)
            holder.bind(getItem(position), onAddSharedApp)
        }
    }

    override fun getItemCount(): Int {
        return (
            favoriteApps.notNullWithElse({ it.size }, 0) +
                unFavoriteApps.notNullWithElse({ it.size }, 0)
            ).run {
            if (this > 0) {
                this + 1
            } else {
                this
            }
        }
    }

    fun getItem(position: Int): App {
        val type = getItemViewType(position)
        return if (type == 0) {
            favoriteApps!![position]
        } else {
            val favoriteSize = favoriteApps.notNullWithElse({ it.size }, 0)
            unFavoriteApps!![position - favoriteSize]
        }
    }

    override fun getItemViewType(position: Int): Int {
        val favoriteSize = favoriteApps.notNullWithElse({ it.size }, 0)
        return if (position < favoriteSize) {
            0
        } else if (position < favoriteSize + unFavoriteApps.notNullWithElse({ it.size }, 0)) {
            1
        } else {
            2
        }
    }
}
