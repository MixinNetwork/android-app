package one.mixin.android.ui.conversation.base

import androidx.recyclerview.widget.RecyclerView

abstract class AsyncAdapter<V : Any, VH : RecyclerView.ViewHolder>(private val dataFetcher: DataFetcher<V>) : RecyclerView.Adapter<VH>() {
    internal val data: CompressedList<V> = dataFetcher.initData()
    override fun getItemCount(): Int {
        return data.size
    }

    fun getItem(position: Int): V? {
        if (position >= 0 && position < data.size) {
            val result = data[position]
            if (result == null){
                // Todo load data
                return null
            }else{
                return result
            }
        } else {
            // Todo load data
            return null
        }
    }

    fun loadAround() {
        // Todo
    }
}
