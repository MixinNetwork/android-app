package one.mixin.android.tip

import android.content.Context
import com.lambdapioneer.argon2kt.Argon2Kt
import ed25519.Ed25519
import one.mixin.android.Constants
import one.mixin.android.RxBus
import one.mixin.android.api.request.PinRequest
import one.mixin.android.api.request.TipSecretAction
import one.mixin.android.api.request.TipSecretReadRequest
import one.mixin.android.api.request.TipSecretRequest
import one.mixin.android.api.response.TipSigner
import one.mixin.android.api.service.AccountService
import one.mixin.android.api.service.TipService
import one.mixin.android.crypto.BasePinCipher
import one.mixin.android.crypto.EdKeyPair
import one.mixin.android.crypto.aesDecrypt
import one.mixin.android.crypto.aesEncrypt
import one.mixin.android.crypto.argon2IHash
import one.mixin.android.crypto.generateRandomBytes
import one.mixin.android.crypto.getValueFromEncryptedPreferences
import one.mixin.android.crypto.isMnemonicValid
import one.mixin.android.crypto.newKeyPairFromMnemonic
import one.mixin.android.crypto.newKeyPairFromSeed
import one.mixin.android.crypto.removeValueFromEncryptedPreferences
import one.mixin.android.crypto.sha3Sum256
import one.mixin.android.crypto.storeValueInEncryptedPreferences
import one.mixin.android.crypto.toCompleteMnemonic
import one.mixin.android.crypto.toMnemonic
import one.mixin.android.event.TipEvent
import one.mixin.android.extension.base64RawURLDecode
import one.mixin.android.extension.base64RawURLEncode
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getStackTraceString
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.extension.nowInUtcNano
import one.mixin.android.extension.remove
import one.mixin.android.extension.toBeByteArray
import one.mixin.android.extension.toHex
import one.mixin.android.job.TipCounterSyncedLiveData
import one.mixin.android.session.Session
import one.mixin.android.tip.exception.PinIncorrectException
import one.mixin.android.tip.exception.TipCounterNotSyncedException
import one.mixin.android.tip.exception.TipException
import one.mixin.android.tip.exception.TipInvalidCounterGroups
import one.mixin.android.tip.exception.TipNetworkException
import one.mixin.android.tip.exception.TipNodeException
import one.mixin.android.tip.exception.TipNotAllWatcherSuccessException
import one.mixin.android.tip.exception.TipNullException
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.reportException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class Tip
    @Inject
    internal constructor(
        private val ephemeral: Ephemeral,
        private val identity: Identity,
        private val argon2Kt: Argon2Kt,
        private val tipService: TipService,
        private val accountService: AccountService,
        private val tipNode: TipNode,
        private val tipCounterSynced: TipCounterSyncedLiveData,
    ) : BasePinCipher() {
        private val observers = mutableListOf<Observer>()

        suspend fun createTipPriv(
            context: Context,
            pin: String,
            deviceId: String,
            failedSigners: List<TipSigner>? = null,
            legacyPin: String? = null,
            forRecover: Boolean = false,
        ): Result<ByteArray> =
            kotlin.runCatching {
                val ephemeralSeed = ephemeral.getEphemeralSeed(context, deviceId)
                Timber.e("createTipPriv after getEphemeralSeed")
                val (priKey, watcher) = identity.getIdentityPrivAndWatcher(pin)
                Timber.e("createTipPriv after getIdentityPrivAndWatcher")
                createPriv(context, priKey, ephemeralSeed, watcher, pin, failedSigners, legacyPin, forRecover)
            }

        suspend fun updateTipPriv(
            context: Context,
            deviceId: String,
            newPin: String,
            oldPin: String,
            counterEqual: Boolean,
            failedSigners: List<TipSigner>? = null,
        ): Result<ByteArray> =
            kotlin.runCatching {
                val ephemeralSeed = ephemeral.getEphemeralSeed(context, deviceId)
                Timber.e("updateTipPriv after getEphemeralSeed")

                if (!counterEqual) { // node success
                    Timber.e("updateTipPriv oldPin isNullOrBlank")
                    val (priKey, watcher) = identity.getIdentityPrivAndWatcher(newPin)
                    Timber.e("updateTipPriv after getIdentityPrivAndWatcher")
                    updatePriv(context, priKey, ephemeralSeed, watcher, newPin, oldPin, null)
                } else {
                    val (priKey, watcher) = identity.getIdentityPrivAndWatcher(oldPin)
                    Timber.e("updateTipPriv after getIdentityPrivAndWatcher")
                    val (assigneePriv, _) = identity.getIdentityPrivAndWatcher(newPin)
                    Timber.e("updateTipPriv after get assignee priv")
                    updatePriv(context, priKey, ephemeralSeed, watcher, newPin, oldPin, assigneePriv, failedSigners)
                }
            }

        suspend fun getOrRecoverTipPriv(
            context: Context,
            pin: String,
        ): Result<ByteArray> =
            kotlin.runCatching {
                assertTipCounterSynced(tipCounterSynced)

                val privTip =
                    try {
                        readTipPriv(context)
                    } catch (e: Exception) {
                        Timber.e("read tip priv meet ${e.stackTraceToString()}")
                        clearTipPriv(context)
                        null
                    }
                Timber.e("getOrRecoverTipPriv after readTipPriv privTip == null is ${privTip == null}")

                suspend fun runCreateTipPriv(): ByteArray {
                    val deviceId = context.defaultSharedPreferences.getString(Constants.DEVICE_ID, null) ?: throw TipNullException("Device id is null")
                    return createTipPriv(context, pin, deviceId, forRecover = true).getOrThrow()
                }

                if (privTip == null) {
                    runCreateTipPriv()
                } else {
                    val aesKeyCipher =
                        try {
                            getAesKey(pin)
                        } catch (e: TipNetworkException) {
                            Timber.e("getOrRecoverTipPriv getAesKey meet ${e.getStackTraceString()}")

                            // workaround with read AES key meet bad data,
                            // clear local priv and run create TIP priv process.
                            if (e.error.code == ErrorHandler.BAD_DATA) {
                                clearTipPriv(context)

                                return@runCatching runCreateTipPriv()
                            }
                            throw e
                        }
                    Timber.e("getOrRecoverTipPriv after getAesKey, aesKeyCipher isEmpty: ${aesKeyCipher.isEmpty()}")
                    val pinToken = Session.getPinToken()?.decodeBase64() ?: throw TipNullException("No pin token")
                    try {
                        val aesKey = aesDecrypt(pinToken, aesKeyCipher)
                        Timber.e("getOrRecoverTipPriv after decrypt AES cipher")
                        val privTipKey = (aesKey + pin.toByteArray()).sha3Sum256()
                        aesDecrypt(privTipKey, privTip)
                    } catch (e: Exception) {
                        // AES decrypt failure means the local priv does not match
                        // the AES key or the cipher AES key is invalid, clearing
                        // the local priv and run create TIP priv process.
                        Timber.e("aes decrypt local priv meet ${e.stackTraceToString()}")
                        clearTipPriv(context)

                        runCreateTipPriv()
                    }
                }
            }

        suspend fun checkSalt(context: Context, pin: String, tipPriv: ByteArray) {
            val saltAESKey = generateSaltAESKey(pin, tipPriv)
            val encryptedSalt = this@Tip.getEncryptedSalt(context)
            val salt = aesDecrypt(saltAESKey, encryptedSalt)
            if (Session.isAnonymous()) {
                if (!salt.contentEquals(ByteArray(16))) {
                    throw TipNullException("Salt not matched")
                }
            } else {
                var local = getMnemonicFromEncryptedPreferences(context)
                if (local != null && !salt.contentEquals(local)) {
                    // Clear local mnemonic if salt not matched
                    removeValueFromEncryptedPreferences(context, Constants.Tip.MNEMONIC)
                }
            }
        }

        suspend fun getEncryptSalt(context: Context, pin: String, tipPriv: ByteArray, isAnonymous: Boolean): String {
            val rawSalt = if (isAnonymous) {
                ByteArray(16)
            } else {
                requireNotNull(getMnemonicFromEncryptedPreferences(context)) // Only register safe
            }
            val saltAESKey = generateSaltAESKey(pin, tipPriv)
            val encryptedSalt = aesEncrypt(saltAESKey, rawSalt)
            val pinToken = Session.getPinToken()?.decodeBase64() ?: throw TipNullException("No pin token")
            return aesEncrypt(pinToken, encryptedSalt).base64RawURLEncode()
        }

        suspend fun getEncryptSalt(context: Context, pin: String, tipPriv: ByteArray): String {
            val encryptedSalt = getEncryptedSalt(context)
            val pinToken = Session.getPinToken()?.decodeBase64() ?: throw TipNullException("No pin token")
            return aesEncrypt(pinToken, encryptedSalt).base64RawURLEncode()
        }

        // Each user can only generate once
        fun generateEntropyAndStore(context: Context): ByteArray {
            var entropy: ByteArray
            var mnemonicPhrase: List<String>
            do {
                entropy = generateRandomBytes(16)
                mnemonicPhrase = toCompleteMnemonic(toMnemonic(entropy))
            } while (mnemonicPhrase.distinct().size != mnemonicPhrase.size && isMnemonicValid(mnemonicPhrase))
            storeValueInEncryptedPreferences(context, Constants.Tip.MNEMONIC, entropy)
            return entropy
        }

        suspend fun getMnemonicEdKey(context: Context, pin: String, tipPriv: ByteArray): EdKeyPair {
            var entropy = getMnemonicFromEncryptedPreferences(context)
            if (entropy == null) { // If not exist, get it from safe and decrypt it
                val saltAESKey = generateSaltAESKey(pin, tipPriv)
                val encryptedSalt = getEncryptedSalt(context)
                entropy = aesDecrypt(saltAESKey, encryptedSalt)
            }
            val edKey = newKeyPairFromMnemonic(toMnemonic(entropy))
            return edKey
        }

        suspend fun getMnemonicOrFetchFromSafe(context: Context, pin: String): List<String>? {
            val entropy = getMnemonicFromEncryptedPreferences(context)
            if (entropy != null) {
                return toMnemonic(entropy).split(" ")
            } else {
                val tipPrivateKey = getOrRecoverTipPriv(context, pin).getOrThrow()
                val safeEntropy = getSalt(getEncryptedSalt(context), pin, tipPrivateKey)
                if (safeEntropy.contentEquals(ByteArray(16))) return null
                val mn = toMnemonic(safeEntropy) // legacy user salt 32 bytes
                storeValueInEncryptedPreferences(context, Constants.Tip.MNEMONIC, safeEntropy)
                return mn.split(" ")
            }
        }

        suspend fun getEncryptedSalt(context: Context): ByteArray {
            var salt = readEncryptedSalt(context)
            if (salt != null) {
                return salt
            }
            val account = tipNetwork { accountService.getMeSuspend() }.getOrThrow()
            val pinTokenEncryptedSalt = account.salt?.base64RawURLDecode() ?: throw TipNullException("account has no salt")
            Session.storeAccount(account)
            val pinToken = Session.getPinToken()?.decodeBase64() ?: throw TipNullException("no pin token")
            salt = aesDecrypt(pinToken, pinTokenEncryptedSalt)
            return salt
        }

        fun getSpendPrivFromEncryptedSalt(
            entropy: ByteArray?,
            encryptedSalt: ByteArray,
            pin: String,
            tipPriv: ByteArray,
        ): ByteArray {
            if (entropy == null) {
                val saltAESKey = generateSaltAESKey(pin, tipPriv)
                val salt = aesDecrypt(saltAESKey, encryptedSalt)
                return getSpendPriv(tipPriv, salt)
            } else {
                return getSpendPriv(tipPriv, entropy)
            }
        }

        private fun getSalt(
            encryptedSalt: ByteArray,
            pin: String,
            tipPriv: ByteArray,
        ): ByteArray {
            val saltAESKey = generateSaltAESKey(pin, tipPriv)
            val salt = aesDecrypt(saltAESKey, encryptedSalt)
            return salt
        }

        fun getSpendPriv(context: Context, seed: ByteArray): ByteArray {
            var entropy = getMnemonicFromEncryptedPreferences(context)
            if (entropy == null) { // Register safe must generate mnemonic, Only once
                if (Session.getAccount() != null && !Session.hasPhone() && !Session.saltExported()) {
                    throw IllegalStateException("Entropy lost")
                }
                entropy = generateEntropyAndStore(context)
            }
            return getSpendPriv(seed, entropy)
        }

        private fun getSpendPriv(
            tipPriv: ByteArray,
            salt: ByteArray,
        ): ByteArray =
            argon2Kt.argon2IHash(tipPriv, salt).rawHashAsByteArray()

        private fun generateSaltAESKey(
            pin: String,
            tipPriv: ByteArray,
        ): ByteArray =
            argon2Kt.argon2IHash(pin.toByteArray(), tipPriv).rawHashAsByteArray()

        suspend fun checkCounter(
            tipCounter: Int,
            onNodeCounterNotEqualServer: suspend (Int, List<TipSigner>?) -> Unit,
            onNodeCounterInconsistency: suspend (Int, List<TipSigner>?) -> Unit,
        ) = kotlin.runCatching {
            val (counters, nodeErrorInfo) = watchTipNodeCounters()
            if (counters.isEmpty()) {
                Timber.e("watch tip node counters but counters is empty")
                throw TipNullException("watch tip node counters but counters is empty")
            }

            if (counters.size != tipNodeCount()) {
                Timber.e("watch tip node result size is ${counters.size} is not equals to node count ${tipNodeCount()}")
                throw TipNotAllWatcherSuccessException(nodeErrorInfo)
            }
            val group = counters.groupBy { it.counter }
            if (group.size <= 1) {
                val nodeCounter = counters.first().counter
                Timber.e("watch tip node all counter are $nodeCounter, tipCounter $tipCounter")
                if (nodeCounter == tipCounter) {
                    return@runCatching
                }
                if (nodeCounter < tipCounter) {
                    Timber.e("watch tip node node counter $nodeCounter < tipCounter $tipCounter")
                    // should balance node counter, so see all nodes as failed node
                    val signers = mutableListOf<TipSigner>()
                    counters.mapTo(signers) { it.tipSigner }
                    onNodeCounterNotEqualServer(nodeCounter, signers)
                    return@runCatching
                }

                onNodeCounterNotEqualServer(nodeCounter, null)
                return@runCatching
            }
            if (group.size > 2) {
                Timber.e("watch tip node group size is ${group.size} > 2, counters: ${counters.joinToString()}")
                throw TipInvalidCounterGroups()
            }

            val maxCounter = group.keys.maxBy { it }
            val failedNodes = group[group.keys.minBy { it }]
            val failedSigners =
                if (failedNodes != null) {
                    val signers = mutableListOf<TipSigner>()
                    failedNodes.mapTo(signers) {
                        Timber.e("watch tip node need update node $it")
                        it.tipSigner
                    }
                } else {
                    null
                }
            Timber.e("watch tip node counter maxCounter $maxCounter")
            onNodeCounterInconsistency(maxCounter, failedSigners)
        }

        @Throws(IOException::class)
        private suspend fun watchTipNodeCounters(): Pair<List<TipNode.TipNodeCounter>, String> {
            val watcher = identity.getWatcher()
            return tipNode.watch(watcher)
        }

        fun tipNodeCount() = tipNode.nodeCount

        @Throws(TipException::class, TipNodeException::class)
        private suspend fun createPriv(
            context: Context,
            identityPriv: ByteArray,
            ephemeral: ByteArray,
            watcher: ByteArray,
            pin: String,
            failedSigners: List<TipSigner>? = null,
            legacyPin: String? = null,
            forRecover: Boolean = false,
        ): ByteArray {
            val tipPriv =
                tipNode.sign(
                    identityPriv,
                    ephemeral,
                    watcher,
                    null,
                    failedSigners,
                    forRecover,
                    callback =
                        object : TipNode.Callback {
                            override fun onNodeComplete(
                                step: Int,
                                total: Int,
                            ) {
                                observers.forEach { it.onSyncing(step, total) }
                            }

                            override fun onNodeFailed(info: String) {
                                observers.forEach { it.onNodeFailed(info) }
                            }
                        },
                ).first.sha3Sum256() // use sha3-256(recover-signature) as priv

            observers.forEach { it.onSyncingComplete() }

            val keyPair = newKeyPairFromSeed(tipPriv.copyOf())
            val pub = keyPair.publicKey

            val localPub = Session.getTipPub()
            if (!localPub.isNullOrBlank() && !localPub.base64RawURLDecode().contentEquals(pub)) {
                Timber.e("local pub not equals to new generated, PIN incorrect")
                throw PinIncorrectException()
            }
            Timber.e("createPriv after compare local pub and remote pub")

            val aesKey = generateAesKeyByPin(pin)
            Timber.e("createPriv after generateAesKey, forRecover: $forRecover")
            if (forRecover) {
                encryptAndSaveTipPriv(context, pin, tipPriv, aesKey)
                return tipPriv
            }

            // Clearing local priv before update remote PIN.
            // If the process crashes after updating PIN and before saving to local,
            // the local priv does not match the remote, and this cannot be detected by checkCounter.
            clearTipPriv(context)
            replaceOldEncryptedPin(pub, legacyPin)
            Timber.e("createPriv after replaceOldEncryptedPin")
            encryptAndSaveTipPriv(context, pin, tipPriv, aesKey)
            Timber.e("createPriv after encryptAndSaveTipPriv")

            return tipPriv
        }

        @Throws(TipException::class, TipNodeException::class)
        private suspend fun updatePriv(
            context: Context,
            identityPriv: ByteArray,
            ephemeral: ByteArray,
            watcher: ByteArray,
            newPin: String,
            oldPin: String,
            assigneePriv: ByteArray?,
            failedSigners: List<TipSigner>? = null,
        ): ByteArray {
            val callback =
                object : TipNode.Callback {
                    override fun onNodeComplete(
                        step: Int,
                        total: Int,
                    ) {
                        observers.forEach { it.onSyncing(step, total) }
                    }

                    override fun onNodeFailed(info: String) {
                        observers.forEach { it.onNodeFailed(info) }
                    }
                }
            val pair = tipNode.sign(identityPriv, ephemeral, watcher, assigneePriv, failedSigners, callback = callback)
            val tipPriv = pair.first.sha3Sum256() // use sha3-256(recover-signature) as priv
            val counter = pair.second

            observers.forEach { it.onSyncingComplete() }
            Timber.e("updatePriv after sign")

            val aesKey = generateAesKeyByPin(newPin)

            // Clearing local priv before update remote PIN.
            // If the process crashes after updating PIN and before saving to local,
            // the local priv does not match the remote, and this cannot be detected by checkCounter.
            clearTipPriv(context)
            Timber.e("updatePriv after clear tip priv")

            replaceEncryptedPin(context, tipPriv, counter, newPin, oldPin)
            Timber.e("updatePriv replaceEncryptedPin")
            encryptAndSaveTipPriv(context, newPin, tipPriv, aesKey)
            Timber.e("updatePriv encryptAndSaveTipPriv")

            return tipPriv
        }

        @Throws(IOException::class, TipNetworkException::class)
        private suspend fun replaceOldEncryptedPin(
            pub: ByteArray,
            legacyPin: String? = null,
        ) {
            val pinToken = requireNotNull(Session.getPinToken()?.decodeBase64() ?: throw TipNullException("No pin token"))
            val oldEncryptedPin =
                if (legacyPin != null) {
                    encryptPinInternal(pinToken, legacyPin.toByteArray())
                } else {
                    null
                }
            val newPin = encryptPinInternal(pinToken, pub + 1L.toBeByteArray())
            val pinRequest = PinRequest(newPin, oldEncryptedPin)
            val account = tipNetwork { accountService.updatePinSuspend(pinRequest) }.getOrThrow()
            Session.storeAccount(account)
        }

        @Throws(IOException::class, TipNetworkException::class)
        private suspend fun replaceEncryptedPin(
            context: Context,
            tipPriv: ByteArray,
            nodeCounter: Long,
            pin: String,
            oldPin: String,
        ) {
            val tipCounter = requireNotNull(Session.getTipCounter()).toLong()
            if (tipCounter == nodeCounter) {
                Timber.e("replaceEncryptedPin tipCounter $tipCounter == nodeCounter $nodeCounter")
                return
            }
            val keyPair = newKeyPairFromSeed(tipPriv.copyOf())
            val pub = keyPair.publicKey
            val pinToken = requireNotNull(Session.getPinToken()?.decodeBase64() ?: throw TipNullException("No pin token"))
            val timestamp = TipBody.forVerify(tipCounter)
            val encryptedOldPin = encryptTipPinInternal(pinToken, tipPriv, timestamp)
            val encryptedNewPin =
                encryptPinInternal(
                    pinToken,
                    pub + (nodeCounter).toBeByteArray(),
                )

            var oldPinTokenEncryptedSalt: ByteArray? = null
            val pinTokenEncryptedSalt =
                if (Session.hasSafe()) {
                    val oldSaltAESKey = generateSaltAESKey(oldPin, tipPriv)
                    val oldEncryptedSalt = getEncryptedSalt(context)
                    val salt = aesDecrypt(oldSaltAESKey, oldEncryptedSalt)
                    oldPinTokenEncryptedSalt = aesEncrypt(pinToken, oldEncryptedSalt)
                    val saltAESKey = generateSaltAESKey(pin, tipPriv)
                    aesEncrypt(pinToken, aesEncrypt(saltAESKey, salt)).base64RawURLEncode()
                } else {
                    null
                }

            val pinRequest = PinRequest(encryptedNewPin, encryptedOldPin, pinTokenEncryptedSalt, oldPinTokenEncryptedSalt?.base64RawURLEncode())
            val account = tipNetwork { accountService.updatePinSuspend(pinRequest) }.getOrThrow()
            Session.storeAccount(account)
        }

        @Throws(TipException::class)
        private fun encryptAndSaveTipPriv(
            context: Context,
            pin: String,
            tipPriv: ByteArray,
            aesKey: ByteArray,
        ) {
            val privTipKey = (aesKey + pin.toByteArray()).sha3Sum256()
            val privTip = aesEncrypt(privTipKey, tipPriv)
            if (!storeTipPriv(context, privTip)) {
                throw TipException("Store tip error")
            }
        }

        @Throws(TipException::class)
        private suspend fun generateAesKeyByPin(pin: String): ByteArray {
            val sessionPriv =
                Session.getEd25519Seed()?.decodeBase64() ?: throw TipNullException("No ed25519 key")
            val pinToken =
                Session.getPinToken()?.decodeBase64() ?: throw TipNullException("No pin token")

            val stSeed = (sessionPriv + pin.toByteArray()).sha3Sum256()
            val keyPair = newKeyPairFromSeed(stSeed)
            val stPriv = keyPair.privateKey
            val stPub = keyPair.publicKey
            val aesKey = generateRandomBytes(32)

            val seedBase64 = aesEncrypt(pinToken, aesKey).base64RawURLEncode()
            val secretBase64 = aesEncrypt(pinToken, stPub).base64RawURLEncode()
            val timestamp = nowInUtcNano()

            val sigBase64 = signTimestamp(stPriv, timestamp)

            val tipSecretRequest =
                TipSecretRequest(
                    action = TipSecretAction.UPDATE.name,
                    seedBase64 = seedBase64,
                    secretBase64 = secretBase64,
                    signatureBase64 = sigBase64,
                    timestamp = timestamp,
                )

            Timber.e("generateAesKeyByPin before updateTipSecret")
            val result =
                tipNetworkNullable {
                    tipService.updateTipSecret(tipSecretRequest)
                }
            val e = result.exceptionOrNull()
            if (e != null) {
                if (e is TipNetworkException && e.error.code == ErrorHandler.BAD_DATA) {
                    reportException("Tip tip-secret meet bad data", e)

                    val msg = TipBody.forVerify(timestamp)
                    val goSigBase64 = Ed25519.sign(msg, stSeed).base64RawURLEncode()
                    Timber.e("signature go-ed25519 $goSigBase64")

                    val request =
                        TipSecretRequest(
                            action = TipSecretAction.UPDATE.name,
                            seedBase64 = seedBase64,
                            secretBase64 = secretBase64,
                            signatureBase64 = goSigBase64,
                            timestamp = timestamp,
                        )
                    Timber.e("use go-ed25519 before updateTipSecret")
                    tipNetworkNullable { tipService.updateTipSecret(request) }.getOrThrow()
                    reportException("Tip tip-secret go update success after bad data", e)
                } else {
                    throw e
                }
            }
            return aesKey
        }

        @Throws(TipException::class)
        private suspend fun getAesKey(pin: String): ByteArray {
            val sessionPriv =
                Session.getEd25519Seed()?.decodeBase64() ?: throw TipNullException("No ed25519 key")

            val stSeed = (sessionPriv + pin.toByteArray()).sha3Sum256()
            val keyPair = newKeyPairFromSeed(stSeed)
            val stPriv = keyPair.privateKey
            val timestamp = nowInUtcNano()

            val sigBase64 = signTimestamp(stPriv, timestamp)

            val tipSecretReadRequest =
                TipSecretReadRequest(
                    signatureBase64 = sigBase64,
                    timestamp = timestamp,
                )
            Timber.e("getAesKey before readTipSecret")
            val response = tipNetwork { tipService.readTipSecret(tipSecretReadRequest) }.getOrThrow()
            return response.seedBase64?.base64RawURLDecode() ?: throw TipNullException("Not get tip secret")
        }

        private fun signTimestamp(
            stPriv: ByteArray,
            timestamp: Long,
        ): String {
            val msg = TipBody.forVerify(timestamp)
            val sig = Ed25519.sign(msg, stPriv)
            return sig.base64RawURLEncode()
        }

        @Throws(TipCounterNotSyncedException::class)
        private suspend fun assertTipCounterSynced(tipCounterSynced: TipCounterSyncedLiveData) {
            Timber.e("assertTipCounterSynced tipCounterSynced.synced: ${tipCounterSynced.synced}")
            if (!tipCounterSynced.synced) {
                checkCounter(
                    Session.getTipCounter(),
                    onNodeCounterNotEqualServer = { nodeMaxCounter, failedSigners ->
                        RxBus.publish(TipEvent(nodeMaxCounter, failedSigners))
                        throw TipCounterNotSyncedException()
                    },
                    onNodeCounterInconsistency = { nodeMaxCounter, failedSigners ->
                        RxBus.publish(TipEvent(nodeMaxCounter, failedSigners))
                        throw TipCounterNotSyncedException()
                    },
                ).onSuccess { tipCounterSynced.synced = true }
                    .onFailure {
                        Timber.e("checkAndPublishTipCounterSynced meet ${it.getStackTraceString()}")
                        ErrorHandler.handleError(it)
                        throw TipCounterNotSyncedException()
                    }
            }
        }

        private fun readTipPriv(context: Context): ByteArray? {
            val tipPriv = context.defaultSharedPreferences.getString(Constants.Tip.TIP_PRIV, null)?.hexStringToByteArray() ?: return null

            val iv = tipPriv.slice(0..15).toByteArray()
            val ciphertext = tipPriv.slice(16 until tipPriv.size).toByteArray()
            val cipher = getDecryptCipher(Constants.Tip.ALIAS_TIP_PRIV, iv)
            return cipher.doFinal(ciphertext)
        }

        private fun storeTipPriv(
            context: Context,
            tipPriv: ByteArray,
        ): Boolean {
            val cipher = getEncryptCipher(Constants.Tip.ALIAS_TIP_PRIV)
            val edit = context.defaultSharedPreferences.edit()
            val ciphertext = cipher.doFinal(tipPriv)
            edit.putString(Constants.Tip.TIP_PRIV, (cipher.iv + ciphertext).toHex())
            return edit.commit()
        }

        private fun clearTipPriv(context: Context) {
            context.defaultSharedPreferences.remove(Constants.Tip.TIP_PRIV)
            deleteKeyByAlias(Constants.Tip.ALIAS_TIP_PRIV)
        }

        fun getMnemonicFromEncryptedPreferences(context: Context): ByteArray? {
            return getValueFromEncryptedPreferences(context, Constants.Tip.MNEMONIC)
        }

        fun addObserver(observer: Observer) {
            observers.add(observer)
        }

        fun removeObserver(observer: Observer) {
            observers.remove(observer)
        }

        interface Observer {
            fun onSyncing(
                step: Int,
                total: Int,
            )

            fun onSyncingComplete()

            fun onNodeFailed(info: String)
        }
    }
