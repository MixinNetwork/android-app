package one.mixin.android.session

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ed25519.Ed25519
import io.jsonwebtoken.Claims
import io.jsonwebtoken.EdDSAPrivateKey
import io.jsonwebtoken.EdDSAPublicKey
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import jwt.Jwt
import okhttp3.Request
import okio.ByteString.Companion.encode
import okio.ByteString.Companion.toByteString
import one.mixin.android.Constants.Account.PREF_TRIED_UPDATE_KEY
import one.mixin.android.MixinApplication
import one.mixin.android.crypto.EdKeyPair
import one.mixin.android.crypto.aesDecrypt
import one.mixin.android.crypto.calculateAgreement
import one.mixin.android.crypto.getRSAPrivateKeyFromString
import one.mixin.android.crypto.newKeyPairFromSeed
import one.mixin.android.crypto.privateKeyToCurve25519
import one.mixin.android.crypto.sha3Sum256
import one.mixin.android.crypto.useGoEd
import one.mixin.android.extension.base64RawURLDecode
import one.mixin.android.extension.base64RawURLEncode
import one.mixin.android.extension.bodyToString
import one.mixin.android.extension.clear
import one.mixin.android.extension.currentTimeSeconds
import one.mixin.android.extension.cutOut
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.hmacSha256
import one.mixin.android.extension.putLong
import one.mixin.android.extension.putString
import one.mixin.android.extension.remove
import one.mixin.android.extension.sha256
import one.mixin.android.extension.sharedPreferences
import one.mixin.android.extension.startsWithIgnoreCase
import one.mixin.android.extension.toHex
import one.mixin.android.tip.storeEncryptedSalt
import one.mixin.android.util.reportException
import one.mixin.android.util.xinDialCode
import one.mixin.android.vo.Account
import one.mixin.eddsa.Ed25519Sign
import timber.log.Timber
import java.security.Key
import java.util.concurrent.ConcurrentHashMap
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
        preference.putString(PREF_NAME_ACCOUNT, Gson().toJson(account.copy(salt = null)))

        val salt = account.salt
        if (salt.isNullOrEmpty()) {
            return
        }
        val pinToken = getPinToken()?.decodeBase64()
        if (pinToken == null) {
            Timber.e("storeAccount store encrypted salt pin token null")
            return
        }
        val encryptedSalt = aesDecrypt(pinToken, salt.base64RawURLDecode())
        if (!storeEncryptedSalt(MixinApplication.appContext, encryptedSalt)) {
            Timber.e("storeAccount store encrypted salt failed")
        }
    }

    fun getAccount(): Account? =
        if (self != null) {
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

    fun hasPhone():Boolean {
        val account = getAccount()
        val phone = account?.phone
        return !phone.isNullOrBlank() && !phone.startsWithIgnoreCase(xinDialCode)
    }

    fun exportedSalt():Boolean {
        val account = getAccount()
        return (account?.exportedSalt == true).not() // todo
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
        if (this.seed != null) {
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

    fun hasSafe() = self?.hasSafe ?: false

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

    fun checkToken() = getAccount() != null && !getPinToken().isNullOrBlank()

    fun shouldUpdateKey() =
        getEd25519Seed().isNullOrBlank() &&
            !MixinApplication.appContext.defaultSharedPreferences
                .getBoolean(PREF_TRIED_UPDATE_KEY, false)

    fun signToken(
        acct: Account?,
        request: Request,
        xRequestId: String,
    ): String {
        return if (useGoEd()) {
            signGoToken(acct, request, xRequestId)
        } else {
            signLegacyToken(acct, request, xRequestId)
        }
    }

    fun requestDelay(
        acct: Account?,
        string: String,
        offset: Int,
    ): JwtResult {
        return if (useGoEd()) {
            requestGoDelay(acct, string, offset)
        } else {
            requestLegacyDelay(acct, string, offset)
        }
    }

    fun signGoToken(
        acct: Account?,
        request: Request,
        xRequestId: String,
        key: ByteArray? = getGoJwtKey(true),
    ): String {
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

    fun signLegacyToken(
        acct: Account?,
        request: Request,
        xRequestId: String,
        key: Key? = getLegacyJwtKey(true),
    ): String {
        if (acct == null || key == null) {
            return ""
        }
        val expire = currentTimeSeconds() + 1800
        val iat = currentTimeSeconds()

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
                },
            )
            .signWith(key)
            .compact()
    }

    fun requestGoDelay(
        acct: Account?,
        string: String,
        offset: Int,
        key: ByteArray? = getGoJwtKey(false),
    ): JwtResult {
        if (acct == null || key == null) {
            return JwtResult(false)
        }
        return when (val iat = Jwt.parseIat(string, key)) {
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

    fun requestLegacyDelay(
        acct: Account?,
        string: String,
        offset: Int,
        key: Key? = getLegacyJwtKey(false),
    ): JwtResult {
        if (acct == null || key == null) {
            return JwtResult(false)
        }
        try {
            val iat = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(string).body[Claims.ISSUED_AT] as Int
            return JwtResult(abs(currentTimeSeconds() - iat) > offset, requestTime = iat.toLong())
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

    private fun getGoJwtKey(isSign: Boolean): ByteArray? {
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

    private fun getLegacyJwtKey(isSign: Boolean): Key? {
        val edPrivateKey = getEd25519KeyPair()?.privateKey?.let { EdDSAPrivateKey(it.toByteString()) }
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
            return getEd25519KeyPair()?.publicKey?.let { EdDSAPublicKey(it.toByteString()) }
        }
    }

    fun getBotSignature(
        publicKey: String?,
        request: Request,
    ): Pair<Long, String> {
        val edKeyPair = getEd25519KeyPair() ?: return Pair(0L, "")
        val botPk = publicKey?.base64RawURLDecode() ?: return Pair(0L, "")
        val private = privateKeyToCurve25519(edKeyPair.privateKey)
        val sharedKey = calculateAgreement(botPk, private)
        val ts = currentTimeSeconds()
        var content = "$ts${request.method}${request.url.cutOut()}"
        request.body?.apply {
            if (contentLength() > 0) {
                content += bodyToString()
            }
        }
        return Pair(ts, (requireNotNull(getAccountId()).toByteArray() + content.hmacSha256(sharedKey)).base64RawURLEncode())
    }

    fun getRegisterSignature(
        message: String,
        seed: ByteArray,
    ): String {
        val signTarget = message.sha3Sum256()
        return if (useGoEd()) {
            Ed25519.sign(signTarget, seed).base64RawURLEncode()
        } else {
            val keyPair =
                one.mixin.eddsa.KeyPair.newKeyPairFromSeed(seed.toByteString(), checkOnCurve = true)
            EdKeyPair(keyPair.publicKey.toByteArray(), keyPair.privateKey.toByteArray())
            val signer =
                Ed25519Sign(keyPair.privateKey.toByteArray().toByteString(), checkOnCurve = true)
            signer.sign(signTarget.toByteString(), checkOnCurve = true).toByteArray()
                .base64RawURLEncode()
        }
    }
}

fun decryptPinToken(
    serverPublicKey: ByteArray,
    privateKey: ByteArray,
): ByteArray {
    val private = privateKeyToCurve25519(privateKey)
    return calculateAgreement(serverPublicKey, private)
}

fun buildHashMembers(ids: List<String>): String {
    return ids.sortedBy { it }
        .joinToString("")
        .sha3Sum256()
        .joinToString("") { "%02x".format(it) }
}
