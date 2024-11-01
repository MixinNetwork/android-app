package one.mixin.android.tip

import android.content.Context
import one.mixin.android.Constants
import one.mixin.android.api.request.TipRequest
import one.mixin.android.api.service.TipService
import one.mixin.android.crypto.aesDecrypt
import one.mixin.android.crypto.aesEncrypt
import one.mixin.android.crypto.generateEphemeralSeed
import one.mixin.android.extension.base64RawURLDecode
import one.mixin.android.extension.base64RawURLEncode
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.extension.remove
import one.mixin.android.extension.toHex
import one.mixin.android.session.Session
import one.mixin.android.tip.exception.TipException
import one.mixin.android.tip.exception.TipNullException
import timber.log.Timber
import tip.Tip
import javax.inject.Inject

class Ephemeral
    @Inject
    internal constructor(private val tipService: TipService) {
        suspend fun getEphemeralSeed(
            context: Context,
            deviceId: String,
        ): ByteArray {
            val seed =
                try {
                    readEphemeralSeed(context)
                } catch (e: Exception) {
                    Timber.e("read ephemeral seed meet: $e")
                    clearEphemeralSeed(context)
                    null
                }
            Timber.d("getEphemeralSeed from keyStore ${seed?.toHex()}")
            if (seed != null) {
                return seed
            }
            val tipEphemeralList = tipNetwork { tipService.tipEphemerals() }.getOrThrow()
            return if (tipEphemeralList.isEmpty()) {
                createEphemeralSeed(context, deviceId)
            } else {
                val first = tipEphemeralList.first()
                updateEphemeralSeed(context, deviceId, first.seedBase64)
            }
        }

        private suspend fun createEphemeralSeed(
            context: Context,
            deviceId: String,
        ): ByteArray {
            val pinToken = Session.getPinToken()?.decodeBase64() ?: throw TipNullException("No pin token")
            val seed = generateEphemeralSeed()
            val cipher = aesEncrypt(pinToken, seed)
            return updateEphemeralSeed(context, deviceId, cipher.base64RawURLEncode())
        }

        private suspend fun updateEphemeralSeed(
            context: Context,
            deviceId: String,
            seedBase64: String,
        ): ByteArray {
            tipNetworkNullable { tipService.tipEphemeral(TipRequest(deviceId, seedBase64)) }.getOrThrow()
            val pinToken = Session.getPinToken()?.decodeBase64() ?: throw TipNullException("No pin token")
            val cipherText = seedBase64.base64RawURLDecode()
            val plain = aesDecrypt(pinToken, cipherText)
            if (!storeEphemeralSeed(context, plain)) {
                throw TipException("Store ephemeral error")
            }
            return plain
        }

        private fun readEphemeralSeed(context: Context): ByteArray? {
            val ephemeralSeed = context.defaultSharedPreferences.getString(Constants.Tip.EPHEMERAL_SEED, null)?.hexStringToByteArray() ?: return null
            val iv = ephemeralSeed.slice(0..15).toByteArray()
            val ciphertext = ephemeralSeed.slice(16 until ephemeralSeed.size).toByteArray()
            return runCatching {
                val cipher = getDecryptCipher(Constants.Tip.ALIAS_EPHEMERAL_SEED, iv)
                cipher.doFinal(ciphertext)
            }.onFailure {
                Tip.decryptCBC(getKeyByAlias(Constants.Tip.ALIAS_TIP_PRIV)?.encoded, iv, ciphertext)
            }.getOrNull()
        }

        private fun storeEphemeralSeed(
            context: Context,
            seed: ByteArray,
        ): Boolean {
            val cipher = getEncryptCipher(Constants.Tip.ALIAS_EPHEMERAL_SEED)
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(seed)
            val edit = context.defaultSharedPreferences.edit()
            edit.putString(Constants.Tip.EPHEMERAL_SEED, (iv + ciphertext).toHex())
            return edit.commit()
        }

        private fun clearEphemeralSeed(context: Context) {
            context.defaultSharedPreferences.remove(Constants.Tip.EPHEMERAL_SEED)
            deleteKeyByAlias(Constants.Tip.ALIAS_EPHEMERAL_SEED)
        }
    }
