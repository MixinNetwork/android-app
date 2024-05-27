package org.sol4k

import okio.Buffer
import one.mixin.android.api.response.solanaNativeTokenAssetKey
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Base64
import kotlin.math.max

class VersionedTransaction(
    val message: Message,
    val signatures: MutableList<String>,
) {

    fun sign(keypair: Keypair) {
        val data = message.serialize()
        val signature = keypair.sign(data)
        for (i in 0 until message.header.numRequireSignatures) {
            val a = message.accounts[i]
            if (a.verify(signature, data)) {
                if (signatures.isEmpty()) {
                    signatures.add(Base58.encode(signature))
                } else {
                    signatures[i] = Base58.encode(signature)
                }
                break
            }
        }
    }

    fun serialize(): ByteArray {
        if (signatures.isEmpty() || signatures.size != message.header.numRequireSignatures) {
            throw Exception("Signature verification failed")
        }

        val messageData = message.serialize()

        val b = Buffer()
        b.write(Binary.encodeLength(signatures.size))
        for (s in signatures) {
            b.write(Base58.decode(s))
        }
        b.write(messageData)
        return b.readByteArray()
    }

    fun calcFee(): BigDecimal {
        val sigFee = lamportToSol(BigDecimal(5000 * max(signatures.size, 1)))
        val accounts = message.accounts
        val data = mutableListOf<ByteArray>()
        for (i in message.instructions) {
            if (accounts[i.programIdIndex] != Constants.COMPUTE_BUDGET__PROGRAM_ID) {
                continue
            }
            data.add(i.data)
        }
        val msgFee = computeBudget(data)
        return sigFee.add(msgFee).setScale(9, RoundingMode.CEILING)
    }

    // TODO uncertainty logic
    fun calcBalanceChange(): TokenBalanceChange {
        val accounts = message.accounts
        for (i in message.instructions) {
            if (accounts[i.programIdIndex] == Constants.SYSTEM_PROGRAM) {
                val lamports = parseSystemProgramData(i.data) ?: continue
                return TokenBalanceChange(lamports, solanaNativeTokenAssetKey)
            } else if (accounts[i.programIdIndex] == Constants.TOKEN_PROGRAM_ID) {
                val lamports = parseTokenProgramData(i.data) ?: continue
                val mintAddress = accounts[i.accounts[1]].toBase58()
                return TokenBalanceChange(lamports, mintAddress)
            } else {
                // TODO parse inner instruction
//                if(i.accounts.find { accounts[it] == Constants.SYSTEM_PROGRAM } != null) {
//
//                } else if (i.accounts.find { accounts[it] == Constants.TOKEN_PROGRAM_ID } != null) {
//
//                }
            }
        }
        return TokenBalanceChange(0L, "")
    }

    private fun parseSystemProgramData(data: ByteArray): Long? {
        val d = Buffer()
        d.write(data)
        val instruction = d.readIntLe()
        if (instruction != 2) {
            return null
        }
        return  d.readLongLe()
    }

    private fun parseTokenProgramData(data: ByteArray): Long? {
        val d = Buffer()
        d.write(data)
        val instruction = d.readByte().toInt()
        if (instruction != 12) {
            return null
        }
        return d.readLongLe()
    }

    data class TokenBalanceChange(
        var change: Long,
        val mint: String,
    )

    companion object {
        const val PUBLIC_KEY_LENGTH = 32
        private const val SIGNATURE_LENGTH = 64

        @JvmStatic
        fun from(encodedTransaction: String): VersionedTransaction {
            var byteArray = Base64.getDecoder().decode(encodedTransaction)
            val signaturesCount = Binary.decodeLength(byteArray)
            byteArray = signaturesCount.second
            val signatures = mutableListOf<String>()
            for (i in 0 until signaturesCount.first) {
                val signature = byteArray.slice(0 until SIGNATURE_LENGTH)
                byteArray = byteArray.drop(SIGNATURE_LENGTH).toByteArray()
                val encodedSignature = Base58.encode(signature.toByteArray())
                signatures.add(encodedSignature)
            }

            val message = Message.deserialize(byteArray)

            if(signaturesCount.first > 0 && message.header.numRequireSignatures != signaturesCount.first) {
                throw Exception("numRequireSignatures is not equal to signatureCount")
            }
            return VersionedTransaction(message, signatures)
        }
    }
}