package one.mixin.android.ui.setting.diagnosis

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.util.GsonHelper

class ResultAdapter : RecyclerView.Adapter<DiagnosisHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiagnosisHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diagnosis, parent, false)
        return DiagnosisHolder(itemView)
    }

    override fun onBindViewHolder(holder: DiagnosisHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int = list.size

    private var list = mutableListOf<ResultBean>()
    fun insertData(item: ResultBean) {
        list.add(item)
        notifyItemInserted(list.size)
    }

    fun getResult(): String? {
        return GsonHelper.customGson.toJson(list)
    }
}
