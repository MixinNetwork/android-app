package one.mixin.android.ui.search.holder

import android.annotation.SuppressLint
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemSearchTipBinding
import one.mixin.android.extension.highLight
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.widget.linktext.Utils

class TipHolder(val binding: ItemSearchTipBinding) : RecyclerView.ViewHolder(binding.root) {
    @SuppressLint("SetTextI18n")
    fun bind(target: String, tipType: TipType, searching: Boolean, onItemClickListener: SearchFragment.OnSearchClickListener?) {
        if (tipType == TipType.Phone) {
            binding.searchTipTv.text = "${itemView.context.getString(R.string.search_placeholder_number)}$target"
            if (searching) {
                binding.pb.isVisible = true
                binding.searchTipTv.isEnabled = false
            } else {
                binding.pb.isVisible = false
                binding.searchTipTv.isEnabled = true
            }
            binding.searchTipTv.highLight(target)
            binding.searchTipTv.setOnClickListener {
                onItemClickListener?.onTipClick(tipType)
            }
        } else {
            binding.pb.isVisible = false
            val urlPattern = Utils.getUrlPattern()
            val matcher = urlPattern.matcher(target)
            if (matcher.find()) {
                val url = target.substring(matcher.start(), matcher.end())
                binding.searchTipTv.text = itemView.context.getString(R.string.Open_Link, url)
                binding.searchTipTv.highLight(url)
                binding.searchTipTv.setOnClickListener {
                    onItemClickListener?.onTipClick(tipType, url)
                }
            }
        }
    }
}

class TipItem

enum class TipType {
    Phone, Url
}
