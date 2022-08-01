package one.mixin.android.tip

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.i2p.crypto.eddsa.EdDSAEngine
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec
import one.mixin.android.Constants
import one.mixin.android.api.MixinResponse
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
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject

class Tip @Inject internal constructor(
    private val ephemeral: Ephemeral,
    private val identityManager: IdentityManager,
    private val tipService: TipService,
    private val accountService: AccountService,
    private val tipNode: TipNode
) {
    suspend fun getOrRecoverTipPriv(context: Context, pin: String, recoverIfNotExists: Boolean): Result<ByteArray> {
        val privTip = readTipPriv(context)
        if (privTip == null) {
            if (recoverIfNotExists) {
                val deviceId = context.defaultSharedPreferences.getString(Constants.DEVICE_ID, null)
                    ?: return Result.failure(TipNullException("Device id is null"))
                return createTipPriv(context, pin, deviceId, forRecover = true)
            }
            return Result.failure(TipNullException("PrivTip is null, but not recover"))
        }

        val aesKey = try {
            getAesKey(pin)
        } catch (e: TipException) {
            return Result.failure(e)
        }

        val privTipKey = (aesKey + pin.toByteArray()).sha3Sum256()
        return Result.success(aesDecrypt(privTipKey, privTip))
    }

    @Throws(TipException::class, TipNodeException::class)
    suspend fun createTipPriv(context: Context, pin: String, deviceId: String, failedSigners: List<TipSigner>? = null, legacyPin: String? = null, forRecover: Boolean = false): Result<ByteArray> {
        return try {
            val ephemeralSeed = ephemeral.getEphemeralSeed(context, deviceId)

            val identityPair = identityManager.getIdentityPrivAndWatcher(pin)

            createPriv(context, identityPair.priKey, ephemeralSeed, identityPair.watcher, pin, failedSigners, legacyPin, forRecover)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Throws(TipNodeException::class)
    suspend fun updateTipPriv(context: Context, pin: String, deviceId: String, newPin: String, nodeSuccess: Boolean, failedSigners: List<TipSigner>? = null): Result<ByteArray> {
        return try {
            val ephemeralSeed = ephemeral.getEphemeralSeed(context, deviceId)

            val identityPair = identityManager.getIdentityPrivAndWatcher(pin)

            val assigneePriv = identityManager.getIdentityPrivAndWatcher(newPin).priKey

            return updatePriv(context, identityPair.priKey, ephemeralSeed, identityPair.watcher, newPin, assigneePriv, nodeSuccess, failedSigners)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun watchTipNodeCounters(): List<TipNode.TipNodeCounter>? {
        val watcher = identityManager.getWatcher() ?: return null
        return tipNode.watch(watcher)
    }

    fun tipNodeCount() = tipNode.nodeCount

    @Throws(TipException::class, TipNodeException::class)
    private suspend fun createPriv(context: Context, identityPriv: ByteArray, ephemeral: ByteArray, watcher: ByteArray, pin: String, failedSigners: List<TipSigner>? = null, legacyPin: String? = null, forRecover: Boolean = false): Result<ByteArray> {
        try {
            val aggSig = tipNode.sign(identityPriv, ephemeral, watcher, null, failedSigners)

            val privateSpec = EdDSAPrivateKeySpec(ed25519, aggSig.copyOf())
            val pub = EdDSAPublicKey(EdDSAPublicKeySpec(privateSpec.a, ed25519))

            val localPub = Session.getTipPub()
            if (!localPub.isNullOrBlank() && !localPub.base64RawUrlDecode()
                .contentEquals(pub.abyte)
            ) {
                Timber.e("local pub not equals to new generated, PIN incorrect")
                throw PinIncorrectException()
            }
            encryptAndSave(context, pin, aggSig)

            if (forRecover) return Result.success(aggSig)

            replaceOldEncryptedPin(pub, legacyPin)

            return Result.success(aggSig)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    @Throws(IOException::class, TipNetWorkException::class)
    private suspend fun replaceOldEncryptedPin(
        pub: EdDSAPublicKey,
        legacyPin: String? = null
    ) {
        val pinToken = requireNotNull(Session.getPinToken())
        val oldEncryptedPin = if (legacyPin != null) {
            encryptPin(pinToken, legacyPin)
        } else null
        val newPin = encryptPin(pinToken, pub.abyte + 1L.toBeByteArray())
        val pinRequest = PinRequest(newPin, oldEncryptedPin)
        val account = tipNetwork { accountService.updatePinSuspend(pinRequest) }.getOrThrow()
        Session.storeAccount(account)
    }

    @Throws(IOException::class, TipNodeException::class, TipNetWorkException::class)
    private suspend fun updatePriv(context: Context, identityPriv: ByteArray, ephemeral: ByteArray, watcher: ByteArray, newPin: String, assigneePriv: ByteArray, nodeSuccess: Boolean, failedSigners: List<TipSigner>? = null): Result<ByteArray> {
        return try {
            val aggSig = if (nodeSuccess) {
                tipNode.sign(assigneePriv, ephemeral, watcher, null, null)
            } else {
                tipNode.sign(identityPriv, ephemeral, watcher, assigneePriv, failedSigners)
            }
            encryptAndSave(context, newPin, aggSig)
            replaceEncryptedPin(aggSig)
            Result.success(aggSig)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Throws(IOException::class, TipNetWorkException::class)
    private suspend fun replaceEncryptedPin(aggSig: ByteArray) {
        val privateSpec = EdDSAPrivateKeySpec(ed25519, aggSig.copyOf())
        val pub = EdDSAPublicKey(EdDSAPublicKeySpec(privateSpec.a, ed25519))
        val pinToken = requireNotNull(Session.getPinToken())
        val counter = requireNotNull(Session.getTipCounter()).toLong()
        val timestamp = TipBody.forVerify(counter)
        val oldPin = encryptTipPin(aggSig, timestamp)
        val newEncryptPin = encryptPin(
            pinToken,
            pub.abyte + (counter + 1).toBeByteArray()
        ) // TODO should use tip node counter?
        val pinRequest = PinRequest(newEncryptPin, oldPin)
        val account = tipNetwork { accountService.updatePinSuspend(pinRequest) }.getOrThrow()
        Session.storeAccount(account)
    }

    @Throws(TipException::class)
    private suspend fun encryptAndSave(context: Context, pin: String, aggSig: ByteArray) {
        val aesKey = generateAesKey(pin)

        val privTipKey = (aesKey + pin.toByteArray()).sha3Sum256()
        val privTip = aesEncrypt(privTipKey, aggSig)

        storeTipPriv(context, privTip)
    }

    @Throws(TipException::class)
    private suspend fun generateAesKey(pin: String): ByteArray {
        val sessionPriv =
            Session.getEd25519Seed()?.decodeBase64() ?: throw TipNullException("No de25519 key")
        val pinToken =
            Session.getPinToken()?.decodeBase64() ?: throw TipNullException("No pin token")

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
        tipNetwork { tipService.tipSecret(tipSecretRequest) }
        return aesKey
    }

    private suspend fun getAesKey(pin: String): ByteArray {
        val sessionPriv =
            Session.getEd25519Seed()?.decodeBase64() ?: throw TipNullException("No ed25519 key")

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
        val response = tipNetwork { tipService.tipSecret(tipSecretRequest) }.getOrThrow()
        val cipher = response.seedBase64?.base64RawUrlDecode() ?: throw TipNullException("Not get tip secret")

        val pinToken =
            Session.getPinToken()?.decodeBase64() ?: throw TipNullException("No pin token")
        return aesDecrypt(pinToken, cipher)
    }

    private fun signTimestamp(stPriv: EdDSAPrivateKey, timestamp: Long): String {
        val engine = EdDSAEngine(MessageDigest.getInstance(ed25519.hashAlgorithm))
        engine.initSign(stPriv)
        engine.update(TipBody.forVerify(timestamp))
        return engine.sign().base64RawEncode()
    }

    private fun readTipPriv(context: Context): ByteArray? {
        val ciphertext =
            context.defaultSharedPreferences.getString(Constants.Tip.TIP_PRIV, null) ?: return null

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

@Throws(IOException::class)
suspend fun <T> tipNetwork(network: suspend () -> MixinResponse<T>): Result<T> {
    return withContext(Dispatchers.IO) {
        val response = network.invoke()
        val data = response.data
        if (response.isSuccess && data != null) {
            return@withContext Result.success(data)
        } else {
            return@withContext Result.failure(
                TipNetWorkException(
                    response.error?.description ?: "Empty error description",
                    response.errorCode
                )
            )
        }
    }
}

fun nodeListJsonToSigners(nodeListJson: String?): List<TipSigner>? =
    if (nodeListJson != null) {
        tipNodeCounterToSigners(GsonHelper.customGson.fromJson(nodeListJson, Array<TipNode.TipNodeCounter>::class.java).toList())
    } else null

fun tipNodeCounterToSigners(tipNodeCounters: List<TipNode.TipNodeCounter>?): List<TipSigner>? =
    if (tipNodeCounters != null) {
        val signers = mutableListOf<TipSigner>()
        tipNodeCounters.mapTo(signers) { it.tipSigner }
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
    onNodeCounterNotConsistency: suspend (Int, List<TipNode.TipNodeCounter>?) -> Unit,
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
        Timber.e("watch tip node all counter are $nodeCounter, tipCounter $tipCounter")
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
    val failedNodes = group[group.keys.minBy { it }]
    Timber.e("watch tip node counter maxCounter $maxCounter, need update nodes: $failedNodes")
    onNodeCounterNotConsistency(maxCounter, failedNodes)
}

private fun reportIllegal(msg: String) {
    Timber.w(msg)
    reportException(IllegalStateException(msg))
}
