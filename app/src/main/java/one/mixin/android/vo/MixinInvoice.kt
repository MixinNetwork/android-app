package one.mixin.android.vo

import androidx.compose.ui.unit.Constraints
import com.google.android.gms.common.internal.service.Common
import one.mixin.android.Constants
import one.mixin.android.crypto.sha3Sum256
import one.mixin.android.extension.base64RawURLDecode
import one.mixin.android.extension.base64RawURLEncode
import one.mixin.android.extension.hexString
import one.mixin.android.util.UUIDUtils
import timber.log.Timber
import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.text.toBigInteger

const val MixinInvoicePrefix = "MIN"

const val MIXIN_INVOICE_VERSION: Byte = 0
const val REFERENCES_COUNT_LIMIT = 2
const val EXTRA_SIZE_STORAGE_CAPACITY = 512
const val EXTRA_SIZE_GENERAL_LIMIT = 256
const val EXTRA_SIZE_STORAGE_STEP = 128
const val EXTRA_STORAGE_PRICE_STEP = "100000"

data class MixinInvoice(
    val version: Byte = MIXIN_INVOICE_VERSION,
    val recipient: MixAddress,
    val entries: MutableList<InvoiceEntry> = mutableListOf(),
) {
    fun addEntry(
        traceId: String,
        assetId: String,
        amount: String,
        extra: ByteArray = byteArrayOf(),
        indexReferences: ByteArray = byteArrayOf(),
        hashReferences: List<ByteArray> = emptyList(),
    ) {
        val entry = InvoiceEntry(
            traceId = traceId,
            assetId = assetId,
            amount = amount.toBigInteger(),
            extra = extra,
            indexReferences = indexReferences.toList(),
            hashReferences = hashReferences
        )

        if (entry.hashReferences.size + indexReferences.size > REFERENCES_COUNT_LIMIT) {
            throw IllegalArgumentException("too many references")
        }

        for (ir in indexReferences) {
            if (ir.toInt() >= entries.size) {
                throw IllegalArgumentException("invalid index reference: ${entries.size}")
            }
        }

        entries.add(entry)
    }

    fun addStorageEntry(traceId: String, extra: ByteArray) {
        require(extra.size <= EXTRA_SIZE_STORAGE_CAPACITY) {
            "Extra data exceeds ${extra.size} bytes"
        }
        val cost = estimateStorageCost(extra)
        entries.add(
            InvoiceEntry(
                traceId = traceId,
                assetId = Constants.AssetId.XIN_ASSET_ID,
                amount = cost,
                extra = extra.copyOf(),
                indexReferences = listOf(),
                hashReferences = emptyList(),
            )
        )
    }

    fun toByteArray(): ByteArray {
        var result = byteArrayOf(version)

        val recipientBytes = recipient.toByteArray()
        if (recipientBytes.size > 1024) {
            throw IllegalArgumentException("recipient bytes too long: ${recipientBytes.size}")
        }
        result += ((recipientBytes.size shr 8) and 0xFF).toByte()
        result += (recipientBytes.size and 0xFF).toByte()
        result += recipientBytes

        if (entries.size > 128) {
            throw IllegalArgumentException("too many entries: ${entries.size}")
        }
        result += entries.size.toByte()

        entries.forEach { entry ->
            result += UUIDUtils.toByteArray(entry.traceId)
            result += UUIDUtils.toByteArray(entry.assetId)

            val amountStr = entry.amountString()
            if (amountStr.length > 128) {
                throw IllegalArgumentException("amount string too long: ${amountStr.length}")
            }
            result += amountStr.length.toByte()
            result += amountStr.toByteArray()

            if (entry.extra.size >= EXTRA_SIZE_STORAGE_CAPACITY) {
                throw IllegalArgumentException("extra size too large: ${entry.extra.size}")
            }
            
            result += ((entry.extra.size shr 8) and 0xFF).toByte()
            result += (entry.extra.size and 0xFF).toByte()
            result += entry.extra

            val referencesCount = entry.indexReferences.size + entry.hashReferences.size
            if (referencesCount > REFERENCES_COUNT_LIMIT) {
                throw IllegalArgumentException("too many references: $referencesCount")
            }
            result += referencesCount.toByte()

            entry.indexReferences.forEach { index ->
                result += 1.toByte()
                result += index
            }

            entry.hashReferences.forEach { hash ->
                result += 0.toByte()
                result += hash
            }
        }

        return result
    }

    override fun toString(): String {
        val payload = toByteArray()
        val data = MixinInvoicePrefix.toByteArray() + payload
        val hash = data.sha3Sum256()
        val checksum = hash.slice(0..3).toByteArray()
        val finalPayload = payload + checksum
        return MixinInvoicePrefix + finalPayload.base64RawURLEncode()
    }

    companion object {
        fun fromString(s: String): MixinInvoice {
            if (!s.startsWith(MixinInvoicePrefix)) {
                throw IllegalArgumentException("invalid invoice prefix $s")
            }

            val data = s.substring(MixinInvoicePrefix.length).base64RawURLDecode()
            if (data.size < 3 + 23 + 1) {
                throw IllegalArgumentException("invalid invoice length ${data.size}")
            }

            val payload = data.slice(0 until data.size - 4).toByteArray()
            val checksum = (MixinInvoicePrefix.toByteArray() + payload).sha3Sum256()
            if (!checksum.slice(0..3).toByteArray()
                    .contentEquals(data.slice(data.size - 4 until data.size).toByteArray())
            ) {
                throw IllegalArgumentException("invalid invoice checksum")
            }

            val decoder = ByteBuffer.wrap(payload)

            val version = decoder.get()
            if (version != MIXIN_INVOICE_VERSION) {
                throw IllegalArgumentException("invalid invoice version $version")
            }

            val recipientLength = ((decoder.get().toInt() and 0xFF) shl 8) or (decoder.get().toInt() and 0xFF)
            if (recipientLength <= 0 || recipientLength > 1024) {
                throw IllegalArgumentException("invalid recipient length: $recipientLength")
            }
            val recipientBytes = ByteArray(recipientLength)
            decoder.get(recipientBytes)
            
            val recipient = recipientBytes.toMixAddress() ?: throw IllegalArgumentException("invalid recipient")
            
            val entriesCount = decoder.get().toInt()
            if (entriesCount > 128) {
                throw IllegalArgumentException("too many entries: $entriesCount")
            }
            val entries = mutableListOf<InvoiceEntry>()

            repeat(entriesCount) {
                val traceIdBytes = ByteArray(16)
                decoder.get(traceIdBytes)
                val traceId = UUIDUtils.fromByteArray(traceIdBytes)

                val assetIdBytes = ByteArray(16)
                decoder.get(assetIdBytes)
                val assetId = UUIDUtils.fromByteArray(assetIdBytes)

                val amountLength = decoder.get().toInt()
                if (amountLength > 128) {
                    throw IllegalArgumentException("amount string too long: $amountLength")
                }
                val amountBytes = ByteArray(amountLength)
                decoder.get(amountBytes)
                val amountStr = String(amountBytes)
                val amount = try {
                    if (amountStr.contains(".")) {
                        val parts = amountStr.split(".")
                        if (parts.size != 2) {
                            throw IllegalArgumentException("invalid amount format: $amountStr")
                        }
                        val intPart = if (parts[0].isEmpty()) "0" else parts[0]
                        val decimalPart = parts[1]
                        (intPart + decimalPart).toBigInteger()
                    } else {
                        amountStr.toBigInteger()
                    }
                } catch (_: NumberFormatException) {
                    throw IllegalArgumentException("invalid amount: $amountStr")
                }

                val extraLengthBytes = ByteArray(2)
                decoder.get(extraLengthBytes)
                val extraLength = ((extraLengthBytes[0].toInt() and 0xFF) shl 8) or (extraLengthBytes[1].toInt() and 0xFF)

                if (extraLength >= EXTRA_SIZE_STORAGE_CAPACITY) {
                    throw IllegalArgumentException("extra size too large: $extraLength")
                }

                val extra = if (extraLength > 0) {
                    val bytes = ByteArray(extraLength)
                    decoder.get(bytes)
                    bytes
                } else byteArrayOf()

                val referencesCount = decoder.get().toInt() and 0xFF
                if (referencesCount > REFERENCES_COUNT_LIMIT) {
                    throw IllegalArgumentException("too many references: $referencesCount")
                }

                val indexReferences = mutableListOf<Byte>()
                val hashReferences = mutableListOf<ByteArray>()

                repeat(referencesCount) { i ->
                    val refType = decoder.get()
                    when (refType.toInt() and 0xFF) {
                        0 -> {
                            val hash = ByteArray(32)
                            decoder.get(hash)
                            hashReferences.add(hash)
                        }
                        1 -> {
                            val index = decoder.get()
                            if (index.toInt() >= entries.size) {
                                throw IllegalArgumentException("invalid reference index: $index")
                            }
                            indexReferences.add(index)
                        }
                        else -> {
                            val remaining = ByteArray(decoder.remaining())
                            decoder.get(remaining)
                            val hex = remaining.joinToString("") { "%02x".format(it) }
                            throw IllegalArgumentException("invalid reference type: ${refType.toInt() and 0xFF}, remaining hex: $hex")
                        }
                    }
                }

                val entry = InvoiceEntry(
                    traceId = traceId.toString(),
                    assetId = assetId.toString(),
                    amount = amount,
                    extra = extra,
                    indexReferences = indexReferences,
                    hashReferences = hashReferences
                )
                entries.add(entry)
            }

            if (entries.isEmpty()) throw IllegalArgumentException("entries is empty")
            validateInvoiceEntries(entries)
            return MixinInvoice(
                version = version,
                recipient = recipient,
                entries = entries
            )
        }

        private fun validateInvoiceEntries(entries: List<InvoiceEntry>) {
            val duplicateTraceIds = entries.groupBy { it.traceId }.filter { it.value.size > 1 }.keys
            val duplicateAssetIds = entries.groupBy { it.assetId }.filter { it.value.size > 1 }.keys

            val errors = buildList {
                if (duplicateTraceIds.isNotEmpty()) add(
                    "Duplicate traceIds: ${
                        duplicateTraceIds.joinToString(
                            ", "
                        )
                    }"
                )
                if (duplicateAssetIds.isNotEmpty()) add(
                    "Duplicate assetIds: ${
                        duplicateAssetIds.joinToString(
                            ", "
                        )
                    }"
                )
            }

            if (errors.isNotEmpty()) {
                throw IllegalArgumentException(errors.joinToString("; "))
            }
        }
    }
}

