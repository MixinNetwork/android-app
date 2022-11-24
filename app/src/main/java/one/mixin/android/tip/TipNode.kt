package one.mixin.android.tip

import com.google.gson.Gson
import crypto.Crypto
import crypto.Scalar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okio.Buffer
import one.mixin.android.api.request.TipSignData
import one.mixin.android.api.request.TipSignRequest
import one.mixin.android.api.request.TipWatchRequest
import one.mixin.android.api.response.TipConfig
import one.mixin.android.api.response.TipSignResponse
import one.mixin.android.api.response.TipSigner
import one.mixin.android.api.service.TipNodeService
import one.mixin.android.crypto.sha3Sum256
import one.mixin.android.extension.base64RawURLEncode
import one.mixin.android.extension.currentTimeSeconds
import one.mixin.android.extension.getStackTraceString
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.extension.toBeByteArray
import one.mixin.android.extension.toHex
import one.mixin.android.tip.exception.DifferentIdentityException
import one.mixin.android.tip.exception.NotAllSignerSuccessException
import one.mixin.android.tip.exception.NotEnoughPartialsException
import one.mixin.android.tip.exception.TipNodeException
import retrofit2.HttpException
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

class TipNode @Inject internal constructor(private val tipNodeService: TipNodeService, private val tipConfig: TipConfig, private val gson: Gson) {
    private val ephemeralGrace = 128.days.inWholeNanoseconds

    private val maxRequestCount = 3

    val nodeCount: Int
        get() {
            return tipConfig.signers.size
        }

    @Throws(TipNodeException::class)
    suspend fun sign(
        identityPriv: ByteArray,
        ephemeral: ByteArray,
        watcher: ByteArray,
        assigneePriv: ByteArray?,
        failedSigners: List<TipSigner>? = null,
        forRecover: Boolean = false,
        callback: Callback? = null
    ): ByteArray {
        val suite = Crypto.newSuiteBn256()
        val userSk = suite.scalar()
        userSk.setBytes(identityPriv)

        var assigneeSk: Scalar? = null
        var assignee: ByteArray? = null
        if (assigneePriv != null) {
            assigneeSk = suite.scalar()
            assigneeSk.setBytes(assigneePriv)
            val assigneePub = assigneeSk.publicKey().publicKeyBytes()
            val assigneeSig = assigneeSk.sign(assigneePub)
            assignee = assigneePub + assigneeSig
        }

        Timber.e("tip get node sig failedSigners size ${failedSigners?.size}, assigneeSk != null: ${assigneeSk != null}")
        val pair = if (!failedSigners.isNullOrEmpty() && assigneeSk != null) {
            // should sign successful signers before failed signers,
            // prevent signing failed signers with different identities.
            val successfulSigners = tipConfig.signers - failedSigners
            val successfulPair = getNodeSigs(assigneeSk, successfulSigners, ephemeral, watcher, null, callback)
            val successfulData = successfulPair.first
            if (successfulData.isEmpty() || successfulData.any { it.counter <= 1 }) {
                Timber.e("previously successful signers use different identities")
                throw DifferentIdentityException()
            }

            val failedPair = getNodeSigs(userSk, failedSigners, ephemeral, watcher, assignee, callback)
            val failedData = failedPair.first
            Timber.e("tip successful data size ${successfulData.size}, failed data size ${failedData.size}")
            val compositionError = listOfNotNull(successfulPair.second, failedPair.second).getRepresentative()
            Pair(failedData + successfulData, compositionError)
        } else {
            getNodeSigs(userSk, tipConfig.signers, ephemeral, watcher, assignee, callback)
        }

        val data = pair.first
        val tipNodeError = pair.second

        if (!forRecover && data.size < nodeCount) {
            Timber.e("not all signer success ${data.size}")
            throw NotAllSignerSuccessException("", data.size, tipNodeError)
        }

        if (data.size < tipConfig.commitments.size) {
            Timber.e("not all signer success ${data.size}")
            throw NotAllSignerSuccessException("", data.size, tipNodeError)
        }

        val (assignor, partials) = parseAssignorAndPartials(data)
        Timber.e("after parseAssignorAndPartials")

        if (partials.size < tipConfig.commitments.size) {
            Timber.e("not enough partials ${partials.size} ${tipConfig.commitments.size}")
            throw NotEnoughPartialsException(partials.size, tipNodeError)
        }

        val hexSigs = partials.joinToString(",") { it.toHex() }
        val commitments = tipConfig.commitments.joinToString(",")
        return Crypto.recoverSignature(hexSigs, commitments, assignor, nodeCount.toLong())
    }

