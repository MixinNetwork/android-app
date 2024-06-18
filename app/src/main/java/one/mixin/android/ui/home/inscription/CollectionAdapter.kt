package one.mixin.android.ui.home.inscription

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemInscriptionBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.roundTopOrBottom
import one.mixin.android.vo.safe.SafeCollection

class CollectionAdapter(val callback: (SafeCollection) -> Unit) : RecyclerView.Adapter<CollectionHolder>() {
    var list: List<SafeCollection> = emptyList()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): CollectionHolder {
        return CollectionHolder(ItemInscriptionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(
        holder: CollectionHolder,
        position: Int,
    ) {
        holder.bind(list[position], callback)
    }
}

class CollectionHolder(val binding: ItemInscriptionBinding) : RecyclerView.ViewHolder(binding.root) {
    init {
        binding.inscriptionIv.roundTopOrBottom(8.dp.toFloat(), top = true, bottom = false)
    }

    @SuppressLint("SetTextI18n")
    fun bind(
        inscriptionItem: SafeCollection,
        callback: (SafeCollection) -> Unit,
    ) {
        binding.apply {
            root.setOnClickListener { callback.invoke(inscriptionItem) }
            inscriptionIv.loadImage(data = inscriptionItem.iconURL, holder = R.drawable.ic_default_inscription)
            title.text = inscriptionItem.name
            subTitle.text = "${inscriptionItem.inscriptionCount}"
        }
    }
}
