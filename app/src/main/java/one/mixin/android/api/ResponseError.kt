package one.mixin.android.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = true)
data class ResponseError(
    @Json(name = "data")
    val status: Int,
    @Json(name = "code")
    val code: Int,
    @Json(name = "description")
    val description: String,
    @Json(name = "extra")
    val extra: Map<String, String>? = null
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 2L
    }
}
