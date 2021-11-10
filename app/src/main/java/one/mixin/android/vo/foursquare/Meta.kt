/**
 * Copyright 2020 bejson.com
 */
package one.mixin.android.vo.foursquare

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class Meta(
    val code: Int,
    val requestId: String?
)
