package one.mixin.android.ui.search.holder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemSearchHeaderBinding

class HeaderHolder constructor(val binding: ItemSearchHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(text: String, showMore: Boolean) {
        binding.searchHeaderTv.text = text
        binding.searchHeaderMore.visibility = if (showMore) View.VISIBLE else View.GONE
    }
}
