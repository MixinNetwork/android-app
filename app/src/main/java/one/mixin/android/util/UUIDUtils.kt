import ulid.ULID
import java.nio.ByteBuffer
import java.util.UUID

object UUIDUtils {
    fun fromByteArray(bytes: ByteArray): String {
        val bb = ByteBuffer.wrap(bytes)
        val mostSigBits = bb.long
        val leastSigBits = bb.long
        // Todo replace
        return UUID(mostSigBits, leastSigBits).toString()
    }

    fun toByteArray(uuidStr: String): ByteArray {
        val uuid = ULID.parseULID(uuidStr)
        val bb = ByteBuffer.wrap(ByteArray(16))
        bb.putLong(uuid.mostSignificantBits)
        bb.putLong(uuid.leastSignificantBits)
        return bb.array()
    }
}
