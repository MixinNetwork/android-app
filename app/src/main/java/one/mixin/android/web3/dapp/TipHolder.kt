package one.mixin.android.web3.dapp

import android.annotation.SuppressLint
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemSearchTipBinding
import one.mixin.android.extension.highLight

class TipHolder(val binding: ItemSearchTipBinding) : RecyclerView.ViewHolder(binding.root) {
    @SuppressLint("SetTextI18n")
    fun bind(
        url: String?,
        onUrlClick: (String) -> Unit,
    ) {
        binding.searchTipTv.text =
            itemView.context.getString(R.string.Open_Link, url)
        binding.searchTipTv.highLight(url)
        binding.pb.isVisible = false
        binding.searchTipTv.isEnabled = true
        binding.searchTipTv.setOnClickListener {
            url?.let {
                onUrlClick.invoke(it)
            }
        }
    }
}
