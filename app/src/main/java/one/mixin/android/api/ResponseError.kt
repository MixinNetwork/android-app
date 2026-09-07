package one.mixin.android.api

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ResponseError(
    @SerializedName("status")
    val status: Int,
    @SerializedName("code")
    val code: Int,
    @SerializedName("description")
    val description: String,
    @SerializedName("extra")
    val extra: JsonElement? = null,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 2L
    }
}
