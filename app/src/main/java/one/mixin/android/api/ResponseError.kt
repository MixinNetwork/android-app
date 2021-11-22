package one.mixin.android.api

import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = true)
data class ResponseError(
    val status: Int,
    val code: Int,
    val description: String,
    val extra: Map<String, String>? = null
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 2L
    }
}
