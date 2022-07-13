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
import one.mixin.android.extension.toBeByteArray
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.session.encryptPin
import one.mixin.android.session.encryptTipPin
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.deleteKeyByAlias
import one.mixin.android.util.getDecryptCipher
import one.mixin.android.util.getEncryptCipher
import one.mixin.android.util.reportException
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
    suspend fun getOrRecoverTipPriv(context: Context, pin: String, recoverIfNotExists: Boolean): ByteArray? {
        val privTip = readTipPriv(context)
        if (privTip == null) {
            if (recoverIfNotExists) {
                val deviceId = context.defaultSharedPreferences.getString(Constants.DEVICE_ID, null) ?: return null
                return try {
                    createTipPriv(context, pin, deviceId, forRecover = true)
                } catch (e: Exception) {
                    Timber.d(e)
                    null
                }
            }
            return null
        }

        val aesKey = getAesKey(pin)
        if (aesKey == null) {
            Timber.d("read priv tip aes key failed")
            return null
        }

        val privTipKey = (aesKey + pin.toByteArray()).sha3Sum256()
        return aesDecrypt(privTipKey, privTip)
    }

    @Throws(TipException::class, TipNodeException::class)
    suspend fun createTipPriv(context: Context, pin: String, deviceId: String, failedSigners: List<TipSigner>? = null, legacyPin: String? = null, forRecover: Boolean = false): ByteArray? {
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

        return createPriv(context, identityPair.first, ephemeralSeed, identityPair.second, pin, failedSigners, legacyPin, forRecover)
    }

    @Throws(TipNodeException::class)
    suspend fun updateTipPriv(context: Context, pin: String, deviceId: String, newPin: String, nodeSuccess: Boolean, failedSigners: List<TipSigner>? = null): ByteArray? {
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

        return updatePriv(context, identityPair.first, ephemeralSeed, identityPair.second, newPin, assigneePriv, nodeSuccess, failedSigners)
    }

    suspend fun watchTipNodeCounters(): List<TipNode.TipNodeCounter>? {
        val watcher = identityManager.getWatcher() ?: return null
        return tipNode.watch(watcher)
    }

    fun tipNodeCount() = tipNode.nodeCount

    @Throws(TipException::class, TipNodeException::class)
    private suspend fun createPriv(context: Context, identityPriv: ByteArray, ephemeral: ByteArray, watcher: ByteArray, pin: String, failedSigners: List<TipSigner>? = null, legacyPin: String? = null, forRecover: Boolean = false): ByteArray? {
        val aggSig = tipNode.sign(identityPriv, ephemeral, watcher, null, failedSigners)

        val privateSpec = EdDSAPrivateKeySpec(ed25519, aggSig.copyOf())
        val pub = EdDSAPublicKey(EdDSAPublicKeySpec(privateSpec.a, ed25519))

        val localPub = Session.getTipPub()
        if (!localPub.isNullOrBlank() && !localPub.base64RawUrlDecode().contentEquals(pub.abyte)) {
            Timber.d("local pub not equals to new generated, PIN incorrect")
            throw PinIncorrectException()
        }

        encryptAndSave(pin, aggSig, context) ?: return null

        if (forRecover) return aggSig

        val pinToken = requireNotNull(Session.getPinToken())
        val oldEncryptedPin = if (legacyPin != null) {
            encryptPin(pinToken, legacyPin)
        } else null
        val newPin = encryptPin(pinToken, pub.abyte + 1L.toBeByteArray())
        val pinRequest = PinRequest(newPin, oldEncryptedPin)
        handleMixinResponse(
            invokeNetwork = {
                accountService.updatePinSuspend(pinRequest)
            },
            switchContext = Dispatchers.IO,
            successBlock = {
                val r = it.data
                requireNotNull(r) { "Required respond account was null." }
                Session.storeAccount(r)
            }
        ) ?: return null

        return aggSig
    }

    @Throws(TipNodeException::class)
    private suspend fun updatePriv(context: Context, identityPriv: ByteArray, ephemeral: ByteArray, watcher: ByteArray, newPin: String, assigneePriv: ByteArray, nodeSuccess: Boolean, failedSigners: List<TipSigner>? = null): ByteArray? {
        val aggSig = if (nodeSuccess) {
            tipNode.sign(assigneePriv, ephemeral, watcher, null, null)
        } else {
            tipNode.sign(identityPriv, ephemeral, watcher, assigneePriv, failedSigners)
        }

        encryptAndSave(newPin, aggSig, context) ?: return null

        val privateSpec = EdDSAPrivateKeySpec(ed25519, aggSig.copyOf())
        val pub = EdDSAPublicKey(EdDSAPublicKeySpec(privateSpec.a, ed25519))
        val pinToken = requireNotNull(Session.getPinToken())
        val counter = requireNotNull(Session.getTipCounter()).toLong()
        val timestamp = TipBody.forVerify(counter)
        val oldPin = encryptTipPin(aggSig, timestamp)
        val newEncryptPin = encryptPin(pinToken, pub.abyte + (counter + 1).toBeByteArray()) // TODO should use tip node counter?
        val pinRequest = PinRequest(newEncryptPin, oldPin)
        handleMixinResponse(
            invokeNetwork = {
                accountService.updatePinSuspend(pinRequest)
            },
            switchContext = Dispatchers.IO,
            successBlock = {
                val r = it.data
                requireNotNull(r) { "Required respond account was null." }
                Session.storeAccount(r)
            }
        ) ?: return null

        return aggSig
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

fun nodeListJsonToSigners(nodeListJson: String?): List<TipSigner>? =
    if (nodeListJson != null) {
        val assignees = mutableListOf<TipSigner>()
        GsonHelper.customGson.fromJson(nodeListJson, Array<TipNode.TipNodeCounter>::class.java)
            .mapTo(assignees) { it.tipSigner }
    } else null

fun Exception.handleTipException() {
    // TODO i18n
    toast(
        when (this) {
            is PinIncorrectException -> "PIN incorrect"
            is NotEnoughPartialsException -> "Not enough partials"
            is NotAllSignerSuccessException -> "Not all signer success"
            is DifferentIdentityException -> "PIN not same as last time"
            else -> "Set or update PIN failed"
        }
    )
}

suspend fun Tip.checkCounter(
    tipCounter: Int,
    onNodeCounterGreaterThanServer: suspend (Int) -> Unit,
    onNodeCounterNotConsistency: suspend (Int, String) -> Unit,
) {
    val counters = watchTipNodeCounters()
    if (counters.isNullOrEmpty()) {
        Timber.w("watch tip node counters but counters is $counters")
        return
    }

    if (counters.size != tipNodeCount()) {
        Timber.w("watch tip node result size is ${counters.size} is not equals to node count ${tipNodeCount()}")
    }
    val group = counters.groupBy { it.counter }
    if (group.size <= 1) {
        val nodeCounter = counters.first().counter
        Timber.d("watch tip node all counter are $nodeCounter, tipCounter $tipCounter")
        if (nodeCounter == tipCounter) {
            return
        }
        if (nodeCounter < tipCounter) {
            reportIllegal("watch tip node node counter $nodeCounter < tipCounter $tipCounter")
            return
        }

        onNodeCounterGreaterThanServer(nodeCounter)
        return
    }
    if (group.size > 2) {
        reportIllegal("watch tip node meet ${group.size} kinds of counter!")
        return
    }

    val maxCounter = group.keys.maxBy { it }
    val smallNodes = group[group.keys.minBy { it }]
    Timber.d("watch tip node counter maxCounter $maxCounter, need update nodes: $smallNodes")
    onNodeCounterNotConsistency(maxCounter, GsonHelper.customGson.toJson(smallNodes))
}

private fun reportIllegal(msg: String) {
    Timber.w(msg)
    reportException(IllegalStateException(msg))
}
