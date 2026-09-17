package one.mixin.android.ui.common.recyclerview

import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

abstract class SafePagingDataAdapter<T : Any, VH : RecyclerView.ViewHolder>(
    diffCallback: DiffUtil.ItemCallback<T>,
) : PagingDataAdapter<T, VH>(diffCallback)
