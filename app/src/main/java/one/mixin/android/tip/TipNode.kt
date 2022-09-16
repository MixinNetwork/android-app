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
import one.mixin.android.api.request.TipWatchRequest
import one.mixin.android.api.response.TipConfig
import one.mixin.android.api.response.TipSignResponse
import one.mixin.android.api.response.TipSigner
import one.mixin.android.api.service.TipNodeService
import one.mixin.android.crypto.sha3Sum256
import one.mixin.android.extension.base64RawEncode
import one.mixin.android.extension.currentTimeSeconds
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.extension.toBeByteArray
import one.mixin.android.extension.toHex
import one.mixin.android.tip.exception.DifferentIdentityException
import one.mixin.android.tip.exception.NotAllSignerSuccessException
import one.mixin.android.tip.exception.NotEnoughPartialsException
import one.mixin.android.tip.exception.TipNodeException
import one.mixin.android.util.GsonHelper
import retrofit2.HttpException
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

class TipNode @Inject internal constructor(private val tipNodeService: TipNodeService) {
    private val ephemeralGrace = 128.days.inWholeNanoseconds

    private val gson = GsonHelper.customGson

    private val maxRetryCount = 2

    @Throws(TipNodeException::class)
    suspend fun sign(
        identityPriv: ByteArray,
        ephemeral: ByteArray,
        watcher: ByteArray,
        assigneePriv: ByteArray?,
        failedSigners: List<TipSigner>? = null,
        forRecover: Boolean = false,
        callback: Callback? = null,
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

        Timber.d("tip get node sig failedSigners size ${failedSigners?.size}")
        val data = if (!failedSigners.isNullOrEmpty() && assigneeSk != null) {
            // should sign successful signers before failed signers,
            // prevent signing failed signers with different identities.
            val successfulSigners = tipConfig.signers - failedSigners
            val successfulData = getNodeSigs(assigneeSk, successfulSigners, ephemeral, watcher, null, callback)
            if (successfulData.isEmpty() || successfulData.any { it.counter <= 1 }) {
                Timber.w("previously successful signers use different identities")
                throw DifferentIdentityException()
            }

            val failedData = getNodeSigs(userSk, failedSigners, ephemeral, watcher, assignee, callback)
            Timber.d("tip successful data size ${successfulData.size}, failed data size ${failedData.size}")
            failedData + successfulData
        } else {
            getNodeSigs(userSk, tipConfig.signers, ephemeral, watcher, assignee, callback)
        }

        if (!forRecover && data.size < tipConfig.signers.size) {
            Timber.w("not all signer success ${data.size}")
            throw NotAllSignerSuccessException(data.size)
        }

        val (assignor, partials) = parseAssignorAndPartials(data)

        if (partials.size < tipConfig.commitments.size) {
            Timber.d("not enough partials ${partials.size} ${tipConfig.commitments.size}")
            throw NotEnoughPartialsException(partials.size)
        }

        val hexSigs = partials.joinToString(",") { it.toHex() }
        val commitments = tipConfig.commitments.joinToString(",")
        return Crypto.recoverSignature(hexSigs, commitments, assignor, tipConfig.signers.size.toLong())
    }

    suspend fun watch(watcher: ByteArray, callback: Callback? = null): List<TipNodeCounter> {
        val result = CopyOnWriteArrayList<TipNodeCounter>()

        val total = tipConfig.signers.size
        val completeCount = AtomicInteger(0)

        coroutineScope {
            tipConfig.signers.mapIndexed { index, signer ->
                async(Dispatchers.IO) {
                    var retryCount = 0

                    while (retryCount <= maxRetryCount) {
                        val (counter, code) = watchTipNode(signer, watcher)
                        if (code == 500) {
                            Timber.d("watch tip node $index meet $code")
                            return@async
                        }

                        if (counter >= 0) {
                            result.add(TipNodeCounter(counter, signer))

                            val step = completeCount.incrementAndGet()
                            callback?.onNodeComplete(step, total)

                            Timber.d("watch tip node $index success")
                            return@async
                        }

                        retryCount++
                        Timber.d("watch tip node $index failed, retry $retryCount")
                    }
                }
            }.awaitAll()
        }
        return result
    }

    private suspend fun getNodeSigs(userSk: Scalar, tipSigners: List<TipSigner>, ephemeral: ByteArray, watcher: ByteArray, assignee: ByteArray?, callback: Callback?): List<TipSignRespData> {
        val signResult = CopyOnWriteArrayList<TipSignRespData>()
        val nonce = currentTimeSeconds()
        val grace = ephemeralGrace

        val total = tipSigners.size
        val completeCount = AtomicInteger(0)

        coroutineScope {
            tipSigners.mapIndexed { index, signer ->
                async(Dispatchers.IO) {
                    var retryCount = 0
                    while (retryCount <= maxRetryCount) {
                        val (sign, code) = signTipNode(userSk, signer, ephemeral, watcher, nonce, grace, assignee)
                        if (code == 429 || code == 500) {
                            Timber.d("fetch tip node $index meet $code")
                            return@async
                        }

                        if (sign != null) {
                            signResult.add(sign)

                            val step = completeCount.incrementAndGet()
                            callback?.onNodeComplete(step, total)

                            Timber.d("fetch tip node $index sign success")
                            return@async
                        }

                        retryCount++
                        Timber.d("fetch tip node $index failed, retry $retryCount")
                    }
                }
            }.awaitAll()
        }
        return signResult
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

    private suspend fun signTipNode(userSk: Scalar, tipSigner: TipSigner, ephemeral: ByteArray, watcher: ByteArray, nonce: Long, grace: Long, assignee: ByteArray?): Pair<TipSignRespData?, Int> {
        return try {
            val tipSignRequest = genTipSignRequest(userSk, tipSigner, ephemeral, watcher, nonce, grace, assignee)
            val tipSignResponse = tipNodeService.sign(tipSignRequest, tipSigner.api)

            val signerPk = Crypto.pubKeyFromBase58(tipSigner.identity)
            val msg = gson.toJson(tipSignResponse.data).toByteArray()
            try {
                signerPk.verify(msg, tipSignResponse.signature.hexStringToByteArray())
            } catch (e: Exception) {
                Timber.d("verify node response meet ${e.localizedMessage}")
                return Pair(null, -1)
            }

            val data = parseNodeSigResp(userSk, tipSigner, tipSignResponse)
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
        @Suppress("UNUSED_VARIABLE") val nonce = buffer.readLong()
        buffer.write(timeBytes)
        @Suppress("UNUSED_VARIABLE") val time = buffer.readLong()
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
            grace = grace,
        )
        val dataJson = gson.toJson(data).toByteArray()
        val cipher = Crypto.encrypt(signerPk, userSk, dataJson)
        return TipSignRequest(sig, userPkStr, cipher.base64RawEncode(), watcherHex)
    }

    @Suppress("ArrayInDataClass")
    private data class TipSignRespData(val partial: ByteArray, val assignor: String, val counter: Long)

    data class TipNodeCounter(val counter: Int, val tipSigner: TipSigner)

    val tipConfig: TipConfig = gson.fromJson(TipConstants.tipConfig,
        TipConfig::class.java
    )
    val nodeCount = tipConfig.signers.size

    interface Callback {
        fun onNodeComplete(step: Int, total: Int)
    }
}
