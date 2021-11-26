package one.mixin.android.ui.sticker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemBannerBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.round

class BannerAdapter : ListAdapter<Banner, BannerHolder>(Banner.DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        BannerHolder(ItemBannerBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: BannerHolder, position: Int) {
        getItem(position)?.let { banner -> holder.bind(banner) }
    }
}

class BannerHolder(val binding: ItemBannerBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(banner: Banner) {
        binding.bannerIv.round(14.dp)
        binding.bannerIv.loadImage(banner.img)
    }
}

data class Banner(val img: String) {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Banner>() {
            override fun areItemsTheSame(oldItem: Banner, newItem: Banner) = oldItem.img == newItem.img

            override fun areContentsTheSame(oldItem: Banner, newItem: Banner) = oldItem == newItem
        }
    }
}
