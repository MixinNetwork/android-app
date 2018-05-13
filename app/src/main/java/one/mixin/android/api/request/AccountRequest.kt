package one.mixin.android.api.request

import android.os.Build
import one.mixin.android.BuildConfig

data class AccountRequest(
    val code: String?,
    val invitation: String? = null,
    val notification_token: String? = null,
    val registration_id: Int? = null,
    val platform: String = "Android",
    val platform_version: String = Build.VERSION.RELEASE,
    val app_version: String = BuildConfig.VERSION_NAME,
    var purpose: String = VerificationPurpose.SESSION.name,
    val pin: String? = null,
    val session_secret: String? = null
)