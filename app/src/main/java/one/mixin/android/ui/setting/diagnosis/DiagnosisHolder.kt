package one.mixin.android.ui.setting.diagnosis

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemDiagnosisBinding

class DiagnosisHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val binding = ItemDiagnosisBinding.bind(itemView)
    fun bind(resultBean: ResultBean) {
        binding.itemDiagnosisTv.text = resultBean.title
        binding.itemDiagnosisParam.text = resultBean.formatString()
    }
}
