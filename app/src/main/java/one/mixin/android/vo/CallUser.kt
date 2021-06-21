package one.mixin.android.vo

import androidx.recyclerview.widget.DiffUtil
import androidx.room.ColumnInfo

data class CallUser(
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "identity_number")
    val identityNumber: String,
    @ColumnInfo(name = "full_name")
    val fullName: String?,
    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String?,
    @ColumnInfo(name = "role")
    val role: String
) {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CallUser>() {
            override fun areItemsTheSame(oldItem: CallUser, newItem: CallUser) =
                oldItem.userId == newItem.userId

            override fun areContentsTheSame(oldItem: CallUser, newItem: CallUser) =
                oldItem == newItem
        }
    }
}
