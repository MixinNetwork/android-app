package one.mixin.android.tip

import crypto.Crypto
import crypto.Scalar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okio.Buffer
import one.mixin.android.api.request.TipSignData
import one.mixin.android.api.request.TipSignRequest
import one.mixin.android.api.response.TipSignResponse
import one.mixin.android.api.response.TipSigner
import one.mixin.android.api.service.TipNodeService
import one.mixin.android.crypto.sha3Sum256
import one.mixin.android.extension.base64RawEncode
import one.mixin.android.extension.currentTimeSeconds
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.extension.toBeByteArray
import one.mixin.android.extension.toHex
import one.mixin.android.util.GsonHelper
import retrofit2.HttpException
import timber.log.Timber
import kotlin.time.Duration.Companion.days

class TipNode(private val tipNodeService: TipNodeService) {
    private val ephemeralGrace = 128.days.inWholeNanoseconds

    private val gson = GsonHelper.customGson

    suspend fun generatePriv(identityPriv: ByteArray, ephemeral: ByteArray, assigneePriv: ByteArray?): ByteArray? {
        val tipConfig = try {
            tipNodeService.tipConfig()
        } catch (e: Exception) {
            Timber.d(e)
            null
        } ?: return null

        val suite = Crypto.newSuiteBn256()
        val userSk = suite.scalar()
        userSk.setBytes(identityPriv)

        val assignee = if (assigneePriv != null) {
            val assigneeSk = suite.scalar()
            assigneeSk.setBytes(assigneePriv)
            val assigneePub = assigneeSk.publicKey().publicKeyBytes()
            val assigneeSig = assigneeSk.sign(assigneePub)
            assigneePub + assigneeSig
        } else null

        val data = getNodeSigs(userSk, tipConfig.signers, ephemeral, assignee)
        if (data.size < tipConfig.commitments.size) {
            Timber.d("not enough partials ${data.size} ${tipConfig.commitments.size}")
            return null
        }

        val pair = parseAssignorAndPartials(data)
        val hexSigs = pair.second.joinToString(",") { it.toHex() }
        val commitments = tipConfig.commitments.joinToString(",")
        return Crypto.recoverSignature(hexSigs, commitments, pair.first, tipConfig.signers.size.toLong())
    }

    private suspend fun getNodeSigs(userSk: Scalar, tipSigners: List<TipSigner>, ephemeral: ByteArray, assignee: ByteArray?): List<TipSignRespData> {
        val result = mutableListOf<TipSignRespData>()
        val nonce = currentTimeSeconds()
        val grace = ephemeralGrace

        coroutineScope {
            tipSigners.mapIndexed { i, signer ->
                async(Dispatchers.IO) {
                    var success = false
                    var retryCount = 0

                    while (!success) {
                        val pair = fetchTipNodeSigns(userSk, signer, ephemeral, nonce, grace, assignee)
                        if (pair.second == 429 || pair.second == 500) {
                            Timber.d("fetch tip node $i meet ${pair.second}")
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

    private suspend fun fetchTipNodeSigns(userSk: Scalar, tipSigner: TipSigner, ephemeral: ByteArray, nonce: Long, grace: Long, assignee: ByteArray?): Pair<TipSignRespData?, Int> {
        val tipSignRequest = genTipSignRequest(userSk, tipSigner, ephemeral, nonce, grace, assignee)
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

            val data = parseNodeSigResp(userSk, tipSigner, r)
            Pair(data, -1)
        } catch (e: Exception) {
            Timber.d(e)
            if (e is HttpException) {
                Pair(null, e.code())
            } else {
                Pair(null, -1)
            }
        }
    }

    private fun parseAssignorAndPartials(data: List<TipSignRespData>): Pair<ByteArray, List<ByteArray>> {
        val acm = mutableMapOf<String, Int>()
        val partials = mutableListOf<ByteArray>()
        data.forEach { d ->
            acm[d.assignor] = (acm[d.assignor] ?: 0) + 1
            partials.add(d.partial)
        }
        var amc = 0
        var assignor = data.first().assignor.hexStringToByteArray()
        acm.forEach { (a, c) ->
            if(c > amc) {
                assignor = a.hexStringToByteArray()
                amc = c
            }
        }
        Timber.d("amc: $amc, assignor ${assignor.toHex()}")
        return Pair(assignor, partials)
    }

    private fun parseNodeSigResp(userSk: Scalar, signer: TipSigner, resp: TipSignResponse): TipSignRespData {
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
        @Suppress("UNUSED_VARIABLE") val nonce = buffer.readLong()
        buffer.write(timeBytes)
        @Suppress("UNUSED_VARIABLE") val time = buffer.readLong()
        buffer.write(counterBytes)
        val counter = buffer.readLong()
        return TipSignRespData(partial, assignor.toHex(), counter)
    }

    private fun genTipSignRequest(userSk: Scalar, tipSigner: TipSigner, ephemeral: ByteArray, nonce: Long, grace: Long, assignee: ByteArray?): TipSignRequest {
        val signerPk = Crypto.pubKeyFromBase58(tipSigner.identity)
        val userPk = userSk.publicKey()
        val esum = (ephemeral + tipSigner.identity.toByteArray()).sha3Sum256()
        var msg = userPk.publicKeyBytes() + esum + nonce.toBeByteArray() + grace.toBeByteArray()
        if (assignee != null) {
            msg += assignee
        }
        val sig = userSk.sign(msg).toHex()

        val userPkStr = userPk.publicKeyString()
        val data = TipSignData(
            identity = userPkStr,
            assignee = assignee?.toHex(),
            ephemeral = esum.toHex(),
            nonce = nonce,
            grace = grace,
        )
        val dataJson = gson.toJson(data).toByteArray()
        val cipher = Crypto.encrypt(signerPk, userSk, dataJson)
        return TipSignRequest(sig, userPkStr, cipher.base64RawEncode())
    }

    @Suppress("ArrayInDataClass")
    private data class TipSignRespData(val partial: ByteArray, val assignor: String, val counter: Long)
}
