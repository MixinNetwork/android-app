package org.sol4k.api

import org.sol4k.PublicKey
import java.math.BigInteger

data class AccountInfo(
    val data: ByteArray,
    val executable: Boolean,
    val lamports: BigInteger,
    val owner: PublicKey,
    val rentEpoch: BigInteger,
    val space: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AccountInfo

        if (!data.contentEquals(other.data)) return false
        if (executable != other.executable) return false
        if (lamports != other.lamports) return false
        if (owner != other.owner) return false
        if (rentEpoch != other.rentEpoch) return false
        if (space != other.space) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + executable.hashCode()
        result = 31 * result + lamports.hashCode()
        result = 31 * result + owner.hashCode()
        result = 31 * result + rentEpoch.hashCode()
        result = 31 * result + space.hashCode()
        return result
    }
}
