package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class RelationshipRequest(
    @SerializedName("user_id")
    val user_id: String,
    @SerializedName("action")
    val action: String,
    @SerializedName("full_name")
    val full_name: String? = null,
)

enum class RelationshipAction { ADD, UPDATE, REMOVE, BLOCK, UNBLOCK }
