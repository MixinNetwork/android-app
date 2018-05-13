package one.mixin.android.api

import java.io.Serializable

data class ResponseError(
    val status: Int,
    val code: Int,
    val description: String
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 2L
    }
}