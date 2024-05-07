package one.mixin.android.ui.home.web3

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.Shimmer
import one.mixin.android.R
import one.mixin.android.databinding.ItemInscriptionBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.round
import one.mixin.android.vo.InscriptionItem

class CollectiblesAdapter(val callback: (InscriptionItem) -> Unit) : RecyclerView.Adapter<InscriptionHolder>() {

    var list: List<InscriptionItem> = emptyList()
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
        binding.root.round(20)
        // binding.inscription.setShimmer(
        //     Shimmer.ColorHighlightBuilder()
        //         .setHighlightColor(Color.TRANSPARENT)
        //         .build()
        // )
    }
    @SuppressLint("SetTextI18n")
    fun bind(inscriptionItem: InscriptionItem, callback: (InscriptionItem) -> Unit) {
        binding.apply {
            root.setOnClickListener { callback.invoke(inscriptionItem) }
            inscriptionIv.loadImage(uri = inscriptionItem.contentURL, holder = R.drawable.ic_default_inscription)
            title.text = inscriptionItem.inscriptionHash
            subTitle.text = "#${inscriptionItem.sequence}"
        }
    }
}
