package one.mixin.android.ui.search.holder

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemSearchTipBinding
import one.mixin.android.extension.highLight
import one.mixin.android.ui.search.SearchFragment

class TipHolder(val binding: ItemSearchTipBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(target: String, searching: Boolean, onItemClickListener: SearchFragment.OnSearchClickListener?) {
        binding.searchTipTv.text = itemView.context.getString(R.string.search_tip, target)
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

class TipItem
