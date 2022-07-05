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
import one.mixin.android.util.GsonHelper
import retrofit2.HttpException
import timber.log.Timber
import kotlin.time.Duration.Companion.days

class TipNode(private val tipNodeService: TipNodeService) {
    private val ephemeralGrace = 128.days.inWholeNanoseconds

    private val gson = GsonHelper.customGson

    suspend fun generatePriv(identityPriv: ByteArray, ephemeral: ByteArray, assigneePriv: ByteArray?): ByteArray? {
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

    private val tipConfig = gson.fromJson("""
        {"commitments":["5HYSNEcudZqucSo8tjVkkkuz6QiTQPSCjSxdGY9gZ1V3JKGtJ5bfZXS3QbB8AnBPLVrJQLEyJaucw4S6MmJhkwTpqCpTiovjiiaab4PUZsS8NBUBFharae9R3QUMR9ouPTgFxqmqMGGXSAqrRziSqneTcEkgLymx5oahxGfSeovgTN1FDgKiAt","5Jr6mXiEF1xtR7jJ8o2923vsGcqnjnegK9eVybDfXCnFXKhRVaJDCMHvGuHLo92TWSuBVrwDxYM8nHDiqYVb7csPQHHTyPjrGMPZiPe3PFhBYpz5nsDeVxjknZ8C9fPYYi67qyBy4fy1T6U4itXQEzjCTBdtjw2TXrNKe5oYPvJWx3X6ZpygCi","5JTnSpeBG9NyBAEkXL6KxFW3fYMhM4rgBqLMXynrBxHcshuQViedG2H4UumcdLZzMjkyyAdsGmEAf4MKiULmN93aaNvqCXiHH7MYnyvGp96s9bXs2M61HgH1KKZ4Tvk6yBfnVmvwmGVnnsQMd7fTky37VZGqn56SkQa7ANEz52Bwfs7uBBVZzV","5JDXsF4qPcf9cwKuAL2H4scnaMEz3GYnyM3koVx5AE7rDvaLCeqWZ8ksXES9eQooKtfQHZyxhhFZumrQqkMqsbxatYSvZJFSWtTESoBqGSqb9F2pvQboik1uJyw7VNrDFUkvVj64JXY6cThHvWQpK96zqELurNjfEPjNRB79c5ESqqfK4FXtce","5Jr8U6gPzoi9iYLRY4bVJBbYfsk95xM7AUyMhrsXzfG16nzkzcR2LpEsPFeYwS75WCDMGVta55SDWjb1cRPmWVKrKvr9A8RKRJkF3yop9gc2Kia4bccYqH1QcnNZAGoDLcqHiLSgbWwvjb1NaS2vVtUHsfnbKAQ64jX96gSMJis39UEh9HJiiA"],"signers":[{"identity":"5HXAHFh8khYBGWA2V3oUUvXAX4aWnQsEyNzKoA3LnJkxtKQNhcWSh4Swt72a1bw7aG8uTg9F31ybzSJyuNHENUBtGobUfHbKNPUYYkHnhuPtWszaCuNJ3nBxZ4Crt8Q8AmJ2fZznLx3EDM2Eqf63drNmW6VVmmzBQUc4N2JaXzFtt4HFFWtvUk","index":0,"api":"https://mercury.tip.id"},{"identity":"5HrtnCWMLkh2R82iwztEorvRhdZkZwhE77ohPekJKumbko2gZ4RJ4HDiP9VRp1ZvjJi1CR7UB5WCxPGwcQS5oapXb2gtC7X37YhJ1TonFMfpiMy1a8w4VbrHsva3HyLeukUNvQ9vwn8eShRkqGXDhs83GGPcMjvpMWE7BrQuowCuzh5Rh1A3Zm","index":1,"api":"https://saturn.tip.id"},{"identity":"5JPtzVXJB5qPf4hZ8PgMFDyrqpjhUkYFSeLyME8mc61z7RErZxKj9To7CBhCpFPtt6LyfPH8D6knWjA8LgcAUjjS1EjbPzGNJ8GWFMP5ZtTh1HLgtLEnT5eJXvPHataxruYTmXMmuxZZiMKWvXf9crHggZBLPTaAxfgiis3JdwUUXYXhN91vi3","index":2,"api":"https://jupiter.tip.id"},{"identity":"5JZegBrWEedonzKwhWYhKjVuDd2ADWnFnnGWp7yjfAJCQSwwSFU7K22hwFeGtyHy9r8Zn7M5R6rX7WMYDQQupKP2NbqxsfSggxj3YrjC5msA4TdWavdLjFFPjNMdcCQHhUQhexkvxenDwjBgBFtsnHMMbSUTPk4nDQEjiF4J1ihZ6bwY3trTVD","index":3,"api":"https://mars.tip.id"},{"identity":"5JaU587MEXez1onCX7RkRyNkswfpdQ979Nsyv6niBFgUKJQmRTmoCy8phdUhMtwfzoJ8fu3zt8Mae1VVoTM5iNL4gP9zWkT8JscsNJSG5vnT6soM511V8exwmgUeXDfugNshJjqaskhyAAih4FfLxtV3NcBZk1zMDxnctwGQaM7z1G2L9Tf9H1","index":4,"api":"https://moon.tip.id"},{"identity":"5JaUby8CxXdEhYcEH2VEbkhdj6zDd1fj3wGdyTjiRQtres4yVoLhkHMf8wZ4qsNMhLvJgk9Mgae1Rs9cm6rY41yCTEzQQRA3a3D8FDDdv4dQms735noeqgxxzZs15utTnu7S5XFDiTUcMhue1Re7DZvvATKFpCbHzwoymU9yhXZBxsYCaHbULa","index":5,"api":"https://venus.tip.id"},{"identity":"5JrHfpJ6ML88u3nbvsB4dej7aHXdvShTEfxNF3mZ8no8wxsdhLTP4sh2Kt6AKboCcBRWGkFPky85e6DkMZsT3WSZ1q8V7gNF4Bdyipw7aS7TP8vtgG7USRJhVe36gNa8LJktf2a2chsWRT6egeB59wEyCHmgiNZpZSPwaezwS32ADTgCSDkzKY","index":6,"api":"https://sun.tip.id"}]}
    """, TipConfig::class.java)
}
