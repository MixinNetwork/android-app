package one.mixin.android.vo

import UUIDUtils
import kernel.Address
import kernel.Kernel
import one.mixin.android.crypto.sha3Sum256
import one.mixin.android.extension.isUUID
import one.mixin.android.util.decodeBase58
import one.mixin.android.util.encodeToBase58String
import timber.log.Timber

const val MixAddressPrefix = "MIX"
private const val MixAddressVersion = 0x2.toByte()

data class MixAddress(
    val version: Byte,
    val threshold: Byte,
) {
    val uuidMembers = mutableListOf<String>()
    val xinMembers = mutableListOf<Address>()

    companion object {
        fun newUuidMixAddress(
            members: List<String>,
            threshold: Int,
        ): MixAddress? {
            return MixAddress(MixAddressVersion, threshold.toByte()).apply {
                members.map {
                    if (!it.isUUID()) return null
                }
                uuidMembers.addAll(members)
            }
        }

        fun newMainnetMixAddress(
            members: List<String>,
            threshold: Int,
        ): MixAddress? {
            return MixAddress(MixAddressVersion, threshold.toByte()).apply {
                for (m in members) {
                    try {
                        xinMembers.add(Kernel.newMainAddressFromString(m))
                    } catch (e: Exception) {
                        Timber.i("newMainAddressFromString with $m meet $e")
                        return null
                    }
                }
            }
        }
    }

    fun members(): List<String> {
        return if (uuidMembers.size > 0) {
            uuidMembers
        } else {
            xinMembers.map { it.string() }
        }
    }

    override fun toString(): String {
        var payload = byteArrayOf(version, threshold)
        var len = uuidMembers.size
        if (len > 0) {
            if (len > 255) {
                return ""
            }
            payload += len.toByte()
            for (u in uuidMembers) {
                payload += UUIDUtils.toByteArray(u)
            }
        } else {
            len = xinMembers.size
            if (len > 255) {
                return ""
            }
            payload += len.toByte()
            for (x in xinMembers) {
                payload += (x.publicSpendKey() + x.publicViewkey())
            }
        }
        val data = MixAddressPrefix.toByteArray() + payload
        val checksum = data.sha3Sum256()
        payload += checksum.sliceArray(0..3)
        return MixAddressPrefix + payload.encodeToBase58String()
    }
}

fun String.toMixAddress(): MixAddress? {
    if (!this.startsWith(MixAddressPrefix)) return null

    val data =
        try {
            this.removePrefix(MixAddressPrefix).decodeBase58()
        } catch (e: Exception) {
            Timber.i("decodeBase58 with $this meet $e")
            return null
        }
    if (data.size < 3 + 16 + 4) return null

    val payload = data.sliceArray(0..data.size - 5)
    val checksum = (MixAddressPrefix.toByteArray() + payload).sha3Sum256().sliceArray(0..3)
    if (!checksum.contentEquals(data.sliceArray(data.size - 4..<data.size))) {
        return null
    }

    val version = payload[0]
    if (version != MixAddressVersion) return null
    val threshold = payload[1]
    val total = payload[2].toInt()
    if (threshold.toInt() == 0 || threshold > total || total > 64) return null
    val mixAddress = MixAddress(version, threshold)
    val mb = payload.sliceArray(3..<payload.size)
    when (mb.size) {
        16 * total -> {
            for (i in 0..<total) {
                val id = UUIDUtils.fromByteArray(mb.sliceArray(i * 16..<i * 16 + 16))
                mixAddress.uuidMembers.add(id)
            }
        }
        64 * total -> {
            for (i in 0..<total) {
                val xinAddress = Address()
                xinAddress.setPublicSpendKey(mb.sliceArray(i * 64..<i * 64 + 32))
                xinAddress.setPublicViewKey(mb.sliceArray(i * 64 + 32..<i * 64 + 64))
                mixAddress.xinMembers.add(xinAddress)
            }
        }
        else -> {
            return null
        }
    }
    return mixAddress
}
