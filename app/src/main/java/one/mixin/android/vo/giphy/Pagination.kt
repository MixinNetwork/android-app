package one.mixin.android.vo.giphy

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Pagination(
    val total_count: Int,
    val count: Int,
    val offset: Int
)
