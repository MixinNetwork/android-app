package one.mixin.android.tip

import android.content.Context
import crypto.Crypto
import crypto.Scalar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import net.i2p.crypto.eddsa.EdDSAEngine
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec
import okio.Buffer
import one.mixin.android.Constants
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.PinRequest
import one.mixin.android.api.request.TipSecretAction
import one.mixin.android.api.request.TipSecretRequest
import one.mixin.android.api.request.TipSignData
import one.mixin.android.api.request.TipSignRequest
import one.mixin.android.api.response.TipSignResponse
import one.mixin.android.api.response.TipSigner
import one.mixin.android.api.service.AccountService
import one.mixin.android.api.service.TipNodeService
import one.mixin.android.api.service.TipService
import one.mixin.android.crypto.aesDecrypt
import one.mixin.android.crypto.aesEncrypt
import one.mixin.android.crypto.ed25519
import one.mixin.android.crypto.generateAesKey
import one.mixin.android.crypto.sha3Sum256
import one.mixin.android.extension.base64RawEncode
import one.mixin.android.extension.base64RawUrlDecode
import one.mixin.android.extension.currentTimeSeconds
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.extension.nowInUtcNano
import one.mixin.android.extension.putString
import one.mixin.android.extension.toBeByteArray
import one.mixin.android.extension.toHex
import one.mixin.android.session.Session
import one.mixin.android.session.encryptPin
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.deleteKeyByAlias
import one.mixin.android.util.getDecryptCipher
import one.mixin.android.util.getEncryptCipher
import retrofit2.HttpException
import timber.log.Timber
import java.security.MessageDigest
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

