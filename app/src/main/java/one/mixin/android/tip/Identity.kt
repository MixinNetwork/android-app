package one.mixin.android.tip

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2KtResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.api.service.TipService
import one.mixin.android.crypto.aesDecrypt
import one.mixin.android.crypto.argon2IdHash
import one.mixin.android.crypto.sha3Sum256
import one.mixin.android.extension.base64RawUrlDecode
import one.mixin.android.extension.decodeBase64
import one.mixin.android.session.Session
import javax.inject.Inject

class Identity @Inject internal constructor(private val tipService: TipService) {

    suspend fun getIdentityPrivAndWatcher(pin: String): PriKeyAndWatcher {
        val plain = getIdentitySeed()
        val argon2Kt = withContext(Dispatchers.Main) {
            Argon2Kt()
        }
        val hashResult: Argon2KtResult = argon2Kt.argon2IdHash(pin, plain)
        return PriKeyAndWatcher(hashResult.rawHashAsByteArray(), plain.sha3Sum256())
    }

    suspend fun getWatcher(): ByteArray = getIdentitySeed().sha3Sum256()

    private suspend fun getIdentitySeed(): ByteArray {
        val tipIdentity = tipNetwork { tipService.tipIdentity() }.getOrThrow().seedBase64.base64RawUrlDecode()
        val pinToken = Session.getPinToken()?.decodeBase64() ?: throw TipNullException("No pin token")
        return aesDecrypt(pinToken, tipIdentity)
    }
}

class PriKeyAndWatcher(
    val priKey: ByteArray,
    val watcher: ByteArray
)
