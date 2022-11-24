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

    var bannerListener: BannerListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        BannerHolder(ItemBannerBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: BannerHolder, position: Int) {
        data?.get(position)?.let { banner -> holder.bind(banner, bannerListener) }
    }

    override fun getItemCount(): Int = data?.size ?: 0
}

class BannerHolder(val binding: ItemBannerBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(banner: Banner, bannerListener: BannerListener? = null) {
        binding.apply {
            bannerIv.loadImage(banner.img)
            bannerIv.round(12.dp)
            root.setOnClickListener { bannerListener?.onBannerClick(banner) }
        }
    }
}

interface BannerListener {
    fun onBannerClick(banner: Banner)
}

data class Banner(
    val albumId: String,
    val img: String,
)
