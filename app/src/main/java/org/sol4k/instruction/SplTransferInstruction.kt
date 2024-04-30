package org.sol4k.instruction

import org.sol4k.AccountMeta
import org.sol4k.Binary
import org.sol4k.Constants.TOKEN_PROGRAM_ID
import org.sol4k.PublicKey

class SplTransferInstruction(
    from: PublicKey,
    to: PublicKey,
    owner: PublicKey,
    private val amount: Long,
) : Instruction {
    override val data: ByteArray
        get() {
            val instruction = byteArrayOf(3)
            val amountBytes = Binary.int64(amount)
            return instruction + amountBytes
        }

    override val keys: List<AccountMeta> = listOf(
        AccountMeta.writable(from),
        AccountMeta.writable(to),
        AccountMeta.signer(owner),
    )

    override val programId: PublicKey = TOKEN_PROGRAM_ID
}
