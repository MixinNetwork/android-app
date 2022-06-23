package one.mixin.android.tip

import crypto.Crypto
import crypto.Scalar
import crypto.SuiteBn256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import one.mixin.android.api.request.TipSignData
import one.mixin.android.api.request.TipSignRequest
import one.mixin.android.api.response.TipSignResponse
import one.mixin.android.api.response.TipSigner
import one.mixin.android.api.service.TipNodeService
import one.mixin.android.crypto.sha3Sum256
import one.mixin.android.extension.base64RawEncode
import one.mixin.android.extension.base64RawUrlDecode
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.extension.toBeByteArray
import one.mixin.android.extension.toHex
import one.mixin.android.util.GsonHelper
import retrofit2.HttpException
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

class Tip @Inject internal constructor(private val tipNodeService: TipNodeService) {
    private val ephemeralGrace = 128.days.inWholeNanoseconds

    private val gson = GsonHelper.customGson

    suspend fun genPriv(identityPub: String, ephemeral: String): String? {
        val tipConfig = try {
            tipNodeService.tipConfig()
        } catch (e: Exception) {
            Timber.d(e)
            null
        } ?: return null

        Timber.d("identityPub: $identityPub")
        Timber.d("ephemeral: $ephemeral")
        Timber.d("nonce ${1024L.toBeByteArray().toHex()}")

        val identityPubBytes = identityPub.base64RawUrlDecode()
        val ephemeralBytes = ephemeral.base64RawUrlDecode()

        val suite = Crypto.newSuiteBn256()
        val userSk = suite.scalar()
        userSk.setBytes(identityPubBytes)
        Timber.d("user pk ${userSk.publicKey().publicKeyBytes().toHex()}")

        val nodeSeeds = getNodeSeeds(tipConfig.signers, identityPubBytes, ephemeralBytes)

        val nodeSigs = getNodeSigs(suite, userSk, tipConfig.signers, nodeSeeds)

        val hexSigs = nodeSigs.joinToString(",") { it.toHex() }
        Timber.d("hexSigs $hexSigs")
        val aggSig = Crypto.aggregateSignatures(hexSigs)
        Timber.d("aggSig: ${aggSig.toHex()}")
        return aggSig.toHex()
    }

    private fun getNodeSeeds(tipSigners: List<TipSigner>, identityPub: ByteArray, ephemeral: ByteArray): List<ByteArray> {
        val nodeSeeds = mutableListOf<ByteArray>()
        for (signer in tipSigners) {
            nodeSeeds.add((identityPub + ephemeral + signer.identity.toByteArray()).sha3Sum256())
        }
        return nodeSeeds
    }

    @Suppress("ArrayInDataClass")
    private data class SigIndex(val sig: ByteArray, val index: Int)

    private suspend fun getNodeSigs(suite: SuiteBn256, userSk: Scalar, tipSigners: List<TipSigner>, nodeSeeds: List<ByteArray>): List<ByteArray> {
        require(tipSigners.size == nodeSeeds.size) { "Required tipSigners size equals nodeSeeds size failed." }
        val sigIndices = mutableListOf<SigIndex>()
        coroutineScope {
            tipSigners.mapIndexed { i, signer ->
                async(Dispatchers.IO) {
                    var success = false
                    var retryCount = 0

                    while (!success) {
                        val pair = fetchTipNodeSigns(suite, userSk, signer, nodeSeeds[i])
                        if (pair.second == 429) {
                            return@async
                        }

                        val sign = pair.first
                        success = sign != null
                        if (success) {
                            require(sign != null)
                            sigIndices.add(SigIndex(sign, signer.index))
                            Timber.d("fetch tip node $i sign success")
                            return@async
                        }

                        retryCount++
                        Timber.d("fetch tip node $i failed, retry $retryCount")
                    }
                }
            }.awaitAll()
        }
        val result = mutableListOf<ByteArray>()
        sigIndices.sortedBy { it.index }
            .mapTo(result) { it.sig }
        return result
    }

    private suspend fun fetchTipNodeSigns(suite: SuiteBn256, userSk: Scalar, tipSigner: TipSigner, nodeSeed: ByteArray): Pair<ByteArray?, Int> {
        val tipSignRequest = genTipSignRequest(suite, userSk, tipSigner, nodeSeed)
        return try {
            val r = tipNodeService.postSign(tipSignRequest, tipSigner.api)
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
        Timber.d("plain len: ${plain.size}")
        val nonce = plain.slice(0..7).toByteArray()
        var offset = 10  // skip 2 bytes, confuse
        val partial = plain.slice(offset..offset + 63).toByteArray()
        offset += 64
        val assignor = plain.slice(offset..offset + 127).toByteArray()
        offset += 128
        val time = plain.slice(offset..offset + 7).toByteArray()
        offset += 8
        val count = plain.slice(offset..offset + 7).toByteArray()
        Timber.d("nonce: ${nonce.toHex()}, partial: ${partial.toHex()}, assignor: ${assignor.toHex()}, time: ${time.toHex()}, count: ${count.toHex()}")
        return partial
    }

    private fun genTipSignRequest(suite: SuiteBn256, userSk: Scalar, tipSigner: TipSigner, nodeSeed: ByteArray): TipSignRequest {
        val signerPk = Crypto.pubKeyFromBase58(tipSigner.identity)
        val ephemeral = suite.scalar()
        ephemeral.setBytes(nodeSeed)
        val eBytes = ephemeral.privateKeyBytes()

        val nonce = 1024L
        val grace = ephemeralGrace

        val sig = genRequestSig(userSk, eBytes, nonce, grace)
        Timber.d("sig: $sig")

        val userPkStr = userSk.publicKey().publicKeyString()
        val data = TipSignData(
            identity = userPkStr,
            ephemeral = eBytes.toHex(),
            nonce = nonce,
            grace = grace,
        )
        val dataJson = gson.toJson(data).toByteArray()
        Timber.d("dataJson: ${dataJson.toHex()}")
        val cipher = Crypto.encrypt(signerPk, userSk, dataJson)
        Timber.d("cipher: ${cipher.toHex()}")
        return TipSignRequest(sig, userPkStr, cipher.base64RawEncode())
    }

    private fun genRequestSig(user: Scalar, ephemeral: ByteArray, nonce: Long, grace: Long): String {
        val uPk = user.publicKey().publicKeyBytes()
        val m = uPk + ephemeral + nonce.toBeByteArray() + grace.toBeByteArray()
        Timber.d("m: ${m.toHex()}")
        val sig = user.sign(m)
        return sig.toHex()
    }
}
