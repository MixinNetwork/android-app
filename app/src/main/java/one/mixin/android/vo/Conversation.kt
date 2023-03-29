package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import one.mixin.android.extension.nowInUtc
import java.util.UUID

@Entity(
    tableName = "conversations",
    indices = [
        Index(value = arrayOf("pin_time", "last_message_created_at")),
    ],
)
open class Conversation(
    @PrimaryKey
    @ColumnInfo(name = "conversation_id")
    @SerializedName("conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "owner_id")
    @SerializedName("owner_id")
    val ownerId: String?,
    @ColumnInfo(name = "category")
    @SerializedName("category")
    val category: String?,
    @ColumnInfo(name = "name")
    @SerializedName("name")
    val name: String?,
    @ColumnInfo(name = "icon_url")
    @SerializedName("icon_url")
    val iconUrl: String?,
    @ColumnInfo(name = "announcement")
    @SerializedName("announcement")
    val announcement: String?,
    @ColumnInfo(name = "code_url")
    @SerializedName("code_url")
    val codeUrl: String?,
    @ColumnInfo(name = "pay_type")
    @SerializedName("pay_type")
    val payType: String?,
    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: String,
    @ColumnInfo(name = "pin_time")
    @SerializedName("pin_time")
    val pinTime: String?,
    @ColumnInfo(name = "last_message_id")
    @SerializedName("last_message_id")
    val lastMessageId: String?,
    @ColumnInfo(name = "last_read_message_id")
    @SerializedName("last_read_message_id")
    val lastReadMessageId: String?,
    @ColumnInfo(name = "unseen_message_count")
    @SerializedName("unseen_message_count")
    val unseenMessageCount: Int?,
    @ColumnInfo(name = "status")
    @SerializedName("status")
    val status: Int,
    @ColumnInfo(name = "draft")
    @SerializedName("draft")
    val draft: String? = null,
    @ColumnInfo(name = "mute_until")
    @SerializedName("mute_until")
    val muteUntil: String? = null,
    @ColumnInfo(name = "last_message_created_at")
    @SerializedName("last_message_created_at")
    val lastMessageCreatedAt: String? = null,
    @ColumnInfo(name = "expire_in")
    @SerializedName("expire_in")
    val expireIn: Long? = null,
) : IConversationCategory {
    override val conversationCategory: String
        get() = category ?: ConversationCategory.CONTACT.name
}

enum class ConversationCategory { CONTACT, GROUP }
enum class ConversationStatus { START, FAILURE, SUCCESS, QUIT }

fun createConversation(conversationId: String, category: String?, recipientId: String, status: Int) =
    ConversationBuilder(conversationId, nowInUtc(), status)
        .setCategory(category)
        .setOwnerId(recipientId)
        .setAnnouncement("")
        .setCodeUrl("")
        .setPayType("")
        .setUnseenMessageCount(0)
        .build()

fun generateConversationId(senderId: String, recipientId: String): String {
    val mix = minOf(senderId, recipientId) + maxOf(senderId, recipientId)
    return UUID.nameUUIDFromBytes(mix.toByteArray()).toString()
}
