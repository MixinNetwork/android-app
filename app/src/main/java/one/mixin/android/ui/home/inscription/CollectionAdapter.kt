package one.mixin.android.ui.home.inscription

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemInscriptionBinding
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


    @SuppressLint("SetTextI18n")
    fun bind(
        inscriptionCollection: SafeCollection,
        callback: (SafeCollection) -> Unit,
    ) {
        binding.apply {
            root.setOnClickListener { callback.invoke(inscriptionCollection) }
            inscription.render(inscriptionCollection)
            title.text = inscriptionCollection.name
            subTitle.text = "${inscriptionCollection.inscriptionCount}"
        }
    }
}
