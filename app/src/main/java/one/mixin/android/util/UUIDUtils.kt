package one.mixin.android.util

import java.nio.ByteBuffer
import java.util.UUID

object UUIDUtils {
    fun fromByteArray(bytes: ByteArray): String {
        val bb = ByteBuffer.wrap(bytes)
        val mostSigBits = bb.long
        val leastSigBits = bb.long
        return UUID(mostSigBits, leastSigBits).toString()
    }

    fun toByteArray(uuidStr: String): ByteArray {
        val uuid = UUID.fromString(uuidStr)
        val bb = ByteBuffer.wrap(ByteArray(16))
        bb.putLong(uuid.mostSignificantBits)
        bb.putLong(uuid.leastSignificantBits)
        return bb.array()
    }
}

fun uniqueObjectId(vararg args: String): String {
    val h = args.joinToString("")
    return UUID.nameUUIDFromBytes(h.toByteArray()).toString()
}
