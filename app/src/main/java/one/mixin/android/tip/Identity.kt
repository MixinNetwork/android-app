package one.mixin.android.tip

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2KtResult
import one.mixin.android.api.service.TipService
import one.mixin.android.crypto.aesDecrypt
import one.mixin.android.crypto.argon2IHash
import one.mixin.android.crypto.sha3Sum256
import one.mixin.android.extension.base64RawURLDecode
import one.mixin.android.extension.decodeBase64
import one.mixin.android.session.Session
import one.mixin.android.tip.exception.TipNullException
import javax.inject.Inject

class Identity
    @Inject
    internal constructor(
        private val tipService: TipService,
        private val argon2Kt: Argon2Kt,
    ) {
        // The two returns are private key and watcher
        suspend fun getIdentityPrivAndWatcher(pin: String): Pair<ByteArray, ByteArray> {
            val plain = getIdentitySeed()
            val hashResult: Argon2KtResult = argon2Kt.argon2IHash(pin.toByteArray(), plain)
            return Pair(hashResult.rawHashAsByteArray(), plain.sha3Sum256())
        }

        suspend fun getWatcher(): ByteArray = getIdentitySeed().sha3Sum256()

        private suspend fun getIdentitySeed(): ByteArray {
            val tipIdentity = tipNetwork { tipService.tipIdentity() }.getOrThrow().seedBase64.base64RawURLDecode()
            val pinToken = Session.getPinToken()?.decodeBase64() ?: throw TipNullException("No pin token")
            return aesDecrypt(pinToken, tipIdentity)
        }
    }
