package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName
import one.mixin.android.BuildConfig

data class SessionRequest(
    val platform: String = "Android",
    @SerializedName("platform_version")
    val platformVersion: String = android.os.Build.VERSION.RELEASE,
    @SerializedName("app_version")
    val appVersion: String = BuildConfig.VERSION_NAME,
    @SerializedName("notification_token")
    val notificationToken: String? = null,
    @SerializedName("device_check_token")
    val deviceCheckToken: String? = null,
)
