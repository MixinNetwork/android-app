package one.mixin.android.util

import java.security.MessageDigest
import java.util.UUID

object ConversationIdUtil {

    /**
     * Convert bytes to UUID (equivalent to Go's uuid.FromBytes)
     */
    private fun uuidFromBytes(bytes: ByteArray): UUID {
        if (bytes.size != 16) {
            throw IllegalArgumentException("UUID bytes must be exactly 16 bytes")
        }

        var msb = 0L
        var lsb = 0L

        for (i in 0..7) {
            msb = (msb shl 8) or (bytes[i].toLong() and 0xff)
        }
        for (i in 8..15) {
            lsb = (lsb shl 8) or (bytes[i].toLong() and 0xff)
        }

        return UUID(msb, lsb)
    }

    /**
     * Generate unique object ID
     * Corresponds to UniqueObjectId function in Go code
     */
    fun uniqueObjectId(vararg args: String): String {
        val md = MessageDigest.getInstance("MD5")

        // Write each string separately to MD5 (like Go's io.WriteString)
        for (arg in args) {
            md.update(arg.toByteArray())
        }

        val sum = md.digest()

        // Set UUID version and variant according to Go code logic
        sum[6] = ((sum[6].toInt() and 0x0f) or 0x30).toByte()
        sum[8] = ((sum[8].toInt() and 0x3f) or 0x80).toByte()

        return uuidFromBytes(sum).toString()
    }

    /**
     * Generate unique conversation ID
     * Corresponds to UniqueConversationId function in Go code
     * Sorts the two parameters to ensure same parameter combination always generates same ID
     */
    fun uniqueConversationId(userId: String, recipientId: String): String {
        val minId: String
        val maxId: String

        if (userId.compareTo(recipientId) > 0) {
            maxId = userId
            minId = recipientId
        } else {
            minId = userId
            maxId = recipientId
        }

        val md = MessageDigest.getInstance("MD5")
        md.update(minId.toByteArray())
        md.update(maxId.toByteArray())
        val sum = md.digest()

        // Set UUID version and variant according to Go code logic
        sum[6] = ((sum[6].toInt() and 0x0f) or 0x30).toByte()
        sum[8] = ((sum[8].toInt() and 0x3f) or 0x80).toByte()

        return uuidFromBytes(sum).toString()
    }

    /**
     * Generate group conversation ID
     * Corresponds to GroupConversationId function in Go code
     *
     * @param ownerId Group owner ID
     * @param groupName Group name
     * @param participants List of participant IDs
     * @param randomId Random UUID string
     * @return Generated group conversation ID
     */
    fun generateGroupConversationId(
        ownerId: String,
        groupName: String,
        participants: List<String>,
        randomId: String
    ): String {
        // Validate and normalize randomId (equivalent to uuid.Must(uuid.FromString(randomId)).String())
        val validRandomId = UUID.fromString(randomId).toString()

        var gid = uniqueConversationId(ownerId, groupName)
        gid = uniqueConversationId(gid, validRandomId)

        val sortedParticipants = participants.sorted()
        for (participant in sortedParticipants) {
            gid = uniqueConversationId(gid, participant)
        }
        return gid
    }
}
