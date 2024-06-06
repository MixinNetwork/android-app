package org.sol4k.instruction

import okio.Buffer
import org.sol4k.AccountMeta
import org.sol4k.Constants.COMPUTE_BUDGET_PROGRAM_ID
import org.sol4k.InstructionSetComputeUnitPrice
import org.sol4k.PublicKey

class SetComputeUnitPriceInstruction(
    private val microLamports: Long,
) : Instruction {

    override val data: ByteArray
        get() {
            val buffer = Buffer()
            buffer.writeByte(InstructionSetComputeUnitPrice)
                .writeLongLe(microLamports)
            return buffer.readByteArray()
        }

    override val keys: List<AccountMeta> = emptyList()

    override val programId: PublicKey = COMPUTE_BUDGET_PROGRAM_ID
}