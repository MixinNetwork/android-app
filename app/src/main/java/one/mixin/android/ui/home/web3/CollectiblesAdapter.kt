package one.mixin.android.ui.home.web3

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemInscriptionBinding
import one.mixin.android.vo.InscriptionItem

class CollectiblesAdapter : RecyclerView.Adapter<InscriptionHolder>() {

    var list: List<InscriptionItem> = emptyList()

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
        holder.bind(list[position])
    }
}

class InscriptionHolder(val binding: ItemInscriptionBinding) : RecyclerView.ViewHolder(binding.root) {
    @SuppressLint("SetTextI18n")
    fun bind(inscriptionItem: InscriptionItem) {
    }
}
