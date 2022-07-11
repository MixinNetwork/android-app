package one.mixin.android.tip

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2KtResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.service.TipService
import one.mixin.android.crypto.aesDecrypt
import one.mixin.android.crypto.argon2IdHash
import one.mixin.android.crypto.sha3Sum256
import one.mixin.android.extension.base64RawUrlDecode
import one.mixin.android.extension.decodeBase64
import one.mixin.android.session.Session
import javax.inject.Inject

class IdentityManager @Inject internal constructor(private val tipService: TipService) {

    suspend fun getIdentityPrivAndWatcher(pin: String): Pair<ByteArray, ByteArray>? {
        val plain = getIdentitySeed() ?: return null
        val argon2Kt = withContext(Dispatchers.Main) {
            Argon2Kt()
        }
        val hashResult: Argon2KtResult = argon2Kt.argon2IdHash(pin, plain)
        return Pair(hashResult.rawHashAsByteArray(), plain.sha3Sum256())
    }

    suspend fun getWatcher(): ByteArray? = getIdentitySeed()?.sha3Sum256()

    private suspend fun getIdentitySeed(): ByteArray? {
        val tipIdentity = handleMixinResponse(
            invokeNetwork = {
                tipService.tipIdentity()
            },
            switchContext = Dispatchers.IO,
            successBlock = {
                it.data?.seedBase64?.base64RawUrlDecode()
            }
        ) ?: return null
        val pinToken = Session.getPinToken()?.decodeBase64() ?: return null
        return aesDecrypt(pinToken, tipIdentity)
    }
}
