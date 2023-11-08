package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class GhostKeyRequest(
    @SerializedName("receivers")
    val receivers: List<String>,
    @SerializedName("index")
    val index: Int,
    @SerializedName("hint")
    val hint: String,
)

fun buildGhostKeyRequest(receiverId: String, senderId: String, traceId:String): List<GhostKeyRequest> {
    return listOf(GhostKeyRequest(listOf(receiverId), TARGET_INDEX, traceId), GhostKeyRequest(listOf(senderId), CHANGE_INDEX, UUID.randomUUID().toString()))
}

fun buildGhostKeyRequest(receiverId: String, senderId: String, targetId: String, traceId: String): List<GhostKeyRequest> {
    return listOf(GhostKeyRequest(listOf(receiverId), TARGET_INDEX, traceId), GhostKeyRequest(listOf(targetId), FEE_CHANGE_INDEX, UUID.randomUUID().toString()), GhostKeyRequest(listOf(senderId), CHANGE_INDEX, UUID.randomUUID().toString()))
}

private const val TARGET_INDEX = 0
private const val FEE_CHANGE_INDEX = 1
private const val CHANGE_INDEX = 2