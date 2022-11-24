package one.mixin.android.vo

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.recyclerview.widget.DiffUtil
import kotlinx.parcelize.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
data class GroupMinimal(
    val conversationId: String,
    val groupIconUrl: String?,
    val groupName: String?,
    val memberCount: Int,
) : Parcelable {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<GroupMinimal>() {
            override fun areItemsTheSame(oldItem: GroupMinimal, newItem: GroupMinimal): Boolean =
                oldItem.conversationId == newItem.conversationId

            override fun areContentsTheSame(oldItem: GroupMinimal, newItem: GroupMinimal): Boolean =
                oldItem == newItem
        }
    }
}
