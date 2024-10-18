package one.mixin.android.ui.search.holder

import android.annotation.SuppressLint
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemSearchTipBinding
import one.mixin.android.extension.highLight
import one.mixin.android.ui.search.SearchFragment

class UrlHolder(val binding: ItemSearchTipBinding) : RecyclerView.ViewHolder(binding.root) {
    @SuppressLint("SetTextI18n")
    fun bind(
        url: String,
        onItemClickListener: SearchFragment.OnSearchClickListener?,
    ) {
        binding.searchTipTv.text =
            itemView.context.getString(R.string.Open_Link, url)
        binding.searchTipTv.highLight(url)
        binding.pb.isVisible = false
        binding.searchTipTv.isEnabled = true
        binding.searchTipTv.setOnClickListener {
            onItemClickListener?.onUrlClick(url)
        }
    }
}

