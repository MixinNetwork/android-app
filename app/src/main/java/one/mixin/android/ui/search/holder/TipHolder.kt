package one.mixin.android.ui.search.holder

import android.annotation.SuppressLint
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemSearchTipBinding
import one.mixin.android.extension.highLight
import one.mixin.android.extension.isMao
import one.mixin.android.ui.search.SearchFragment

class TipHolder(val binding: ItemSearchTipBinding) : RecyclerView.ViewHolder(binding.root) {
    @SuppressLint("SetTextI18n")
    fun bind(
        target: String,
        searching: Boolean,
        url: String?,
        onItemClickListener: SearchFragment.OnSearchClickListener?,
    ) {
        if (url != null) {
            binding.searchTipTv.text =
                itemView.context.getString(R.string.Open_Link, url)
            binding.searchTipTv.highLight(url)
            binding.pb.isVisible = false
            binding.searchTipTv.isEnabled = true
            binding.searchTipTv.setOnClickListener {
                onItemClickListener?.onUrlClick(url)
            }
        } else {
            binding.searchTipTv.text = if (target.isMao()) {
                "${itemView.context.getString(R.string.search_mao)}$target"
            } else {
                "${itemView.context.getString(R.string.search_placeholder_number)}$target"
            }
            binding.searchTipTv.highLight(target)
            if (searching) {
                binding.pb.isVisible = true
                binding.searchTipTv.isEnabled = false
            } else {
                binding.pb.isVisible = false
                binding.searchTipTv.isEnabled = true
            }
            binding.searchTipTv.setOnClickListener {
                onItemClickListener?.onTipClick()
            }
        }
    }
}

class TipItem
