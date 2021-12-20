package one.mixin.android.vo.giphy

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Meta(
    val status: Int,
    val msg: String,
    val response_id: String
)
