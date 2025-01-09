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
        return appId != null
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
    return if (isMao())
        this
    else {
        when {
            endsWith(".mao") -> this
            endsWith(".") -> "${this}mao"
            endsWith(".m") -> "${this}ao"
            endsWith(".ma") -> "${this}o"
            else -> "${this}.mao"
        }
    }
}