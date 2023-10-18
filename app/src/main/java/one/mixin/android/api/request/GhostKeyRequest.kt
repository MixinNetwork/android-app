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

fun buildGhostKeyRequest(userId:String, selfId:String): List<GhostKeyRequest> {
    return listOf(GhostKeyRequest(listOf(userId), 0, UUID.randomUUID().toString()), GhostKeyRequest(listOf(selfId), 1, UUID.randomUUID().toString()))
}