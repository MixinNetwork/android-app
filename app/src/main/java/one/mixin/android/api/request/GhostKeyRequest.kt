package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName
import java.util.UUID

class GhostKeyRequest(
    @SerializedName("receivers")
    val receivers: List<String>,
    @SerializedName("index")
    val index: Int,
    @SerializedName("hint")
    val hint: String,
)

fun buildGhostKeyRequest(receiverId: String, senderId: String, traceId:String): List<GhostKeyRequest> {
    return listOf(GhostKeyRequest(listOf(receiverId), 0, traceId), GhostKeyRequest(listOf(senderId), 1, UUID.randomUUID().toString()))
}