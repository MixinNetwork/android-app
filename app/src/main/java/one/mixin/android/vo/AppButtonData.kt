package one.mixin.android.vo

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AppButtonData(
    val label: String,
    val color: String,
    val action: String
)
