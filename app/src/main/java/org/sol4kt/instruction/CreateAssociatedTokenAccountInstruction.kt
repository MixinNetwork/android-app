package org.sol4kt.instruction

import org.sol4k.AccountMeta
import org.sol4k.Constants
import org.sol4k.Constants.ASSOCIATED_TOKEN_PROGRAM_ID
import org.sol4k.Constants.SYSTEM_PROGRAM
import org.sol4k.Constants.TOKEN_PROGRAM_ID
import org.sol4k.PublicKey
import org.sol4k.instruction.Instruction

class CreateAssociatedTokenAccountInstructionCompat(
    payer: PublicKey,
    associatedToken: PublicKey,
    owner: PublicKey,
    mint: PublicKey,
    tokenProgramId: PublicKey = TOKEN_PROGRAM_ID,
) : Instruction {
    companion object {
        @Suppress("unused")
        private const val instructionCreate = 0
        private const val instructionCreateIdempotent = 1
    }

    override val data: ByteArray = byteArrayOf(instructionCreateIdempotent.toByte())

    override val keys: List<AccountMeta> = listOf(
        AccountMeta.signerAndWritable(payer),
        AccountMeta.writable(associatedToken),
        AccountMeta(owner),
        AccountMeta(mint),
        AccountMeta(SYSTEM_PROGRAM),
        AccountMeta(tokenProgramId),
        AccountMeta(Constants.SYSVAR_RENT_ADDRESS)
    )

    override val programId: PublicKey = ASSOCIATED_TOKEN_PROGRAM_ID
}