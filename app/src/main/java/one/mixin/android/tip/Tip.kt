package one.mixin.android.tip

import android.content.Context
import kotlinx.coroutines.Dispatchers
import net.i2p.crypto.eddsa.EdDSAEngine
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec
import one.mixin.android.Constants
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.PinRequest
import one.mixin.android.api.request.TipSecretAction
import one.mixin.android.api.request.TipSecretRequest
import one.mixin.android.api.response.TipSigner
import one.mixin.android.api.service.AccountService
import one.mixin.android.api.service.TipService
import one.mixin.android.crypto.aesDecrypt
import one.mixin.android.crypto.aesEncrypt
import one.mixin.android.crypto.ed25519
import one.mixin.android.crypto.generateAesKey
import one.mixin.android.crypto.sha3Sum256
import one.mixin.android.extension.base64RawEncode
import one.mixin.android.extension.base64RawUrlDecode
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.nowInUtcNano
import one.mixin.android.extension.putString
import one.mixin.android.session.Session
import one.mixin.android.session.encryptPin
import one.mixin.android.util.deleteKeyByAlias
import one.mixin.android.util.getDecryptCipher
import one.mixin.android.util.getEncryptCipher
import timber.log.Timber
import java.security.MessageDigest
import javax.inject.Inject

class Tip @Inject internal constructor(
    private val ephemeral: Ephemeral,
    private val identityManager: IdentityManager,
    private val tipService: TipService,
    private val accountService: AccountService,
    private val tipNode: TipNode
) {

    suspend fun getTipPriv(context: Context, pin: String, deviceId: String): ByteArray? {
        val ephemeralSeed = ephemeral.getEphemeralSeed(context, deviceId)
        if (ephemeralSeed == null) {
            Timber.d("empty ephemeral seed")
            return null
        }

        val identityPair = identityManager.getIdentityPrivAndWatcher(pin)
        if (identityPair == null) {
            Timber.d("identity pair is null")
            return null
        }

        val tipPub = Session.getTipPub()
        val tipPriv = if (tipPub.isNullOrBlank()) {
            createPriv(context, identityPair.first, ephemeralSeed, identityPair.second, pin)
        } else {
            getPriv(context, pin)
        }
        return tipPriv
    }

    suspend fun updateTipPriv(context: Context, pin: String, deviceId: String, newPin: String, assigneeSigners: List<TipSigner>? = null): ByteArray? {
        val ephemeralSeed = ephemeral.getEphemeralSeed(context, deviceId)
        if (ephemeralSeed == null) {
            Timber.d("empty ephemeral seed")
            return null
        }

        val identityPair = identityManager.getIdentityPrivAndWatcher(pin)
        if (identityPair == null) {
            Timber.d("identity priv and seed is null")
            return null
        }

        val assigneePriv = identityManager.getIdentityPrivAndWatcher(newPin)?.first
        if (assigneePriv == null) {
            Timber.d("assignee priv is null")
            return null
        }

        return updatePriv(context, identityPair.first, ephemeralSeed, identityPair.second, newPin, assigneePriv, assigneeSigners)
    }

    suspend fun watchTipNodeCounters(): List<TipNode.TipNodeCounter>? {
        val watcher = identityManager.getWatcher() ?: return null
        return tipNode.watch(watcher)
    }

    fun tipNodeCount() = tipNode.nodeCount

    private suspend fun getPriv(context: Context, pin: String): ByteArray? {
        val privTip = readTipPriv(context) ?: return null

        val aesKey = getAesKey(pin)
        if (aesKey == null) {
            Timber.d("read priv tip aes key failed")
            return null
        }

        val privTipKey = (aesKey + pin.toByteArray()).sha3Sum256()
        return aesDecrypt(privTipKey, privTip)
    }

    private suspend fun createPriv(context: Context, identityPriv: ByteArray, ephemeral: ByteArray, watcher: ByteArray, pin: String): ByteArray? {
        val aggSig = tipNode.generatePriv(identityPriv, ephemeral, watcher, null) ?: return null

        val privateSpec = EdDSAPrivateKeySpec(ed25519, aggSig.copyOf())
        val pub = EdDSAPublicKey(EdDSAPublicKeySpec(privateSpec.a, ed25519))

        val localPub = Session.getTipPub()
        if (!localPub.isNullOrBlank() && localPub != pub.abyte.base64RawEncode()) {
            Timber.d("local pub not equals to new generated, PIN incorrect")
            return null
        }

        val pinToken = requireNotNull(Session.getPinToken())
        val oldPin = encryptPin(pinToken, pin)
        val newPin = encryptPin(pinToken, pub.abyte)
        val pinRequest = PinRequest(newPin, oldPin)
        handleMixinResponse(
            invokeNetwork = {
                accountService.updatePinSuspend(pinRequest)
            },
            switchContext = Dispatchers.IO,
            successBlock = {
                val r = it.data
                requireNotNull(r) { "Required respond account was null." }
                Session.storeAccount(r)
                return@handleMixinResponse r.tipKeyBase64
            }
        ) ?: return null

        return encryptAndSave(pin, aggSig, context)
    }

    private suspend fun updatePriv(context: Context, identityPriv: ByteArray, ephemeral: ByteArray, watcher: ByteArray, newPin: String, assigneePriv: ByteArray, assigneeSigners: List<TipSigner>? = null): ByteArray? {
        val aggSig = tipNode.generatePriv(identityPriv, ephemeral, watcher, assigneePriv, assigneeSigners) ?: return null

        return encryptAndSave(newPin, aggSig, context)
    }

    private suspend fun encryptAndSave(pin: String, aggSig: ByteArray, context: Context): ByteArray? {
        val aesKey = generateAesKey(pin)
        if (aesKey == null) {
            Timber.d("generate priv tip aes key failed")
            return null
        }

        val privTipKey = (aesKey + pin.toByteArray()).sha3Sum256()
        val privTip = aesEncrypt(privTipKey, aggSig)

        storeTipPriv(context, privTip)
        return aggSig
    }

    private suspend fun generateAesKey(pin: String): ByteArray? {
        val sessionPriv = Session.getEd25519Seed()?.decodeBase64() ?: return null
        val pinToken = Session.getPinToken()?.decodeBase64() ?: return null

        val stSeed = (sessionPriv + pin.toByteArray()).sha3Sum256()
        val privateSpec = EdDSAPrivateKeySpec(stSeed, ed25519)
        val stPriv = EdDSAPrivateKey(privateSpec)
        val stPub = EdDSAPublicKey(EdDSAPublicKeySpec(privateSpec.a, ed25519))
        val aesKey = generateAesKey(32)

        val seedBase64 = aesEncrypt(pinToken, aesKey).base64RawEncode()
        val secretBase64 = aesEncrypt(pinToken, stPub.abyte).base64RawEncode()
        val timestamp = nowInUtcNano()

        val sigBase64 = signTimestamp(stPriv, timestamp)

        val tipSecretRequest = TipSecretRequest(
            action = TipSecretAction.UPDATE.name,
            seedBase64 = seedBase64,
            secretBase64 = secretBase64,
            signatureBase64 = sigBase64,
            timestamp = timestamp,
        )
        return handleMixinResponse(
            invokeNetwork = {
                tipService.tipSecret(tipSecretRequest)
            },
            switchContext = Dispatchers.IO,
            successBlock = {
                return@handleMixinResponse aesKey
            }
        )
    }

    private suspend fun getAesKey(pin: String): ByteArray? {
        val sessionPriv = Session.getEd25519Seed()?.decodeBase64() ?: return null

        val stSeed = (sessionPriv + pin.toByteArray()).sha3Sum256()
        val privateSpec = EdDSAPrivateKeySpec(stSeed, ed25519)
        val stPriv = EdDSAPrivateKey(privateSpec)
        val timestamp = nowInUtcNano()

        val sigBase64 = signTimestamp(stPriv, timestamp)

        val tipSecretRequest = TipSecretRequest(
            action = TipSecretAction.READ.name,
            signatureBase64 = sigBase64,
            timestamp = timestamp,
        )
        val cipher = handleMixinResponse(
            invokeNetwork = {
                tipService.tipSecret(tipSecretRequest)
            },
            switchContext = Dispatchers.IO,
            successBlock = {
                val result = it.data
                requireNotNull(result) { "Required tipSecret response data was null." }
                return@handleMixinResponse result.seedBase64?.base64RawUrlDecode()
            }
        ) ?: return null

        val pinToken = Session.getPinToken()?.decodeBase64() ?: return null
        return aesDecrypt(pinToken, cipher)
    }

    private fun signTimestamp(stPriv: EdDSAPrivateKey, timestamp: Long): String {
        val engine = EdDSAEngine(MessageDigest.getInstance(ed25519.hashAlgorithm))
        engine.initSign(stPriv)
        engine.update(TipBody.forVerify(timestamp))
        return engine.sign().base64RawEncode()
    }

    private fun readTipPriv(context: Context): ByteArray? {
        val ciphertext = context.defaultSharedPreferences.getString(Constants.Tip.TIP_PRIV, null) ?: return null

        val iv = context.defaultSharedPreferences.getString(Constants.Tip.IV_TIP_PRIV, null)
        if (iv == null) {
            deleteKeyByAlias(Constants.Tip.ALIAS_TIP_PRIV)
            context.defaultSharedPreferences.putString(Constants.Tip.TIP_PRIV, null)
            return null
        }

        val cipher = getDecryptCipher(Constants.Tip.ALIAS_TIP_PRIV, iv.base64RawUrlDecode())
        return cipher.doFinal(ciphertext.base64RawUrlDecode())
    }

    private fun storeTipPriv(context: Context, tipPriv: ByteArray) {
        val cipher = getEncryptCipher(Constants.Tip.ALIAS_TIP_PRIV)
        val iv = cipher.iv.base64RawEncode()
        context.defaultSharedPreferences.putString(Constants.Tip.IV_TIP_PRIV, iv)
        val ciphertext = cipher.doFinal(tipPriv).base64RawEncode()
        context.defaultSharedPreferences.putString(Constants.Tip.TIP_PRIV, ciphertext)
        // return true or false
    }
}
