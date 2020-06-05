package one.mixin.android.vo

import android.graphics.Color
import android.os.Parcelable
import androidx.room.ColumnInfo
import kotlinx.android.parcel.Parcelize
import one.mixin.android.MixinApplication
import one.mixin.android.R
import java.util.UUID
import kotlin.math.abs

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

fun getCircleColor(circleId: String?): Int {
    return if (circleId == null) {
        Color.BLACK
    } else {
        val hashcode = try {
            UUID.fromString(circleId).hashCode()
        } catch (e: IllegalArgumentException) {
            circleId.hashCode()
        }
        colors[abs(hashcode).rem(colors.size)]
    }
}

private val colors: IntArray = MixinApplication.appContext.resources.getIntArray(R.array.circle_colors)
