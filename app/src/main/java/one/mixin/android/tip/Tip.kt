package one.mixin.android.tip

import android.content.Context
import net.i2p.crypto.eddsa.EdDSAEngine
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec
import one.mixin.android.Constants
import one.mixin.android.RxBus
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
import one.mixin.android.event.TipEvent
import one.mixin.android.extension.base64RawEncode
import one.mixin.android.extension.base64RawUrlDecode
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.nowInUtcNano
import one.mixin.android.extension.putString
import one.mixin.android.extension.toBeByteArray
import one.mixin.android.job.TipCounterSyncedLiveData
import one.mixin.android.session.Session
import one.mixin.android.session.encryptPin
import one.mixin.android.session.encryptTipPin
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.deleteKeyByAlias
import one.mixin.android.util.getDecryptCipher
import one.mixin.android.util.getEncryptCipher
import timber.log.Timber
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject

class Tip @Inject internal constructor(
    private val ephemeral: Ephemeral,
    private val identity: Identity,
    private val tipService: TipService,
    private val accountService: AccountService,
    internal val tipNode: TipNode,
    private val tipCounterSynced: TipCounterSyncedLiveData,
) {
    private val observers = mutableListOf<Observer>()

    suspend fun getOrRecoverTipPriv(context: Context, pin: String, recoverIfNotExists: Boolean): Result<ByteArray> =
        kotlin.runCatching {
            assertTipCounterSynced(tipCounterSynced)

            val privTip = readTipPriv(context)
            if (privTip == null) {
                if (recoverIfNotExists) {
                    val deviceId = context.defaultSharedPreferences.getString(Constants.DEVICE_ID, null)
                        ?: throw TipNullException("Device id is null")
                    return@runCatching createTipPriv(context, pin, deviceId, forRecover = true).getOrThrow()
                }
                throw TipNullException("PrivTip is null, but not recover")
            }

            val aesKey = getAesKey(pin)

            val privTipKey = (aesKey + pin.toByteArray()).sha3Sum256()
            aesDecrypt(privTipKey, privTip)
        }

    suspend fun createTipPriv(context: Context, pin: String, deviceId: String, failedSigners: List<TipSigner>? = null, legacyPin: String? = null, forRecover: Boolean = false): Result<ByteArray> =
        kotlin.runCatching {
            val ephemeralSeed = ephemeral.getEphemeralSeed(context, deviceId)
            val identityPair = identity.getIdentityPrivAndWatcher(pin)
            createPriv(context, identityPair.priKey, ephemeralSeed, identityPair.watcher, pin, failedSigners, legacyPin, forRecover)
        }

    suspend fun updateTipPriv(context: Context, deviceId: String, newPin: String, oldPin: String?, failedSigners: List<TipSigner>? = null): Result<ByteArray> =
        kotlin.runCatching {
            val ephemeralSeed = ephemeral.getEphemeralSeed(context, deviceId)

            if (oldPin.isNullOrBlank()) { // node success
                val identityPair = identity.getIdentityPrivAndWatcher(newPin)
                updatePriv(context, identityPair.priKey, ephemeralSeed, identityPair.watcher, newPin, null)
            } else {
                val identityPair = identity.getIdentityPrivAndWatcher(oldPin)
                val assigneePriv = identity.getIdentityPrivAndWatcher(newPin).priKey
                updatePriv(context, identityPair.priKey, ephemeralSeed, identityPair.watcher, newPin, assigneePriv, failedSigners)
            }
        }

    @Throws(IOException::class)
    suspend fun watchTipNodeCounters(): List<TipNode.TipNodeCounter> {
        val watcher = identity.getWatcher()
        return tipNode.watch(watcher)
    }

    internal fun tipNodeCount() = tipNode.nodeCount

    @Throws(TipException::class, TipNodeException::class)
    private suspend fun createPriv(context: Context, identityPriv: ByteArray, ephemeral: ByteArray, watcher: ByteArray, pin: String, failedSigners: List<TipSigner>? = null, legacyPin: String? = null, forRecover: Boolean = false): ByteArray {
        val aggSig = tipNode.sign(
            identityPriv, ephemeral, watcher, null, failedSigners,
            callback = object : TipNode.Callback {
                override fun onNodeComplete(step: Int, total: Int) {
                    observers.forEach { it.onSyncing(step, total) }
                }
            }
        ).sha3Sum256() // use sha3-256(recover-signature) as priv

        observers.forEach { it.onSyncingComplete() }

        val privateSpec = EdDSAPrivateKeySpec(aggSig.copyOf(), ed25519)
        val pub = EdDSAPublicKey(EdDSAPublicKeySpec(privateSpec.a, ed25519))

        val localPub = Session.getTipPub()
        if (!localPub.isNullOrBlank() && !localPub.base64RawUrlDecode().contentEquals(pub.abyte)) {
            Timber.e("local pub not equals to new generated, PIN incorrect")
            throw PinIncorrectException()
        }

        val aesKey = generateAesKey(pin)
        if (forRecover) {
            encryptAndSaveTipPriv(context, pin, aggSig, aesKey)
            return aggSig
        }

        replaceOldEncryptedPin(pub, legacyPin)
        encryptAndSaveTipPriv(context, pin, aggSig, aesKey)

        return aggSig
    }

    @Throws(TipException::class, TipNodeException::class)
    private suspend fun updatePriv(context: Context, identityPriv: ByteArray, ephemeral: ByteArray, watcher: ByteArray, newPin: String, assigneePriv: ByteArray?, failedSigners: List<TipSigner>? = null): ByteArray {
        val callback = object : TipNode.Callback {
            override fun onNodeComplete(step: Int, total: Int) {
                observers.forEach { it.onSyncing(step, total) }
            }
        }
        val aggSig = tipNode.sign(identityPriv, ephemeral, watcher, assigneePriv, failedSigners, callback = callback)
            .sha3Sum256() // use sha3-256(recover-signature) as priv

        observers.forEach { it.onSyncingComplete() }

        val aesKey = generateAesKey(newPin)
        replaceEncryptedPin(aggSig)
        encryptAndSaveTipPriv(context, newPin, aggSig, aesKey)

        return aggSig
    }

    @Throws(IOException::class, TipNetworkException::class)
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

    @Throws(IOException::class, TipNetworkException::class)
    private suspend fun replaceEncryptedPin(aggSig: ByteArray) {
        val privateSpec = EdDSAPrivateKeySpec(aggSig.copyOf(), ed25519)
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
    private fun encryptAndSaveTipPriv(context: Context, pin: String, aggSig: ByteArray, aesKey: ByteArray) {
        val privTipKey = (aesKey + pin.toByteArray()).sha3Sum256()
        val privTip = aesEncrypt(privTipKey, aggSig)

        storeTipPriv(context, privTip)
    }

    @Throws(TipException::class)
    private suspend fun generateAesKey(pin: String): ByteArray {
        val sessionPriv =
            Session.getEd25519Seed()?.decodeBase64() ?: throw TipNullException("No ed25519 key")
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
        tipNetworkNullable { tipService.updateTipSecret(tipSecretRequest) }.getOrThrow()
        return aesKey
    }

    @Throws(TipException::class)
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
        val response = tipNetwork { tipService.readTipSecret(tipSecretRequest) }.getOrThrow()
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

    @Throws(TipCounterNotSyncedException::class)
    private suspend fun assertTipCounterSynced(tipCounterSynced: TipCounterSyncedLiveData) {
        if (!tipCounterSynced.synced) {
            runCatching {
                checkCounter(
                    Session.getTipCounter(),
                    onNodeCounterGreaterThanServer = {
                        RxBus.publish(TipEvent(it))
                        throw TipCounterNotSyncedException()
                    },
                    onNodeCounterInconsistency = { nodeMaxCounter, failedSigners ->
                        RxBus.publish(TipEvent(nodeMaxCounter, failedSigners))
                        throw TipCounterNotSyncedException()
                    }
                )
            }.onSuccess { tipCounterSynced.synced = true }
                .onFailure {
                    Timber.d("checkAndPublishTipCounterSynced meet ${it.localizedMessage}")
                    ErrorHandler.handleError(it)
                    throw TipCounterNotSyncedException()
                }
        }
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

    fun addObserver(observer: Observer) {
        observers.add(observer)
    }

    fun removeObserver(observer: Observer) {
        observers.remove(observer)
    }

    interface Observer {
        fun onSyncing(step: Int, total: Int)
        fun onSyncingComplete()
    }
}
