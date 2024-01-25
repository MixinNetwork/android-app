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
import one.mixin.android.vo.ExploreApp

class MySharedAppsAdapter(
    private val onAddSharedApp: (app: ExploreApp) -> Unit,
    private val onRemoveSharedApp: (app: ExploreApp) -> Unit,
) : RecyclerView.Adapter<ItemViewHolder>() {
    private var favoriteApps: List<ExploreApp>? = null
    private var unFavoriteApps: List<ExploreApp>? = null
    private var target: String? = null

    @SuppressLint("NotifyDataSetChanged")
    fun setData(
        favoriteApps: List<ExploreApp>,
        unFavoriteApps: List<ExploreApp>,
        target: String? = null
    ) {
        this.favoriteApps = favoriteApps
        this.unFavoriteApps = unFavoriteApps
        this.target = target
        notifyDataSetChanged()
    }

    fun isEmpty() = favoriteApps.isNullOrEmpty() && unFavoriteApps.isNullOrEmpty()
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
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
        position: Int,
    ) {
        if (getItemViewType(position) == 0) {
            holder.bind(getItem(position), target, onRemoveSharedApp)
        } else if (getItemViewType(position) == 1) {
            holder.itemView.tag = position == favoriteApps.notNullWithElse({ it.size }, 0)
            holder.bind(getItem(position), target, onAddSharedApp)
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

    fun getItem(position: Int): ExploreApp {
        val type = getItemViewType(position)
        return if (type == 0) {
            favoriteApps!![position]
        } else {
            val favoriteSize = favoriteApps.notNullWithElse({ it.size }, 0)
            unFavoriteApps!![position - favoriteSize - 1]
        }
    }

    override fun getItemViewType(position: Int): Int {
        val favoriteSize = favoriteApps.notNullWithElse({ it.size }, 0)
        return if (position < favoriteSize) {
            0
        } else if (position == favoriteSize) {
            2
        } else {
            1
        }
    }
}
