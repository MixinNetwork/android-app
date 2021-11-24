package one.mixin.android.vo

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class TestModel<T>(
    @Json(name = "title") val title: String?,
    @Json(name = "list")
    val list: List<String?> = emptyList(),
    @Json(name = "child")
    val child: List<T>? = null
)
