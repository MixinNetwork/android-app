package one.mixin.android.vo

import androidx.recyclerview.widget.DiffUtil
import androidx.room.ColumnInfo

class CallUser(
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "identity_number")
    val identityNumber: String,
    @ColumnInfo(name = "full_name")
    val fullName: String?,
    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String?,
    @ColumnInfo(name = "role")
    val role: String,
    @ColumnInfo(name = "is_verified")
    val isVerified: Boolean?,
    @ColumnInfo(name = "app_id")
    var appId: String?,
    @ColumnInfo(name = "membership")
    val membership: Membership?,
) {
    companion object {
        val DIFF_CALLBACK =
            object : DiffUtil.ItemCallback<CallUser>() {
                override fun areItemsTheSame(
                    oldItem: CallUser,
                    newItem: CallUser,
                ) =
                    oldItem.userId == newItem.userId

                override fun areContentsTheSame(
                    oldItem: CallUser,
                    newItem: CallUser,
                ) =
                    oldItem == newItem
            }
    }

    override fun equals(other: Any?): Boolean =
        other is CallUser && other.userId == userId && other.role == role &&
            other.fullName == fullName && other.avatarUrl == avatarUrl

    override fun hashCode(): Int {
        var result = userId.hashCode()
        result = 31 * result + identityNumber.hashCode()
        result = 31 * result + (fullName?.hashCode() ?: 0)
        result = 31 * result + (avatarUrl?.hashCode() ?: 0)
        result = 31 * result + role.hashCode()
        return result
    }
}
