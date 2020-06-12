package one.mixin.android.api

import com.google.gson.JsonElement
import java.io.Serializable

data class ResponseError(
    val status: Int,
    val code: Int,
    val description: String,
    val extra: JsonElement?
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 2L
    }
}
