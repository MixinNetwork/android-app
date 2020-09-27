package one.mixin.android.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec
import okhttp3.Request
import one.mixin.android.Constants.Account.PREF_TRIED_UPDATE_KEY
import one.mixin.android.MixinApplication
import one.mixin.android.crypto.aesEncrypt
import one.mixin.android.crypto.getRSAPrivateKeyFromString
import one.mixin.android.extension.bodyToString
import one.mixin.android.extension.clear
import one.mixin.android.extension.cutOut
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putLong
import one.mixin.android.extension.putString
import one.mixin.android.extension.remove
import one.mixin.android.extension.sha256
import one.mixin.android.extension.sharedPreferences
import one.mixin.android.extension.toHex
import one.mixin.android.vo.Account
import timber.log.Timber
import java.security.Key
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

object Session {
    const val PREF_EXTENSION_SESSION_ID = "pref_extension_session_id"
    const val PREF_SESSION = "pref_session"

    private var self: Account? = null
    private const val PREF_PIN_ITERATOR = "pref_pin_iterator"
    private const val PREF_PIN_TOKEN = "pref_pin_token"
    private const val PREF_NAME_ACCOUNT = "pref_name_account"
    private const val PREF_NAME_TOKEN = "pref_name_token"
    private const val PREF_ED25519_PRIVATE_KEY = "pref_ed25519_private_key"

    fun storeAccount(account: Account) {
        self = account
        val preference = MixinApplication.appContext.sharedPreferences(PREF_SESSION)
        preference.putString(PREF_NAME_ACCOUNT, Gson().toJson(account))
    }

    fun getAccount(): Account? = if (self != null) {
        self
    } else {
        val preference = MixinApplication.appContext.sharedPreferences(PREF_SESSION)
        val json = preference.getString(PREF_NAME_ACCOUNT, "")
        if (!json.isNullOrBlank()) {
            Gson().fromJson<Account>(json, object : TypeToken<Account>() {}.type)
        } else {
            null
        }
    }

    fun clearAccount() {
        self = null
        val preference = MixinApplication.appContext.sharedPreferences(PREF_SESSION)
        preference.clear()
    }

    fun storeEd25519PrivateKey(token: String) {
        val preference = MixinApplication.appContext.sharedPreferences(PREF_SESSION)
        preference.putString(PREF_ED25519_PRIVATE_KEY, token)
    }

    fun getEd25519PrivateKey(): String? {
        val preference = MixinApplication.appContext.sharedPreferences(PREF_SESSION)
        return preference.getString(PREF_ED25519_PRIVATE_KEY, null)
    }

    fun storeToken(token: String) {
        val preference = MixinApplication.appContext.sharedPreferences(PREF_SESSION)
        preference.putString(PREF_NAME_TOKEN, token)
    }

    private fun getToken(): String? {
        val preference = MixinApplication.appContext.sharedPreferences(PREF_SESSION)
        return preference.getString(PREF_NAME_TOKEN, null)
    }

    fun storeExtensionSessionId(extensionSession: String) {
        val preference = MixinApplication.appContext.sharedPreferences(PREF_SESSION)
        preference.putString(PREF_EXTENSION_SESSION_ID, extensionSession)
    }

    fun getExtensionSessionId(): String? {
        val preference = MixinApplication.appContext.sharedPreferences(PREF_SESSION)
        return preference.getString(PREF_EXTENSION_SESSION_ID, null)
    }

    fun deleteExtensionSessionId() {
        val preference = MixinApplication.appContext.sharedPreferences(PREF_SESSION)
        preference.remove(PREF_EXTENSION_SESSION_ID)
    }

    fun storePinToken(pinToken: String) {
        val preference = MixinApplication.appContext.sharedPreferences(PREF_SESSION)
        preference.putString(PREF_PIN_TOKEN, pinToken)
    }

    fun getPinToken(): String? {
        val preference = MixinApplication.appContext.sharedPreferences(PREF_SESSION)
        return preference.getString(PREF_PIN_TOKEN, null)
    }

    fun storePinIterator(pinIterator: Long) {
        val preference = MixinApplication.appContext.sharedPreferences(PREF_SESSION)
        preference.putLong(PREF_PIN_ITERATOR, pinIterator)
    }

    fun getPinIterator(): Long {
        val preference = MixinApplication.appContext.sharedPreferences(PREF_SESSION)
        return preference.getLong(PREF_PIN_ITERATOR, 1)
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

    fun checkToken() = getAccount() != null && !getPinToken().isNullOrBlank()

    fun shouldUpdateKey() = getEd25519PrivateKey().isNullOrBlank() &&
        !MixinApplication.appContext.defaultSharedPreferences
            .getBoolean(PREF_TRIED_UPDATE_KEY, false)

    internal val ed25519 = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519)

    fun signToken(acct: Account?, request: Request, key: Key? = getJwtKey(true)): String {
        if (acct == null) {
            return ""
        }
        key ?: return ""

        val expire = System.currentTimeMillis() / 1000 + 1800
        val iat = System.currentTimeMillis() / 1000

        var content = "${request.method}${request.url.cutOut()}"
        request.body?.apply {
            if (contentLength() > 0) {
                content += bodyToString()
            }
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
            .signWith(key)
            .compact()
    }

    fun requestDelay(acct: Account?, string: String, offset: Int, key: Key? = getJwtKey(false)): JwtResult {
        if (acct == null) {
            return JwtResult(false)
        }
        key ?: return JwtResult(false)

        return try {
            val iat = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(string).body[Claims.ISSUED_AT] as Int
            JwtResult(abs(System.currentTimeMillis() / 1000 - iat) > offset, requestTime = iat.toLong())
        } catch (e: ExpiredJwtException) {
            Timber.w(e)
            reportException(e)
            JwtResult(true)
        } catch (e: Exception) {
            Timber.e(e)
            reportException(e)
            JwtResult(false)
        }
    }

    fun getFiatCurrency() = getAccount()?.fiatCurrency ?: "USD"

    private fun getJwtKey(isSign: Boolean): Key? {
        val keyBase64 = getEd25519PrivateKey()
        val isRsa = keyBase64.isNullOrBlank()
        return if (isRsa) {
            val token = getToken()
            if (token.isNullOrBlank()) {
                return null
            }
            getRSAPrivateKeyFromString(token)
        } else {
            val privateSpec = EdDSAPrivateKeySpec(keyBase64?.decodeBase64(), ed25519)
            if (isSign) {
                EdDSAPrivateKey(privateSpec)
            } else {
                EdDSAPublicKey(EdDSAPublicKeySpec(privateSpec.a, ed25519))
            }
        }
    }
}

fun encryptPin(key: String, code: String?): String? {
    val pinCode = code ?: return null
    val iterator = Session.getPinIterator()
    val based = aesEncrypt(key, iterator, pinCode)
    Session.storePinIterator(iterator + 1)
    return based
}
