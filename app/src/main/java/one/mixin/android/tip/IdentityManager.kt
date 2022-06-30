package one.mixin.android.tip

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2KtResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.service.TipService
import one.mixin.android.crypto.aesDecrypt
import one.mixin.android.crypto.argon2IdHash
import one.mixin.android.extension.base64RawUrlDecode
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.toHex
import one.mixin.android.session.Session
import timber.log.Timber
import javax.inject.Inject

class IdentityManager @Inject internal constructor(private val tipService: TipService) {

    suspend fun getIdentityPriv(pin: String): ByteArray? {
        val tipIdentity = handleMixinResponse(
            invokeNetwork = {
                tipService.tipIdentity()
            },
            switchContext = Dispatchers.IO,
            successBlock = {
                requireNotNull(it.data) { "Required tipIdentity response data was null." }
            }
        ) ?: return null

        val pinToken = Session.getPinToken()?.decodeBase64() ?: return null
        val plain = aesDecrypt(pinToken, tipIdentity.seedBase64.base64RawUrlDecode())
        Timber.d("tip identity ${plain.toHex()}")
        val argon2Kt = withContext(Dispatchers.Main) {
            Argon2Kt()
        }
        val hashResult: Argon2KtResult = argon2Kt.argon2IdHash(pin, plain)
        return hashResult.rawHashAsByteArray()
    }
}
