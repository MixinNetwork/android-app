package one.mixin.android.api.request

data class RelationshipRequest(val user_id: String, val action: String, val full_name: String? = null)

enum class RelationshipAction { ADD, UPDATE, REMOVE, BLOCK, UNBLOCK }
