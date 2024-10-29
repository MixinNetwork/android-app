package one.mixin.android.api.request

import android.os.Build
import com.google.gson.annotations.SerializedName
import one.mixin.android.BuildConfig

data class AccountRequest(
    val code: String? = null,
    val notification_token: String? = null,
    val registration_id: Int? = null,
    val platform: String = "Android",
    val platform_version: String = Build.VERSION.RELEASE,
    val app_version: String = BuildConfig.VERSION_NAME,
    val package_name: String = BuildConfig.APPLICATION_ID,
    var purpose: String = VerificationPurpose.SESSION.name,
    val pin: String? = null,
    val session_secret: String? = null,
    val public_key_hex: String? = null,
    val message_hex: String? = null,
    val signature_hex: String? = null,
)
