package org.sol4k.instruction

import org.sol4k.AccountMeta
import org.sol4k.PublicKey

data class BaseInstruction(
    override val data: ByteArray,
    override val keys: List<AccountMeta>,
    override val programId: PublicKey,
) : Instruction {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BaseInstruction

        if (!data.contentEquals(other.data)) return false
        if (keys != other.keys) return false
        if (programId != other.programId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + keys.hashCode()
        result = 31 * result + programId.hashCode()
        return result
    }
}
