package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

class ParticipantRequest(
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("role")
    val role: String,
    @SerializedName("created_at")
    val createdAt: String? = null,
)

enum class ParticipantAction { ADD, REMOVE, JOIN, EXIT, ROLE }
