package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Ignore

class ConversationStorageUsage(
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String?,
    @ColumnInfo(name = "icon_url")
    val groupIconUrl: String?,
    @ColumnInfo(name = "category")
    val category: String?,
    @ColumnInfo(name = "name")
    val groupName: String?,
    @ColumnInfo(name = "full_name")
    val name: String?,
    @ColumnInfo(name = "owner_id")
    val ownerId: String,
    @ColumnInfo(name = "identity_number")
    val ownerIdentityNumber: String,
    @ColumnInfo(name = "is_verified")
    val ownerIsVerified: Boolean
) {
    @Ignore
    var mediaSize: Long = 0L
}
