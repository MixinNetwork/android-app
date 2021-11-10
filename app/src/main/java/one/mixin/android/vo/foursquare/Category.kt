package one.mixin.android.vo.foursquare

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class Category(
    val name: String,
    val icon: Icon
)
