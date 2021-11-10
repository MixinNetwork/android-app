package one.mixin.android.vo.foursquare

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class Icon(val prefix: String, val suffix: String)
