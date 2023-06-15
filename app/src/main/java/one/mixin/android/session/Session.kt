package one.mixin.android.session

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import jwt.Jwt
import okhttp3.Request
import one.mixin.android.Constants.Account.PREF_TRIED_UPDATE_KEY
import one.mixin.android.MixinApplication
import one.mixin.android.crypto.EdKeyPair
import one.mixin.android.crypto.calculateAgreement
import one.mixin.android.crypto.newKeyPairFromSeed
import one.mixin.android.crypto.privateKeyToCurve25519
import one.mixin.android.extension.bodyToString
import one.mixin.android.extension.clear
import one.mixin.android.extension.currentTimeSeconds
import one.mixin.android.extension.cutOut
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putLong
import one.mixin.android.extension.putString
import one.mixin.android.extension.remove
import one.mixin.android.extension.sha256
import one.mixin.android.extension.sharedPreferences
import one.mixin.android.extension.toHex
import one.mixin.android.util.reportException
import one.mixin.android.vo.Account
import timber.log.Timber
import kotlin.math.abs

object Session {
    const val PREF_EXTENSION_SESSION_ID = "pref_extension_session_id"
    const val PREF_SESSION = "pref_session"

    private var self: Account? = null

    private var seed: String? = null
    private var edKeyPair: EdKeyPair? = null

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
            Gson().fromJson<Account>(json, object : TypeToken<Account>() {}.type).also {
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
        this.edKeyPair = null
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

    fun getEd25519KeyPair(): EdKeyPair? {
        if (edKeyPair != null) {
            return edKeyPair
        } else {
            val seed = getEd25519Seed() ?: return null
            return initEdKeypair(seed)
        }
    }

    private fun initEdKeypair(seed: String): EdKeyPair {
        val edKeyPair = newKeyPairFromSeed(seed.decodeBase64())

        this.edKeyPair = edKeyPair
        return edKeyPair
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

    fun getTipPub(): String? = getAccount()?.tipKeyBase64

    fun getTipCounter(): Int = getAccount()?.tipCounter ?: 0

    fun isTipFeatureEnabled(): Boolean = getAccount()?.features?.contains("tip") == true

    fun checkToken() = getAccount() != null && !getPinToken().isNullOrBlank()

    fun shouldUpdateKey() = getEd25519Seed().isNullOrBlank() &&
        !MixinApplication.appContext.defaultSharedPreferences
            .getBoolean(PREF_TRIED_UPDATE_KEY, false)

    fun signToken(acct: Account?, request: Request, xRequestId: String, key: ByteArray? = getJwtKey(true)): String {
        if (acct == null || key == null) {
            return ""
        }

        var content = "${request.method}${request.url.cutOut()}"
        request.body?.apply {
            if (contentLength() > 0) {
                content += bodyToString()
            }
        }

        return Jwt.signToken(xRequestId, acct.userId, acct.sessionId, content.sha256().toHex(), key)
    }

    fun requestDelay(acct: Account?, string: String, offset: Int, key: ByteArray? = getJwtKey(false)): JwtResult {
        if (acct == null || key == null) {
            return JwtResult(false)
        }
        val iat = Jwt.parseIat(string, key)
        return when (iat) {
            -1L -> {
                Timber.w("ErrTokenExpired")
                reportException(IllegalArgumentException("ErrTokenExpired"))
                JwtResult(true)
            }
            0L -> {
                Timber.e("")
                JwtResult(false)
            }
            else -> {
                JwtResult(abs(currentTimeSeconds() - iat) > offset, requestTime = iat)
            }
        }
    }

    fun getFiatCurrency() = getAccount()?.fiatCurrency ?: "USD"

    private fun getJwtKey(isSign: Boolean): ByteArray? {
        val edPrivateKey = getEd25519KeyPair()?.privateKey
        if (edPrivateKey == null) {
            val token = getToken()
            if (token.isNullOrBlank()) {
                return null
            }
            return token.toByteArray()
        } else {
            val edPublicKey = getEd25519KeyPair()?.publicKey ?: return null
            return if (isSign) {
                edPrivateKey + edPublicKey
            } else {
                edPublicKey
            }
        }
    }
}

fun decryptPinToken(serverPublicKey: ByteArray, privateKey: ByteArray): ByteArray {
    val private = privateKeyToCurve25519(privateKey)
    return calculateAgreement(serverPublicKey, private)
}
