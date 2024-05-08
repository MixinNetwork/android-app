package org.sol4k.instruction

import org.sol4k.AccountMeta
import org.sol4k.Binary
import org.sol4k.Constants.SYSTEM_PROGRAM
import org.sol4k.PublicKey

class TransferInstruction(
    from: PublicKey,
    to: PublicKey,
    private val lamports: Long,
) : Instruction {
    override val data: ByteArray
        get() {
            val instruction = Binary.uint32(2L)
            val lamports = Binary.int64(this.lamports)
            return instruction + lamports
        }

    override val keys: List<AccountMeta> = listOf(
        AccountMeta(from, writable = true, signer = true),
        AccountMeta(to, writable = true, signer = false),
    )

    override val programId: PublicKey = SYSTEM_PROGRAM
}
