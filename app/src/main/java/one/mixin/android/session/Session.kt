package one.mixin.android.session

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec
import okhttp3.Request
import okio.ByteString.Companion.encode
import one.mixin.android.Constants.Account.PREF_TRIED_UPDATE_KEY
import one.mixin.android.MixinApplication
import one.mixin.android.crypto.Base64
import one.mixin.android.crypto.aesEncrypt
import one.mixin.android.crypto.calculateAgreement
import one.mixin.android.crypto.ed25519
import one.mixin.android.crypto.getRSAPrivateKeyFromString
import one.mixin.android.crypto.privateKeyToCurve25519
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.bodyToString
import one.mixin.android.extension.clear
import one.mixin.android.extension.cutOut
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putLong
import one.mixin.android.extension.putString
import one.mixin.android.extension.remove
import one.mixin.android.extension.sharedPreferences
import one.mixin.android.extension.toLeByteArray
import one.mixin.android.util.MoshiHelper
import one.mixin.android.util.reportException
import one.mixin.android.vo.Account
import timber.log.Timber
import java.security.Key
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

object Session {
    const val PREF_EXTENSION_SESSION_ID = "pref_extension_session_id"
    const val PREF_SESSION = "pref_session"

    private var self: Account? = null

    private var seed: String? = null
    private var edPrivateKey: EdDSAPrivateKey? = null
    private var edPublicKey: EdDSAPublicKey? = null

    private const val PREF_PIN_ITERATOR = "pref_pin_iterator"
    private const val PREF_PIN_TOKEN = "pref_pin_token"
    private const val PREF_NAME_ACCOUNT = "pref_name_account"
    private const val PREF_NAME_TOKEN = "pref_name_token"
    private const val PREF_ED25519_PRIVATE_KEY = "pref_ed25519_private_key"

    fun storeAccount(account: Account) {
        self = account
        val preference = MixinApplication.appContext.sharedPreferences(PREF_SESSION)
        preference.putString(PREF_NAME_ACCOUNT, MoshiHelper.getTypeAdapter<Account>(Account::class.java).toJson(account))
    }

    fun getAccount(): Account? = if (self != null) {
        self
    } else {
        val preference = MixinApplication.appContext.sharedPreferences(PREF_SESSION)
        val json = preference.getString(PREF_NAME_ACCOUNT, "")
        if (!json.isNullOrBlank()) {
            MoshiHelper.getTypeAdapter<Account>(Account::class.java).fromJson(json).also {
                self = it
            }
        } else {
            null
        }
    }

    fun clearAccount() {
        self = null
        val preference = MixinApplication.appContext.sharedPreferences(PREF_SESSION)
        preference.clear()
    }

    fun storeEd25519Seed(token: String) {
        val preference = MixinApplication.appContext.sharedPreferences(PREF_SESSION)
        preference.putString(PREF_ED25519_PRIVATE_KEY, token)
        seed = token
        edPrivateKey = null
        edPublicKey = null
        initEdKeypair(token)
    }

    fun getEd25519Seed(): String? {
        if (seed != null) {
            return seed
        } else {
            val preference = MixinApplication.appContext.sharedPreferences(PREF_SESSION)
            seed = preference.getString(PREF_ED25519_PRIVATE_KEY, null)
            return seed
        }
    }

    fun getEd25519PrivateKey(): EdDSAPrivateKey? {
        if (edPrivateKey != null) {
            return edPrivateKey
        } else {
            val seed = getEd25519Seed() ?: return null

            initEdKeypair(seed)
            return edPrivateKey
        }
    }

    fun getEd25519PublicKey(): EdDSAPublicKey? {
        if (edPublicKey != null) {
            return edPublicKey
        } else {
            val seed = getEd25519Seed() ?: return null

            initEdKeypair(seed)
            return edPublicKey
        }
    }

    private fun initEdKeypair(seed: String) {
        val privateSpec = EdDSAPrivateKeySpec(seed.decodeBase64(), ed25519)
        edPrivateKey = EdDSAPrivateKey(privateSpec)
        edPublicKey = EdDSAPublicKey(EdDSAPublicKeySpec(privateSpec.a, ed25519))
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

    fun shouldUpdateKey() = getEd25519Seed().isNullOrBlank() &&
        !MixinApplication.appContext.defaultSharedPreferences
            .getBoolean(PREF_TRIED_UPDATE_KEY, false)

    fun signToken(acct: Account?, request: Request, xRequestId: String, key: Key? = getJwtKey(true)): String {
        if (acct == null || key == null) {
            return ""
        }
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
                    put(Claims.ID, xRequestId)
                    put(Claims.EXPIRATION, expire)
                    put(Claims.ISSUED_AT, iat)
                    put("uid", acct.userId)
                    put("sid", acct.sessionId)
                    put("sig", content.encode().sha256().hex())
                    put("scp", "FULL")
                }
            )
            .signWith(key)
            .compact()
    }

    fun requestDelay(acct: Account?, string: String, offset: Int, key: Key? = getJwtKey(false)): JwtResult {
        if (acct == null || key == null) {
            return JwtResult(false)
        }
        try {
            val iat = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(string).body[Claims.ISSUED_AT] as Int
            return JwtResult(abs(System.currentTimeMillis() / 1000 - iat) > offset, requestTime = iat.toLong())
        } catch (e: ExpiredJwtException) {
            Timber.w(e)
            reportException(e)
            return JwtResult(true)
        } catch (e: Exception) {
            Timber.e(e)
            reportException(e)
        }
        return JwtResult(false)
    }

    fun getFiatCurrency() = getAccount()?.fiatCurrency ?: "USD"

    private fun getJwtKey(isSign: Boolean): Key? {
        val edPrivateKey = getEd25519PrivateKey()
        if (edPrivateKey == null) {
            val token = getToken()
            if (token.isNullOrBlank()) {
                return null
            }
            return getRSAPrivateKeyFromString(token)
        } else {
            if (isSign) {
                return edPrivateKey
            }
            return getEd25519PublicKey()
        }
    }
}

fun encryptPin(key: String, code: String?): String? {
    val pinCode = code ?: return null
    val iterator = Session.getPinIterator()
    val pinByte = pinCode.toByteArray() + (System.currentTimeMillis() / 1000).toLeByteArray() + iterator.toLeByteArray()
    val based = aesEncrypt(Base64.decode(key), pinByte).base64Encode()
    Session.storePinIterator(iterator + 1)
    return based
}

fun decryptPinToken(serverPublicKey: ByteArray, privateKey: EdDSAPrivateKey): ByteArray? {
    val private = privateKeyToCurve25519(privateKey.seed)
    return calculateAgreement(serverPublicKey, private)
}
