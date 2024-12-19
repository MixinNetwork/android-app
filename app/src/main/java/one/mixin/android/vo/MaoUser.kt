package one.mixin.android.vo

import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
fun User.toMaoUser(query: String) :MaoUser{
  val maoName = when {
        query.endsWith(".mao") -> query 
        query.endsWith(".") -> "${query}mao" 
        query.endsWith(".m") -> "${query}ao" 
        query.endsWith(".ma") -> "${query}o" 
        else -> "${query}.mao" 
    }  
    return MaoUser(maoName = maoName, userId, identityNumber, fullName, avatarUrl, isVerified, appId, membership)
} 