    suspend fun watch(watcher: ByteArray, callback: Callback? = null): Pair<List<TipNodeCounter>, String> {
        val result = CopyOnWriteArrayList<TipNodeCounter>()
        val nodeFailedInfo = StringBuffer()

        val total = nodeCount
        val completeCount = AtomicInteger(0)

        coroutineScope {
            tipConfig.signers.mapIndexed { _, signer ->
                async(Dispatchers.IO) {
                    var retryCount = 0

                    while (retryCount < maxRequestCount) {
                        val (counter, code) = watchTipNode(signer, watcher)
                        if (code == 429 || code == 500) {
                            Timber.e("watch tip node failed, ${signer.index} ${signer.api} meet $code")
                            nodeFailedInfo.append("[${signer.index}, $code] ")

                            if (code == 429) {
                                return@async
                            }
                        }

                        if (counter >= 0) {
                            result.add(TipNodeCounter(counter, signer))

                            val step = completeCount.incrementAndGet()
                            callback?.onNodeComplete(step, total)

                            Timber.e("watch tip node ${signer.index} success")
                            return@async
                        }

                        retryCount++
                        Timber.e("watch tip node ${signer.index} failed, retry $retryCount")
                    }
                }
            }.awaitAll()
        }
        return Pair(result, nodeFailedInfo.toString())
    }

    private suspend fun getNodeSigs(userSk: Scalar, tipSigners: List<TipSigner>, ephemeral: ByteArray, watcher: ByteArray, assignee: ByteArray?, callback: Callback?): Pair<List<TipSignRespData>, TipNodeError?> {
        val signResult = CopyOnWriteArrayList<TipSignRespData>()
        val nodeErrorList = CopyOnWriteArrayList<TipNodeError>()
        val nodeFailedInfo = StringBuffer()
        val nonce = currentTimeSeconds()
        val grace = ephemeralGrace

        val total = tipSigners.size
        val completeCount = AtomicInteger(0)

        coroutineScope {
            tipSigners.mapIndexed { _, signer ->
                async(Dispatchers.IO) {
                    var retryCount = 0
                    while (retryCount < maxRequestCount) {
                        val (sign, tipNodeError) = signTipNode(userSk, signer, ephemeral, watcher, nonce + retryCount, grace, assignee)
                        if (tipNodeError != null) {
                            val errorMessage = "sign tip node failed, ${signer.index} ${signer.api} meet $tipNodeError"
                            nodeFailedInfo.append("[${signer.index}, ${tipNodeError.code}] ")
                            nodeErrorList.add(tipNodeError)
                            Timber.e(errorMessage)

                            if (tipNodeError.notRetry()) {
                                return@async
                            }
                        }

                        if (sign != null) {
                            signResult.add(sign)
                            val step = completeCount.incrementAndGet()
                            callback?.onNodeComplete(step, total)
                            Timber.e("sign tip node ${signer.index} sign success")
                            return@async
                        }
                        retryCount += 1
                        Timber.e("sign tip node ${signer.index} failed, retry $retryCount")
                    }
                }
            }.awaitAll()
        }

        callback?.onNodeFailed(nodeFailedInfo.toString())

        return Pair(signResult, nodeErrorList.getRepresentative())
    }

