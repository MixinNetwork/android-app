package one.mixin.android.vo

import android.os.Parcelable
import androidx.room.ColumnInfo
import kotlinx.android.parcel.Parcelize

@Parcelize
class ConversationCircleItem(
    @ColumnInfo(name = "circle_id")
    val circleId: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String?,
    @ColumnInfo(name = "count")
    val count: Int,
    @ColumnInfo(name = "unseen_message_count")
    val unseenMessageCount: Int
) : Parcelable

class CircleOrder(
    val circleId: String,
    val orderAt: String
)
