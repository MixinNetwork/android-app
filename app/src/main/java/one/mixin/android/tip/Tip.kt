package one.mixin.android.tip

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import one.mixin.android.api.request.TipSignData
import one.mixin.android.api.request.TipSignRequest
import one.mixin.android.api.response.TipSigner
import one.mixin.android.api.service.TipNodeService
import one.mixin.android.crypto.blst.aggregateSignatures
import one.mixin.android.crypto.blst.sign
import one.mixin.android.extension.currentTimeSeconds
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.toHex
import one.mixin.android.extension.toLeByteArray
import one.mixin.android.util.GsonHelper
import timber.log.Timber
import java.security.MessageDigest
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

class Tip @Inject internal constructor(private val tipNodeService: TipNodeService) {
    private val ephemeralGrace = 128.days.inWholeMilliseconds.toString()

    private val gson = GsonHelper.customGson

    suspend fun genPriv(identityPub: String, ephemeral: String): String? {
        val tipConfig = try {
            tipNodeService.tipConfig()
        } catch (e: Exception) {
            Timber.d(e)
            null
        } ?: return null

        val identityPubBytes = identityPub.decodeBase64()
        val ephemeralBytes = ephemeral.decodeBase64()

        val nodeSeeds = getNodeSeeds(tipConfig.signers, identityPubBytes, ephemeralBytes)

        val nodeSigs = getNodeSigs(tipConfig.signers, identityPubBytes, nodeSeeds)

        val aggSig = aggregateSignatures(nodeSigs)
        return aggSig.compress().toHex()
    }

    private fun getNodeSeeds(tipSigners: List<TipSigner>, identityPub: ByteArray, ephemeral: ByteArray): List<ByteArray> {
        val sha256 = MessageDigest.getInstance("SHA256") // or other hash func
        val nodeSeeds = mutableListOf<ByteArray>()
        for (signer in tipSigners) {
            sha256.run {
                update(identityPub)
                update(ephemeral)
                update(signer.identity.toByteArray())
                nodeSeeds.add(digest())
            }
        }
        return nodeSeeds
    }

    private suspend fun getNodeSigs(tipSigners: List<TipSigner>, identityPub: ByteArray, nodeSeeds: List<ByteArray>): List<String> {
        require(tipSigners.size == nodeSeeds.size) { "Required tipSigners size equals nodeSeeds size failed." }
        val result = mutableListOf<String>()
        coroutineScope {
            tipSigners.mapIndexed { i, signer ->
                async(Dispatchers.IO) {
                    var success = false
                    var retryCount = 0

                    while (!success) {
                        val sign = fetchTipNodeSigs(signer, identityPub, nodeSeeds[i])
                        Timber.d("$i sign: $sign")
                        success = sign != null
                        if (success) {
                            require(sign != null)
                            result.add(i, sign)
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

    private suspend fun fetchTipNodeSigs(tipSigner: TipSigner, identityPub: ByteArray, nodeSeed: ByteArray): String? {
        val tipSignRequest = genTipSignRequest(tipSigner, nodeSeed, identityPub)
        return try {
            val r = tipNodeService.postSign(tipSignRequest, tipSigner.api)
            r.signature
        } catch (e: Exception) {
            Timber.d(e)
            null
        }
    }

    private fun genTipSignRequest(tipSigner: TipSigner, nodeSeed: ByteArray, identityPub: ByteArray): TipSignRequest {
        val nonce = currentTimeSeconds()
        val grace = ephemeralGrace
        val sig = genRequestSig(nodeSeed, identityPub, nonce, grace)
        val data = TipSignData(tipSigner.identity, null, nodeSeed.toHex(), grace, nonce.toString(), null)
        val dataJson = gson.toJson(data)
        return TipSignRequest(sig, tipSigner.identity, dataJson)
    }

    private fun genRequestSig(nodeSeed: ByteArray, identityPub: ByteArray, nonce: Long, grace: String): String {
        val m = identityPub + nodeSeed + nonce.toLeByteArray() + grace.toByteArray()
        val sig = sign(m, identityPub)
        return sig.compress().toHex()
    }
}