    private suspend fun watchTipNode(tipSigner: TipSigner, watcher: ByteArray): Pair<Int, Int> {
        val tipWatchRequest = TipWatchRequest(watcher.toHex())
        return try {
            val tipWatchResponse = tipNodeService.watch(tipWatchRequest, tipSigner.api)
            return Pair(tipWatchResponse.counter, -1)
        } catch (e: Exception) {
            Timber.d(e)
            if (e is HttpException) {
                Pair(-1, e.code())
            } else {
                Pair(-1, -1)
            }
        }
    }

    private suspend fun signTipNode(userSk: Scalar, tipSigner: TipSigner, ephemeral: ByteArray, watcher: ByteArray, nonce: Long, grace: Long, assignee: ByteArray?): Pair<TipSignRespData?, TipNodeError?> {
        return try {
            val tipSignRequest = genTipSignRequest(userSk, tipSigner, ephemeral, watcher, nonce, grace, assignee)
            val response = tipNodeService.sign(tipSignRequest, tipSigner.api)
            val requestId = response.headers()["x-request-id"] ?: ""
            if (response.isSuccessful.not()) {
                return Pair(null, response.code().toTipNodeError(tipSigner.index, requestId, response.message()))
            }
            val tipSignResponse = requireNotNull(response.body()) {
                "sign tipNode response success but body is null"
            }
            val resError = tipSignResponse.error
            if (resError != null) {
                return Pair(null, resError.code.toTipNodeError(tipSigner.index, requestId, resError.description))
            }
            val signerPk = Crypto.pubKeyFromBase58(tipSigner.identity)
            val msg = gson.toJson(tipSignResponse.data).toByteArray()
            try {
                signerPk.verify(msg, tipSignResponse.signature.hexStringToByteArray())
            } catch (e: Exception) {
                Timber.e("verify node response meet ${e.getStackTraceString()}")
                return Pair(null, null)
            }

            val data = parseNodeSigResp(userSk, tipSigner, tipSignResponse)
            Pair(data, null)
        } catch (e: Exception) {
            Timber.d(e)
            if (e is HttpException) {
                Pair(null, e.code().toTipNodeError(tipSigner.index, "", e.message))
            } else {
                Pair(null, null)
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
            if (c > amc) {
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
        @Suppress("UNUSED_VARIABLE")
        val nonce = buffer.readLong()
        buffer.write(timeBytes)
        @Suppress("UNUSED_VARIABLE")
        val time = buffer.readLong()
        buffer.write(counterBytes)
        val counter = buffer.readLong()
        Timber.d("tip sign node ${signer.index} counter $counter")
        return TipSignRespData(partial, assignor.toHex(), counter)
    }

    private fun genTipSignRequest(userSk: Scalar, tipSigner: TipSigner, ephemeral: ByteArray, watcher: ByteArray, nonce: Long, grace: Long, assignee: ByteArray?): TipSignRequest {
        val signerPk = Crypto.pubKeyFromBase58(tipSigner.identity)
        val userPk = userSk.publicKey()
        val esum = (ephemeral + tipSigner.identity.toByteArray()).sha3Sum256()
        var msg = userPk.publicKeyBytes() + esum + nonce.toBeByteArray() + grace.toBeByteArray()
        if (assignee != null) {
            msg += assignee
        }
        val sig = userSk.sign(msg).toHex()

        val userPkStr = userPk.publicKeyString()
        val watcherHex = watcher.toHex()
        val data = TipSignData(
            identity = userPkStr,
            assignee = assignee?.toHex(),
            ephemeral = esum.toHex(),
            watcher = watcherHex,
            nonce = nonce,
            grace = grace
        )
        val dataJson = gson.toJson(data).toByteArray()
        val cipher = Crypto.encrypt(signerPk, userSk, dataJson)
        return TipSignRequest(sig, userPkStr, cipher.base64RawURLEncode(), watcherHex)
    }

    @Suppress("ArrayInDataClass")
    private data class TipSignRespData(val partial: ByteArray, val assignor: String, val counter: Long)

    data class TipNodeCounter(val counter: Int, val tipSigner: TipSigner)

    interface Callback {
        fun onNodeComplete(step: Int, total: Int)
        fun onNodeFailed(info: String)
    }
}
