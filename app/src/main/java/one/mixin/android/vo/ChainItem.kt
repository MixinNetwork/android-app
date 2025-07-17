package one.mixin.android.vo

import android.os.Parcelable
import androidx.recyclerview.widget.DiffUtil
import androidx.room.ColumnInfo
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChainItem(
    @ColumnInfo(name = "chain_id")
    val chainId: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "destination")
    val destination: String,
    @ColumnInfo(name = "icon_url")
    val iconUrl: String?,
) : Parcelable {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ChainItem>() {
            override fun areItemsTheSame(oldItem: ChainItem, newItem: ChainItem) = oldItem.chainId == newItem.chainId
            override fun areContentsTheSame(oldItem: ChainItem, newItem: ChainItem) = oldItem == newItem
        }
    }
}