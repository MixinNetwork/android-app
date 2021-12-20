package one.mixin.android.api.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PinToken {
    @Json(name = "pin_token")
    var pinToken: String? = null
}
