package one.mixin.android.ui.home.inscription

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemInscriptionBinding
import one.mixin.android.databinding.ItemInscriptionHeaderBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.vo.safe.SafeCollectible
import one.mixin.android.vo.safe.SafeCollection
import one.mixin.android.widget.CoilRoundedHexagonTransformation

class CollectiblesHeaderAdapter(val collection: SafeCollection, val callback: (SafeCollectible) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var list: List<SafeCollectible> = emptyList()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        return if (viewType == 1)
            InscriptionHolder(ItemInscriptionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        else
            CollectiblesHeaderHolder(ItemInscriptionHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int = list.size + 1

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        if (getItemViewType(position) == 1) {
            (holder as InscriptionHolder).bind(list[position - 1], callback)
        } else {
            (holder as CollectiblesHeaderHolder).bind(collection)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            0
        } else {
            1
        }
    }
}

class CollectiblesHeaderHolder(val binding: ItemInscriptionHeaderBinding) : RecyclerView.ViewHolder(binding.root) {

    @SuppressLint("SetTextI18n")
    fun bind(
        collection: SafeCollection
    ) {
        binding.apply {
            inscriptionIv.loadImage(collection.iconURL, R.drawable.ic_avatar_place_holder, transformation = CoilRoundedHexagonTransformation())
            nameTv.text = collection.name
            countTv.text = root.context.getString(R.string.Collection_collected, collection.inscriptionCount)
            descriptionTv.text = collection.description
        }
    }
}