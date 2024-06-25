package org.sol4k.instruction

import org.sol4k.AccountMeta
import org.sol4k.Constants
import org.sol4k.Constants.ASSOCIATED_TOKEN_PROGRAM_ID
import org.sol4k.Constants.SYSTEM_PROGRAM
import org.sol4k.Constants.TOKEN_PROGRAM_ID
import org.sol4k.PublicKey

class CreateAssociatedTokenAccountInstruction(
    payer: PublicKey,
    associatedToken: PublicKey,
    owner: PublicKey,
    mint: PublicKey,
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
        AccountMeta(TOKEN_PROGRAM_ID),
        AccountMeta(Constants.SysPubkey.RENT_PUBKEY),
    )

    override val programId: PublicKey = ASSOCIATED_TOKEN_PROGRAM_ID
}
