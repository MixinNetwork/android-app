package one.mixin.android.api.response

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class DeviceCheckResponse(val nonce: String)
