package one.mixin.android.ui.wallet.network

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemSelectableTokenBinding
import one.mixin.android.databinding.ItemSelectedTokenBinding
import one.mixin.android.extension.loadImage

internal class SelectableNetworkAdapter(
    private val selectedChainIds: Set<String>,
    private val onClick: (NetworkOption) -> Unit,
) : RecyclerView.Adapter<SelectableNetworkAdapter.ViewHolder>() {
    var networks: List<NetworkOption> = emptyList()
    var iconUrls: Map<String, String> = emptyMap()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSelectableTokenBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.apply {
            balance.isVisible = false
            networkTv.isVisible = false
            avatar.badge.isVisible = false
            cb.isClickable = false
            cb.isFocusable = false
            name.ellipsize = TextUtils.TruncateAt.END
            name.updateLayoutParams<RelativeLayout.LayoutParams> {
                removeRule(RelativeLayout.ABOVE)
                addRule(RelativeLayout.CENTER_VERTICAL)
                width = ViewGroup.LayoutParams.MATCH_PARENT
                bottomMargin = 0
                marginEnd = root.resources.getDimensionPixelSize(R.dimen.margin16)
            }
        }
        return ViewHolder(binding)
    }

    override fun getItemCount() = networks.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val network = networks[position]
        holder.binding.apply {
            name.text = network.name
            avatar.bg.loadImage(iconUrls[network.chainId], R.drawable.ic_avatar_place_holder)
            cb.isChecked = network.chainId in selectedChainIds
            root.setOnClickListener { onClick(network) }
        }
    }

    class ViewHolder(val binding: ItemSelectableTokenBinding) : RecyclerView.ViewHolder(binding.root)
}

internal class SelectedNetworkAdapter(
    private val onRemove: (NetworkOption) -> Unit,
) : RecyclerView.Adapter<SelectedNetworkAdapter.ViewHolder>() {
    var networks: List<NetworkOption> = emptyList()
    var iconUrls: Map<String, String> = emptyMap()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSelectedTokenBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.removeIv.updateLayoutParams<ConstraintLayout.LayoutParams> {
            bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            topToTop = binding.avatarView.id
        }
        binding.nameTv.ellipsize = TextUtils.TruncateAt.END
        return ViewHolder(binding)
    }

    override fun getItemCount() = networks.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val network = networks[position]
        holder.binding.apply {
            avatarView.loadImage(iconUrls[network.chainId], R.drawable.ic_avatar_place_holder)
            nameTv.text = network.name
            root.setOnClickListener { onRemove(network) }
        }
    }

    class ViewHolder(val binding: ItemSelectedTokenBinding) : RecyclerView.ViewHolder(binding.root)
}
