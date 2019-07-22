package one.mixin.android.vo

import androidx.recyclerview.widget.DiffUtil

data class SearchMessageDetailItem(
    val messageId: String,
    val type: String,
    val content: String?,
    val createdAt: String,
    val mediaName: String?,
    val userId: String,
    val userFullName: String?,
    val userAvatarUrl: String?
) {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SearchMessageDetailItem>() {
            override fun areItemsTheSame(oldItem: SearchMessageDetailItem, newItem: SearchMessageDetailItem) =
                oldItem.messageId == newItem.messageId

            override fun areContentsTheSame(oldItem: SearchMessageDetailItem, newItem: SearchMessageDetailItem) =
                oldItem == newItem
        }
    }
}
