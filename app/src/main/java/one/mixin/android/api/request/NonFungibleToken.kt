package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName
import one.mixin.android.api.response.Metadata

data class NonFungibleToken(
    val type: String,
    @SerializedName("token_id")
    val tokenId: String,
    @SerializedName("group")
    val groupKey: String,
    @SerializedName("token")
    val tokenKey: String,
    @SerializedName("meta")
    val metadata: Metadata,
    @SerializedName("created_at")
    val createdAt: String
)
