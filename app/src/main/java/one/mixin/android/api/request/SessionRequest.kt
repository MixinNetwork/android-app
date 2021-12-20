package one.mixin.android.api.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import one.mixin.android.BuildConfig

@JsonClass(generateAdapter = true)
data class SessionRequest(
    val platform: String = "Android",
    @Json(name = "platform_version")
    val platformVersion: String = android.os.Build.VERSION.RELEASE,
    @Json(name = "app_version")
    val appVersion: String = BuildConfig.VERSION_NAME,
    @Json(name = "notification_token")
    val notificationToken: String? = null,
    @Json(name = "device_check_token")
    val deviceCheckToken: String? = null
)
