package one.mixin.android.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.microsoft.appcenter.AppCenter
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import okhttp3.Request
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.crypto.aesEncrypt
import one.mixin.android.crypto.getRSAPrivateKeyFromString
import one.mixin.android.extension.bodyToString
import one.mixin.android.extension.clear
import one.mixin.android.extension.cutOut
import one.mixin.android.extension.putLong
import one.mixin.android.extension.putString
import one.mixin.android.extension.remove
import one.mixin.android.extension.sha256
import one.mixin.android.extension.sharedPreferences
import one.mixin.android.extension.toHex
import one.mixin.android.vo.Account
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

object Session {
    private var self: Account? = null
    fun storeAccount(account: Account) {
        self = account
        val preference = MixinApplication.appContext.sharedPreferences(Constants.Account.PREF_SESSION)
        preference.putString(Constants.Account.PREF_NAME_ACCOUNT, Gson().toJson(account))
    }

    fun getAccount(): Account? = if (self != null) {
        self
    } else {
        val preference = MixinApplication.appContext.sharedPreferences(Constants.Account.PREF_SESSION)
        val json = preference.getString(Constants.Account.PREF_NAME_ACCOUNT, "")
        if (!json.isNullOrBlank()) {
            Gson().fromJson<Account>(json, object : TypeToken<Account>() {}.type)
        } else {
            null
        }
    }

    fun clearAccount() {
        self = null
        val preference = MixinApplication.appContext.sharedPreferences(Constants.Account.PREF_SESSION)
        preference.clear()
    }

    fun storeToken(token: String) {
        val preference = MixinApplication.appContext.sharedPreferences(Constants.Account.PREF_SESSION)
        preference.putString(Constants.Account.PREF_NAME_TOKEN, token)
    }

    fun getToken(): String? {
        val preference = MixinApplication.appContext.sharedPreferences(Constants.Account.PREF_SESSION)
        return preference.getString(Constants.Account.PREF_NAME_TOKEN, null)
    }

    fun storeExtensionSessionId(extensionSession: String) {
        val preference = MixinApplication.appContext.sharedPreferences(Constants.Account.PREF_SESSION)
        preference.putString(Constants.Account.PREF_EXTENSION_SESSION_ID, extensionSession)
    }

    fun getExtensionSessionId(): String? {
        val preference = MixinApplication.appContext.sharedPreferences(Constants.Account.PREF_SESSION)
        return preference.getString(Constants.Account.PREF_EXTENSION_SESSION_ID, null)
    }

    fun deleteExtensionSessionId() {
        val preference = MixinApplication.appContext.sharedPreferences(Constants.Account.PREF_SESSION)
        preference.remove(Constants.Account.PREF_EXTENSION_SESSION_ID)
    }

    fun storePinToken(pinToken: String) {
        val preference = MixinApplication.appContext.sharedPreferences(Constants.Account.PREF_SESSION)
        preference.putString(Constants.Account.PREF_PIN_TOKEN, pinToken)
    }

    fun getPinToken(): String? {
        val preference = MixinApplication.appContext.sharedPreferences(Constants.Account.PREF_SESSION)
        return preference.getString(Constants.Account.PREF_PIN_TOKEN, null)
    }

    fun storePinIterator(pinIterator: Long) {
        val preference = MixinApplication.appContext.sharedPreferences(Constants.Account.PREF_SESSION)
        preference.putLong(Constants.Account.PREF_PIN_ITERATOR, pinIterator)
    }

    fun getPinIterator(): Long {
        val preference = MixinApplication.appContext.sharedPreferences(Constants.Account.PREF_SESSION)
        return preference.getLong(Constants.Account.PREF_PIN_ITERATOR, 1)
    }

    fun hasEmergencyContact() = getAccount()?.hasEmergencyContact ?: false

    fun setHasEmergencyContact(enabled: Boolean) {
        getAccount()?.hasEmergencyContact = enabled
    }

    @JvmStatic
    fun getAccountId(): String? {
        val account = getAccount()
        return account?.userId
    }

    fun getSessionId(): String? {
        val account = getAccount()
        return account?.sessionId
    }

    fun checkToken() = getAccount() != null && !getToken().isNullOrBlank()

    fun signToken(acct: Account?, request: Request): String {
        val token = getToken()
        if (acct == null || token == null || token.isBlank()) {
            return ""
        }
        val key = getRSAPrivateKeyFromString(token)
        val expire = System.currentTimeMillis() / 1000 + 1800
        val iat = System.currentTimeMillis() / 1000

        var content = "${request.method}${request.url.cutOut()}"
        if (request.body != null && request.body!!.contentLength() > 0) {
            content += request.body!!.bodyToString()
        }
        return Jwts.builder()
            .setClaims(
                ConcurrentHashMap<String, Any>().apply {
                    put(Claims.ID, UUID.randomUUID().toString())
                    put(Claims.EXPIRATION, expire)
                    put(Claims.ISSUED_AT, iat)
                    put("uid", acct.userId)
                    put("sid", acct.sessionId)
                    put("sig", content.sha256().toHex())
                    put("scp", "FULL")
                }
            )
            .signWith(key, SignatureAlgorithm.RS512)
            .compact()
    }

    fun requestDelay(acct: Account?, string: String, offset: Int): Boolean {
        val token = getToken()
        if (acct == null || token == null || token.isBlank()) {
            return false
        }
        val key = getRSAPrivateKeyFromString(token)
        return try {
            val iat = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(string).body[Claims.ISSUED_AT] as Long
            abs(System.currentTimeMillis() / 1000 - iat) > offset
        } catch (e: Exception) {
            false
        }
    }

    fun getFiatCurrency() = getAccount()?.fiatCurrency ?: "USD"
}

fun encryptPin(key: String, code: String?): String? {
    val pinCode = code ?: return null
    val iterator = Session.getPinIterator()
    val based = aesEncrypt(key, iterator, pinCode)
    Session.storePinIterator(iterator + 1)
    return based
}
