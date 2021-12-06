package one.mixin.android.ui.sticker

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemBannerBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.round

class BannerAdapter : RecyclerView.Adapter<BannerHolder>() {
    var data: List<Banner>? = null
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            if (field == value) return

            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        BannerHolder(ItemBannerBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: BannerHolder, position: Int) {
        data?.get(position)?.let { banner -> holder.bind(banner) }
    }

    override fun getItemCount(): Int = data?.size ?: 0
}

class BannerHolder(val binding: ItemBannerBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(banner: Banner) {
        binding.bannerIv.loadImage(banner.img)
    }
}

data class Banner(val img: String)
