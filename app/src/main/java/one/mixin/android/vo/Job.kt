package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import one.mixin.android.extension.nowInUtc
import one.mixin.android.job.BaseJob.Companion.PRIORITY_ACK_MESSAGE
import one.mixin.android.moshi.MoshiHelper.getTypeAdapter
import one.mixin.android.websocket.BlazeAckMessage
import java.util.UUID

@Entity(
    tableName = "jobs",
    indices = [
        Index(value = arrayOf("action")),
    ]
)
@JsonClass(generateAdapter = true)
data class Job(
    @PrimaryKey
    @Json(name = "job_id")
    @ColumnInfo(name = "job_id")
    var jobId: String,
    @Json(name = "action")
    @ColumnInfo(name = "action")
    var action: String,
    @Json(name = "created_at")
    @ColumnInfo(name = "created_at")
    var created_at: String,
    @Json(name = "order_id")
    @ColumnInfo(name = "order_id")
    var orderId: Int?,
    @Json(name = "priority")
    @ColumnInfo(name = "priority")
    var priority: Int,
    @Json(name = "user_id")
    @ColumnInfo(name = "user_id")
    var userId: String?,
    @Json(name = "blaze_message")
    @ColumnInfo(name = "blaze_message")
    var blazeMessage: String?,
    @Json(name = "conversation_id")
    @ColumnInfo(name = "conversation_id")
    var conversationId: String?,
    @Json(name = "resend_message_id")
    @ColumnInfo(name = "resend_message_id")
    var resendMessageId: String?,
    @Json(name = "run_count")
    @ColumnInfo(name = "run_count")
    var runCount: Int = 0
)

fun createAckJob(action: String, ackMessage: BlazeAckMessage, conversationId: String? = null) =
    Job(
        UUID.nameUUIDFromBytes("${ackMessage.message_id}${ackMessage.status}$action".toByteArray()).toString(),
        action,
        nowInUtc(),
        null,
        PRIORITY_ACK_MESSAGE,
        null,
        getTypeAdapter<BlazeAckMessage>(BlazeAckMessage::class.java).toJson(ackMessage),
        conversationId,
        null,
        0
    )
