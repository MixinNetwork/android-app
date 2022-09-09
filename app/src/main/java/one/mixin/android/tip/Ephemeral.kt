package one.mixin.android.tip

import android.content.Context
import one.mixin.android.Constants
import one.mixin.android.api.request.TipRequest
import one.mixin.android.api.service.TipService
import one.mixin.android.crypto.aesDecrypt
import one.mixin.android.crypto.aesEncrypt
import one.mixin.android.crypto.generateEphemeralSeed
import one.mixin.android.extension.base64RawEncode
import one.mixin.android.extension.base64RawUrlDecode
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putString
import one.mixin.android.extension.toHex
import one.mixin.android.session.Session
import one.mixin.android.tip.exception.TipNullException
import one.mixin.android.util.deleteKeyByAlias
import one.mixin.android.util.getDecryptCipher
import one.mixin.android.util.getEncryptCipher
import timber.log.Timber
import javax.inject.Inject

class Ephemeral @Inject internal constructor(private val tipService: TipService) {

    suspend fun getEphemeralSeed(context: Context, deviceId: String): ByteArray {
        val seed = readEphemeralSeed(context)
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

    private suspend fun createEphemeralSeed(context: Context, deviceId: String): ByteArray {
        val pinToken = Session.getPinToken()?.decodeBase64() ?: throw TipNullException("No pin token")
        val seed = generateEphemeralSeed()
        val cipher = aesEncrypt(pinToken, seed)
        return updateEphemeralSeed(context, deviceId, cipher.base64RawEncode())
    }

    private suspend fun updateEphemeralSeed(context: Context, deviceId: String, seedBase64: String): ByteArray {
        tipNetworkNullable { tipService.tipEphemeral(TipRequest(deviceId, seedBase64)) }.getOrThrow()
        val pinToken = Session.getPinToken()?.decodeBase64() ?: throw TipNullException("No pin token")
        val cipher = seedBase64.base64RawUrlDecode()
        val plain = aesDecrypt(pinToken, cipher)
        storeEphemeralSeed(context, plain)
        return plain
    }

    private fun readEphemeralSeed(context: Context): ByteArray? {
        val ciphertext = context.defaultSharedPreferences.getString(Constants.Tip.EPHEMERAL_SEED, null) ?: return null

        val iv = context.defaultSharedPreferences.getString(Constants.Tip.IV_EPHEMERAL_SEED, null)
        if (iv == null) {
            deleteKeyByAlias(Constants.Tip.ALIAS_EPHEMERAL_SEED)
            context.defaultSharedPreferences.putString(Constants.Tip.EPHEMERAL_SEED, null)
            return null
        }

        val cipher = getDecryptCipher(Constants.Tip.ALIAS_EPHEMERAL_SEED, iv.base64RawUrlDecode())
        return cipher.doFinal(ciphertext.base64RawUrlDecode())
    }

    private fun storeEphemeralSeed(context: Context, seed: ByteArray) {
        val cipher = getEncryptCipher(Constants.Tip.ALIAS_EPHEMERAL_SEED)
        val iv = cipher.iv.base64RawEncode()
        context.defaultSharedPreferences.putString(Constants.Tip.IV_EPHEMERAL_SEED, iv)
        val ciphertext = cipher.doFinal(seed).base64RawEncode()
        context.defaultSharedPreferences.putString(Constants.Tip.EPHEMERAL_SEED, ciphertext)
        // return true or false
    }
}
