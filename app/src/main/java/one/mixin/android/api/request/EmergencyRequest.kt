package one.mixin.android.api.request

import android.os.Build
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import one.mixin.android.BuildConfig

@JsonClass(generateAdapter = true)
data class EmergencyRequest(
    val phone: String? = null,
    @Json(name ="identity_number")
    val identityNumber: String? = null,
    val pin: String? = null,
    val code: String? = null,
    val purpose: String,

    val platform: String = "Android",
    @Json(name ="platform_version")
    val platformVersion: String = Build.VERSION.RELEASE,
    @Json(name ="package_name")
    val packageName: String = BuildConfig.APPLICATION_ID,
    @Json(name ="app_version")
    val appVersion: String = BuildConfig.VERSION_NAME,
    @Json(name ="notification_token")
    val notificationToken: String? = null,
    @Json(name ="session_secret")
    val sessionSecret: String? = null,
    @Json(name ="registration_id")
    val registrationId: Int? = null
)

enum class EmergencyPurpose {
    SESSION,
    CONTACT
}
