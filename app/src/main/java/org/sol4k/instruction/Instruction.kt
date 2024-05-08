package org.sol4k.instruction

import org.sol4k.AccountMeta
import org.sol4k.PublicKey

interface Instruction {
    val data: ByteArray
    val keys: List<AccountMeta>
    val programId: PublicKey
}
