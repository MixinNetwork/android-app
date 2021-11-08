package one.mixin.android.vo.giphy

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SearchData(
    val data: List<Gif>,
    val meta: Meta,
    val pagination: Pagination
)
