package one.mixin.android.api.request

import android.os.Build
import com.google.gson.annotations.SerializedName
import one.mixin.android.BuildConfig

data class EmergencyRequest(
    val phone: String? = null,
    @SerializedName("identity_number")
    val identityNumber: String? = null,
    val pin: String? = null,
    val code: String? = null,
    val purpose: String,
    val platform: String = "Android",
    @SerializedName("platform_version")
    val platformVersion: String = Build.VERSION.RELEASE,
    @SerializedName("package_name")
    val packageName: String = BuildConfig.APPLICATION_ID,
    @SerializedName("app_version")
    val appVersion: String = BuildConfig.VERSION_NAME,
    @SerializedName("notification_token")
    val notificationToken: String? = null,
    @SerializedName("session_secret")
    val sessionSecret: String? = null,
    @SerializedName("registration_id")
    val registrationId: Int? = null,
)

enum class EmergencyPurpose {
    SESSION,
    CONTACT,
}
