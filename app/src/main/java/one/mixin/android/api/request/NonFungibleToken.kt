package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class NonFungibleToken(
    val type: String,
    @SerializedName("token_id")
    val tokenId: String,
    @SerializedName("chain_id")
    val chain_id: String,
    @SerializedName("class")
    val clazz: String,
    @SerializedName("group")
    val groupKey: String,
    @SerializedName("token")
    val tokenKey: String,
    @SerializedName("created_at")
    val createdAt: String
)