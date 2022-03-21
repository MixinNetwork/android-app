package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import one.mixin.android.extension.nowInUtc
import one.mixin.android.job.BaseJob.Companion.PRIORITY_ACK_MESSAGE
import one.mixin.android.util.GsonHelper
import one.mixin.android.websocket.BlazeAckMessage
import java.util.UUID

@Entity(
    tableName = "jobs",
    indices = [
        Index(value = arrayOf("action")),
    ]
)
data class Job(
    @PrimaryKey
    @SerializedName("job_id")
    @ColumnInfo(name = "job_id")
    var jobId: String,
    @SerializedName("action")
    @ColumnInfo(name = "action")
    var action: String,
    @SerializedName("created_at")
    @ColumnInfo(name = "created_at")
    var created_at: String,
    @SerializedName("order_id")
    @ColumnInfo(name = "order_id")
    var orderId: Int?,
    @SerializedName("priority")
    @ColumnInfo(name = "priority")
    var priority: Int,
    @SerializedName("user_id")
    @ColumnInfo(name = "user_id")
    var userId: String?,
    @SerializedName("blaze_message")
    @ColumnInfo(name = "blaze_message")
    var blazeMessage: String?,
    @SerializedName("conversation_id")
    @ColumnInfo(name = "conversation_id")
    var conversationId: String?,
    @SerializedName("resend_message_id")
    @ColumnInfo(name = "resend_message_id")
    var resendMessageId: String?,
    @SerializedName("run_count")
    @ColumnInfo(name = "run_count")
    var runCount: Int = 0
)

fun createAckJob(action: String, ackMessage: BlazeAckMessage, conversationId: String? = null) =
    Job(
        UUID.nameUUIDFromBytes("${ackMessage.messageId}${ackMessage.status}$action".toByteArray()).toString(),
        action,
        nowInUtc(),
        null,
        PRIORITY_ACK_MESSAGE,
        null,
        GsonHelper.customGson.toJson(ackMessage),
        conversationId,
        null,
        0
    )
