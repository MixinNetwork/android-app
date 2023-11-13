package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName
import uniqueObjectId
import java.util.UUID

data class GhostKeyRequest(
    @SerializedName("receivers")
    val receivers: List<String>,
    @SerializedName("index")
    val index: Int,
    @SerializedName("hint")
    val hint: String,
)

fun buildGhostKeyRequest(receiverId: String, senderId: String, traceId: String): List<GhostKeyRequest> {
    val output = uniqueObjectId(traceId, "OUTPUT", "0")
    val change = uniqueObjectId(traceId, "OUTPUT", "1")
    return listOf(GhostKeyRequest(listOf(receiverId), 0, output),
        GhostKeyRequest(listOf(senderId), 1, change))
}

fun buildWithdrawalSubmitGhostKeyRequest(receiverId: String, senderId: String, traceId: String): List<GhostKeyRequest> {
    // 0 is withdrawal
    val output = uniqueObjectId(traceId, "OUTPUT", "1")
    val change = uniqueObjectId(traceId, "OUTPUT", "2")
    return listOf(GhostKeyRequest(listOf(receiverId), 1, output),
        GhostKeyRequest(listOf(senderId), 2, change))
}

fun buildWithdrawalFeeGhostKeyRequest(receiverId: String, senderId: String, traceId: String): List<GhostKeyRequest> {
    val change = uniqueObjectId(traceId, "OUTPUT", "1")
    val requestId = uniqueObjectId(traceId, "FEE")
    val feeOutput = uniqueObjectId(requestId, "OUTPUT", "0")
    val feeChange = uniqueObjectId(requestId, "OUTPUT", "1")
    return listOf(GhostKeyRequest(listOf(senderId), 1, change),
        GhostKeyRequest(listOf(receiverId), 0, feeOutput),
        GhostKeyRequest(listOf(senderId), 1, feeChange))
}
