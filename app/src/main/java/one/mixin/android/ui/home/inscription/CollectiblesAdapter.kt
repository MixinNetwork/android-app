package one.mixin.android.ui.home.inscription

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemInscriptionBinding
import one.mixin.android.vo.safe.SafeCollectible

class CollectiblesAdapter(val callback: (SafeCollectible) -> Unit) : RecyclerView.Adapter<InscriptionHolder>() {
    var list: List<SafeCollectible> = emptyList()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): InscriptionHolder {
        return InscriptionHolder(ItemInscriptionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(
        holder: InscriptionHolder,
        position: Int,
    ) {
        holder.bind(list[position], callback)
    }
}

class InscriptionHolder(val binding: ItemInscriptionBinding) : RecyclerView.ViewHolder(binding.root) {

    @SuppressLint("SetTextI18n")
    fun bind(
        inscriptionItem: SafeCollectible,
        callback: (SafeCollectible) -> Unit,
    ) {
        binding.apply {
            root.setOnClickListener { callback.invoke(inscriptionItem) }
            inscription.render(inscriptionItem)
            title.text = inscriptionItem.name
            subTitle.text = "#${inscriptionItem.sequence}"
        }
    }
}
