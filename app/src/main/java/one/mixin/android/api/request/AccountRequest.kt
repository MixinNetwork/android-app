package one.mixin.android.api.request

import android.os.Build
import com.google.gson.annotations.SerializedName
import one.mixin.android.BuildConfig

data class AccountRequest(
    val code: String? = null,
    @SerializedName("notification_token")
    val notificationToken: String? = null,
    @SerializedName("registration_id")
    val registrationId: Int? = null,
    val platform: String = "Android",
    @SerializedName("platform_version")
    val platformVersion: String = Build.VERSION.RELEASE,
    @SerializedName("app_version")
    val appVersion: String = BuildConfig.VERSION_NAME,
    @SerializedName("package_name")
    val packageName: String = BuildConfig.APPLICATION_ID,
    var purpose: String = VerificationPurpose.SESSION.name,
    val pin: String? = null,
    @SerializedName("session_secret")
    val sessionSecret: String? = null,
    @SerializedName("master_public_hex")
    val masterPublicHex: String? = null,
    @SerializedName("master_message_hex")
    val masterMessageHex: String? = null,
    @SerializedName("master_signature_hex")
    val masterSignatureHex: String? = null,
    @SerializedName("salt_base64")
    val saltBase64: String? = null,
)
