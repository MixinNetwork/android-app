package one.mixin.android.tip

import crypto.Crypto
import crypto.Scalar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import one.mixin.android.api.request.TipSignData
import one.mixin.android.api.request.TipSignRequest
import one.mixin.android.api.response.TipSigner
import one.mixin.android.api.service.TipNodeService
import one.mixin.android.crypto.sha3Sum256
import one.mixin.android.extension.base64RawEncode
import one.mixin.android.extension.base64RawUrlDecode
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
        val identityPubBytes = identityPub.base64RawUrlDecode()
        val ephemeralBytes = ephemeral.base64RawUrlDecode()

        val nodeSeeds = getNodeSeeds(tipConfig.signers, identityPubBytes, ephemeralBytes)

        val nodeSigs = getNodeSigs(tipConfig.signers, identityPubBytes, nodeSeeds)
        Timber.d("nodeSigs: $nodeSigs")

        // val aggSig = aggregateSignatures(nodeSigs)
        // return aggSig.compress().toHex()
        return null
    }

    private fun getNodeSeeds(tipSigners: List<TipSigner>, identityPub: ByteArray, ephemeral: ByteArray): List<ByteArray> {
        val nodeSeeds = mutableListOf<ByteArray>()
        for (signer in tipSigners) {
            nodeSeeds.add((identityPub + ephemeral + signer.identity.toByteArray()).sha3Sum256())
        }
        return nodeSeeds
    }

    private class SigIndex(val sig: String, val index: Int)

    private suspend fun getNodeSigs(tipSigners: List<TipSigner>, identityPub: ByteArray, nodeSeeds: List<ByteArray>): List<String> {
        require(tipSigners.size == nodeSeeds.size) { "Required tipSigners size equals nodeSeeds size failed." }
        val sigIndices = mutableListOf<SigIndex>()
        coroutineScope {
            tipSigners.mapIndexed { i, signer ->
                async(Dispatchers.IO) {
                    var success = false
                    var retryCount = 0

                    while (!success) {
                        val pair = fetchTipNodeSigns(signer, identityPub, nodeSeeds[i])
                        if (pair.second == 429) {
                            return@async
                        }

                        val sign = pair.first
                        Timber.d("$i sign: $sign")
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
        val result = mutableListOf<String>()
        sigIndices.sortedBy { it.index }
            .mapTo(result) { it.sig }
        return result
    }

    private suspend fun fetchTipNodeSigns(tipSigner: TipSigner, identityPub: ByteArray, nodeSeed: ByteArray): Pair<String?, Int> {
        val tipSignRequest = genTipSignRequest(tipSigner, nodeSeed, identityPub)
        return try {
            val r = tipNodeService.postSign(tipSignRequest, tipSigner.api)
            Pair(r.signature, -1)
        } catch (e: Exception) {
            Timber.d(e)
            if (e is HttpException) {
                Pair(null, e.code())
            } else {
                Pair(null, -1)
            }
        }
    }

    private fun genTipSignRequest(tipSigner: TipSigner, nodeSeed: ByteArray, identityPub: ByteArray): TipSignRequest {
        val suite = Crypto.newSuiteBn256()
        val signerPk = Crypto.pubKeyFromBase58(tipSigner.identity)
        val user = suite.scalar()
        user.setBytes(identityPub)
        val uPkStr = user.publicKey().publicKeyString()
        Timber.d("uPKStr: $uPkStr")

        val ephemeral = suite.scalar()
        ephemeral.setBytes(nodeSeed)
        val eBytes = ephemeral.privateKeyBytes()

        val nonce = 1024L
        val grace = ephemeralGrace

        val sig = genRequestSig(user, eBytes, nonce, grace)
        Timber.d("sig: $sig")

        val data = TipSignData(
            identity = uPkStr,
            ephemeral = eBytes.toHex(),
            nonce = nonce,
            grace = grace,
        )
        val dataJson = gson.toJson(data).toByteArray()
        Timber.d("dataJson: ${dataJson.toHex()}")
        val cipher = Crypto.encrypt(signerPk, user, dataJson)
        Timber.d("cipher: ${cipher.toHex()}")
        return TipSignRequest(sig, uPkStr, cipher.base64RawEncode())
    }

    private fun genRequestSig(user: Scalar, ephemeral: ByteArray, nonce: Long, grace: Long): String {
        val uPk = user.publicKey().publicKeyBytes()
        val m = uPk + ephemeral + nonce.toBeByteArray() + grace.toBeByteArray()
        Timber.d("m: ${m.toHex()}")
        val sig = user.sign(m)
        return sig.toHex()
    }
}