class Tip @Inject internal constructor(
    private val ephemeral: Ephemeral,
    private val identityManager: IdentityManager,
    private val tipNodeService: TipNodeService,
    private val tipService: TipService,
    private val accountService: AccountService,
) {
    private val ephemeralGrace = 128.days.inWholeNanoseconds

    private val gson = GsonHelper.customGson

    suspend fun getTipPriv(context: Context, pin: String, deviceId: String): ByteArray? {
        val ephemeralSeed = ephemeral.getEphemeralSeed(context, deviceId)
        if (ephemeralSeed == null) {
            Timber.d("empty ephemeral seed")
            return null
        }

        val identityPriv = identityManager.getIdentityPriv(pin)
        if (identityPriv == null) {
            Timber.d("identity pub is null")
            return null
        }

        val tipPub = Session.getTipPub()
        val tipPriv = if (tipPub.isNullOrBlank()) {
            genPriv(context, identityPriv, ephemeralSeed, pin)
        } else {
            getPriv(context, pin)
        }
        Timber.d("tipPriv: ${tipPriv?.toHex()}")

        return tipPriv
    }

    suspend fun updateTipPriv(context: Context, pin: String, deviceId: String, newPin: String): ByteArray? {
        val ephemeralSeed = ephemeral.getEphemeralSeed(context, deviceId)
        if (ephemeralSeed == null) {
            Timber.d("empty ephemeral seed")
            return null
        }

        val identityPriv = identityManager.getIdentityPriv(pin)
        if (identityPriv == null) {
            Timber.d("identity priv is null")
            return null
        }

        val assigneePriv = identityManager.getIdentityPriv(newPin)
        if (assigneePriv == null) {
            Timber.d("assignee priv is null")
            return null
        }

        val tipPriv = updatePriv(context, identityPriv, ephemeralSeed, newPin, assigneePriv)
        Timber.d("tipPriv: ${tipPriv?.toHex()}")

        return tipPriv
    }

    private suspend fun getPriv(context: Context, pin: String): ByteArray? {
        val privTip = readTipPriv(context) ?: return null
        Timber.d("read priv from keyStore ${privTip.toHex()}")

        val aesKey = getAesKey(pin)
        if (aesKey == null) {
            Timber.d("read priv tip aes key failed")
            return null
        }

        val privTipKey = (aesKey + pin.toByteArray()).sha3Sum256()
        Timber.d("get priv privTipKey ${privTipKey.toHex()}")
        return aesDecrypt(privTipKey, privTip)
    }

    private suspend fun genPriv(context: Context, identityPriv: ByteArray, ephemeral: ByteArray, pin: String): ByteArray? {
        val tipConfig = try {
            tipNodeService.tipConfig()
        } catch (e: Exception) {
            Timber.d(e)
            null
        } ?: return null
        Timber.d("ephemeral ${ephemeral.toHex()}")

        val suite = Crypto.newSuiteBn256()
        val userSk = suite.scalar()
        Timber.d("identityPriv ${identityPriv.toHex()}")
        userSk.setBytes(identityPriv)
        Timber.d("user sk ${userSk.privateKeyBytes().toHex()}")
        val userPkBytes = userSk.publicKey().publicKeyBytes()
        Timber.d("user pk ${userPkBytes.toHex()}")

        val partials = getNodeSigs(userSk, tipConfig.signers, ephemeral)
        if (partials.size < tipConfig.commitments.size) {
            Timber.d("not enough partials ${partials.size} ${tipConfig.commitments.size}")
            return null
        }

        val hexSigs = partials.joinToString(",") { it.toHex() }
        val commitments = tipConfig.commitments.joinToString(",")
        Timber.d("hexSigs $hexSigs")
        val aggSig = Crypto.recoverSignature(hexSigs, commitments, userPkBytes, tipConfig.signers.size.toLong())
        Timber.d("aggSig: ${aggSig.toHex()}")

        val privateSpec = EdDSAPrivateKeySpec(ed25519, aggSig.copyOf())
        val pub = EdDSAPublicKey(EdDSAPublicKeySpec(privateSpec.a, ed25519))

        // check pub == local pub

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
                Timber.d("respond tipKeyBase64 ${r.tipKeyBase64}")
                return@handleMixinResponse r.tipKeyBase64
            }
        ) ?: return null

        val aesKey = genAesKey(pin)
        if (aesKey == null) {
            Timber.d("generate priv tip aes key failed")
            return null
        }

        val privTipKey = (aesKey + pin.toByteArray()).sha3Sum256()
        Timber.d("privTipKey ${privTipKey.toHex()}")
        val privTip = aesEncrypt(privTipKey, aggSig)

        storeTipPriv(context, privTip)
        return aggSig
    }

    private suspend fun updatePriv(context: Context, identityPriv: ByteArray, ephemeral: ByteArray, newPin: String, assigneePriv: ByteArray): ByteArray? {
        val tipConfig = try {
            tipNodeService.tipConfig()
        } catch (e: Exception) {
            Timber.d(e)
            null
        } ?: return null
        Timber.d("ephemeral ${ephemeral.toHex()}")

        val suite = Crypto.newSuiteBn256()
        val userSk = suite.scalar()
        Timber.d("identityPriv ${identityPriv.toHex()}")
        userSk.setBytes(identityPriv)
        Timber.d("user sk ${userSk.privateKeyBytes().toHex()}")
        val userPkBytes = userSk.publicKey().publicKeyBytes()
        Timber.d("user pk ${userPkBytes.toHex()}")

        val assigneeSk = suite.scalar()
        Timber.d("assigneePriv: ${assigneePriv.toHex()}")
        assigneeSk.setBytes(assigneePriv)
        Timber.d("assignee sk ${assigneeSk.privateKeyBytes().toHex()}")
        val assigneePub = assigneeSk.publicKey().publicKeyBytes()
        Timber.d("assigneePub: ${assigneePub.toHex()}")
        val assigneeSig = assigneeSk.sign(assigneePub)
        val assignee = assigneePub + assigneeSig
        Timber.d("assignee ${assignee.toHex()}")

        val partials = getNodeSigs(userSk, tipConfig.signers, ephemeral)
        if (partials.size < tipConfig.commitments.size) {
            Timber.d("not enough partials ${partials.size} ${tipConfig.commitments.size}")
            return null
        }

        val hexSigs = partials.joinToString(",") { it.toHex() }
        val commitments = tipConfig.commitments.joinToString(",")
        Timber.d("hexSigs $hexSigs")
        val aggSig = Crypto.recoverSignature(hexSigs, commitments, userPkBytes, tipConfig.signers.size.toLong())
        Timber.d("aggSig: ${aggSig.toHex()}")

        val aesKey = genAesKey(newPin)
        if (aesKey == null) {
            Timber.d("generate priv tip aes key failed")
            return null
        }

        val privTipKey = (aesKey + newPin.toByteArray()).sha3Sum256()
        Timber.d("privTipKey ${privTipKey.toHex()}")
        val privTip = aesEncrypt(privTipKey, aggSig)

        storeTipPriv(context, privTip)
        return aggSig
    }

    private suspend fun genAesKey(pin: String): ByteArray? {
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

    private fun signTimestamp(
        stPriv: EdDSAPrivateKey,
        timestamp: Long
    ): String {
        val engine = EdDSAEngine(MessageDigest.getInstance(ed25519.hashAlgorithm))
        engine.initSign(stPriv)
        engine.update(TipBody.forVerify(timestamp))
        return engine.sign().base64RawEncode()
    }

    private suspend fun getNodeSigs(userSk: Scalar, tipSigners: List<TipSigner>, ephemeral: ByteArray, assigneePub: ByteArray? = null): List<ByteArray> {
        val result = mutableListOf<ByteArray>()

        val nonce = currentTimeSeconds()
        val grace = ephemeralGrace

        coroutineScope {
            tipSigners.mapIndexed { i, signer ->
                async(Dispatchers.IO) {
                    var success = false
                    var retryCount = 0

                    while (!success) {
                        val pair = fetchTipNodeSigns(userSk, signer, ephemeral, nonce, grace, assigneePub)
                        if (pair.second == 429) {
                            return@async
                        }

                        val sign = pair.first
                        success = sign != null
                        if (success) {
                            require(sign != null)
                            result.add(sign)
                            Timber.d("fetch tip node $i sign success")
                            return@async
                        }

                        retryCount++
                        Timber.d("fetch tip node $i failed, retry $retryCount")
                    }
                }
            }.awaitAll()
        }
        return result
    }

    private suspend fun fetchTipNodeSigns(userSk: Scalar, tipSigner: TipSigner, ephemeral: ByteArray, nonce: Long, grace: Long, assigneePub: ByteArray? = null): Pair<ByteArray?, Int> {
        val tipSignRequest = genTipSignRequest(userSk, tipSigner, ephemeral, nonce, grace, assigneePub)
        return try {
            val r = tipNodeService.postSign(tipSignRequest, tipSigner.api)

            val signerPk = Crypto.pubKeyFromBase58(tipSigner.identity)
            val msg = gson.toJson(r.data).toByteArray()
            try {
                signerPk.verify(msg, r.signature.hexStringToByteArray())
            } catch (e: Exception) {
                Timber.d("verify node response meet ${e.localizedMessage}")
                return Pair(null, -1)
            }

            val sig = parseNodeSigResp(userSk, tipSigner, r)
            Pair(sig, -1)
        } catch (e: Exception) {
            Timber.d(e)
            if (e is HttpException) {
                Pair(null, e.code())
            } else {
                Pair(null, -1)
            }
        }
    }

    private fun parseNodeSigResp(userSk: Scalar, signer: TipSigner, resp: TipSignResponse): ByteArray {
        val signerPk = Crypto.pubKeyFromBase58(signer.identity)
        val plain = Crypto.decrypt(signerPk, userSk, resp.data.cipher.hexStringToByteArray())
        val nonceBytes = plain.slice(0..7).toByteArray()
        var offset = 8
        val partial = plain.slice(offset..offset + 65).toByteArray()
        offset += 66
        val assignor = plain.slice(offset..offset + 127).toByteArray()
        offset += 128
        val timeBytes = plain.slice(offset..offset + 7).toByteArray()
        offset += 8
        val counterBytes = plain.slice(offset..offset + 7).toByteArray()

        val buffer = Buffer()
        buffer.write(nonceBytes)
        val nonce = buffer.readLong()
        buffer.write(timeBytes)
        val time = buffer.readLong()
        buffer.write(counterBytes)
        val counter = buffer.readLong()
        Timber.d("nonce: $nonce, partial: ${partial.toHex()}, assignor: ${assignor.toHex()}, time: $time, counter: $counter")
        return partial
    }

    private fun genTipSignRequest(userSk: Scalar, tipSigner: TipSigner, ephemeral: ByteArray, nonce: Long, grace: Long, assigneePub: ByteArray? = null): TipSignRequest {
        val signerPk = Crypto.pubKeyFromBase58(tipSigner.identity)
        val userPk = userSk.publicKey()
        val esum = (ephemeral + tipSigner.identity.toByteArray()).sha3Sum256()

        var msg = userPk.publicKeyBytes() + esum + nonce.toBeByteArray() + grace.toBeByteArray()
        if (assigneePub != null) {
           msg += assigneePub
        }
        val sig = userSk.sign(msg).toHex()

        val userPkStr = userPk.publicKeyString()
        val data = TipSignData(
            identity = userPkStr,
            assignee = assigneePub?.toHex(),
            ephemeral = esum.toHex(),
            nonce = nonce,
            grace = grace,
        )
        val dataJson = gson.toJson(data).toByteArray()
        val cipher = Crypto.encrypt(signerPk, userSk, dataJson)
        return TipSignRequest(sig, userPkStr, cipher.base64RawEncode())
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