data class InvoiceEntry(
    val traceId: String,
    val assetId: String,
    val amount: BigInteger,
    val extra: ByteArray,
    val indexReferences: List<Byte>,
    val hashReferences: List<ByteArray>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InvoiceEntry

        if (traceId != other.traceId) return false
        if (assetId != other.assetId) return false
        if (amount != other.amount) return false
        if (!extra.contentEquals(other.extra)) return false
        if (indexReferences != other.indexReferences) return false
        if (hashReferences != other.hashReferences) return false

        return true
    }

    val references:List<Reference>
        get() {
            val indexRefs = indexReferences.map { byte ->
                Reference.IndexValue(byte.toInt() and 0xFF)
            }

            val hashRefs = hashReferences.map { byteArray ->
                Reference.HashValue(byteArray.hexString())
            }

            return indexRefs + hashRefs
        }

    fun isStorage(): Boolean {
        return assetId.toString() == Constants.AssetId.XIN_ASSET_ID &&
            extra.isNotEmpty() &&
            extra.size >= EXTRA_SIZE_GENERAL_LIMIT &&
            amount.compareTo(estimateStorageCost(extra)) == 0
    }

    override fun hashCode(): Int {
        var result = traceId.hashCode()
        result = 31 * result + assetId.hashCode()
        result = 31 * result + amount.hashCode()
        result = 31 * result + extra.contentHashCode()
        result = 31 * result + indexReferences.hashCode()
        result = 31 * result + hashReferences.hashCode()
        return result
    }

    fun amountString(): String {
        val amountStr = amount.toString()
        return if (amountStr.length <= 8) {
            "0.${"0".repeat(8 - amountStr.length)}$amountStr"
        } else {
            val intPart = amountStr.substring(0, amountStr.length - 8)
            val decimalPart = amountStr.substring(amountStr.length - 8)
            "$intPart.$decimalPart"
        }
    }

    override fun toString(): String {
        return "InvoiceEntry(traceId=$traceId, assetId=$assetId, amount=${amountString()}, extra=${extra.joinToString("") { "%02x".format(it) }}, indexReferences=$indexReferences, hashReferences=${hashReferences.joinToString { it.joinToString("") { "%02x".format(it) } }})"
    }
}

sealed class Reference {
    data class IndexValue(val value: Int) : Reference()
    data class HashValue(val value: String) : Reference()
}

fun estimateStorageCost(extra: ByteArray): BigInteger {
    require(extra.size <= EXTRA_SIZE_STORAGE_CAPACITY) {
        "Extra data exceeds limit: ${extra.size} > $EXTRA_SIZE_STORAGE_CAPACITY"
    }

    val step = EXTRA_STORAGE_PRICE_STEP.toBigInteger()
    val steps = (extra.size / EXTRA_SIZE_STORAGE_STEP).toBigInteger() + BigInteger.ONE
    return step.multiply(steps)
}