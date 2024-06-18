package one.mixin.android.api.request.web3

import com.google.gson.annotations.SerializedName

data class PriorityFeeRequest(
    val transaction: String,
    @SerializedName("priority_level")
    val priorityLevel: PriorityLevel = PriorityLevel.Medium,
)

enum class PriorityLevel {
    Keep, High, Medium,
}
