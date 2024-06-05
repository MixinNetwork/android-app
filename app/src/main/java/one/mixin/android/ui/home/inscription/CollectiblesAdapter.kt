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
import one.mixin.android.vo.safe.SafeInscription

class CollectiblesAdapter(val callback: (SafeInscription) -> Unit) : RecyclerView.Adapter<InscriptionHolder>() {
    var list: List<SafeInscription> = emptyList()
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
    init {
        binding.inscriptionIv.roundTopOrBottom(8.dp.toFloat(), top = true, bottom = false)
    }

    @SuppressLint("SetTextI18n")
    fun bind(
        inscriptionItem: SafeInscription,
        callback: (SafeInscription) -> Unit,
    ) {
        binding.apply {
            root.setOnClickListener { callback.invoke(inscriptionItem) }
            inscriptionIv.loadImage(data = inscriptionItem.contentURL, holder = R.drawable.ic_default_inscription)
            title.text = inscriptionItem.name
            subTitle.text = "#${inscriptionItem.sequence}"
        }
    }
}
