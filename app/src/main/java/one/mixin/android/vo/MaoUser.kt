package one.mixin.android.vo

import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import one.mixin.android.extension.isMao

class MaoUser(
    val maoName: String,
    val userId: String,
    val identityNumber: String,
    val fullName: String?,
    val avatarUrl: String?,
    val isVerified: Boolean?,
    val appId: String? = null,
    val membership: Membership? = null,
) {
    fun isBot(): Boolean {
        return appId != null && identityNumber.let {
            val n = it.toLongOrNull() ?: return@let false
            return (n in 7000000001..7999999999) || n == 7000L
        }
    }

    fun isMembership(): Boolean {
        return membership?.isMembership() == true
    }

    fun isProsperity(): Boolean {
        return membership?.isProsperity() == true
    }
}

fun User.toMaoUser(maoName: String): MaoUser {
    return MaoUser(maoName = maoName, userId, identityNumber, fullName, avatarUrl, isVerified, appId ?: app?.appId, membership)
}

fun String.completeMao(): String {
    val text = this.lowercase()
    return if (text.isMao())
        text
    else {
        when {
            endsWith(".mao") -> text
            endsWith(".") -> "${text}mao"
            endsWith(".m") -> "${text}ao"
            endsWith(".ma") -> "${text}o"
            else -> "${text}.mao"
        }
    }
}