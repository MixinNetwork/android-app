package one.mixin.android.tip.wc.internal

import org.sol4k.AccountMeta
import org.sol4k.PublicKey
import org.sol4k.Transaction
import org.sol4k.instruction.Instruction

class WcSolanaTransaction(
    val signatures: List<WcSignature>,
    val feePayer: String,
    val instructions: List<WcInstruction>,
    val recentBlockhash: String,
    val transaction: String,
)

class WcSolanaMessage(
    val message:String
)

class WcSignature(
    val publicKey: String,
    val signature: String?
)

class WcInstruction(
    val keys: List<WcAccountMeta>,
    val programId: String,
    val data: List<Int>
)

class WcAccountMeta(
    val pubkey: String,
    val isSigner: Boolean,
    val isWritable: Boolean
)

class SolanaInstruction(override val data: ByteArray, override val keys: List<AccountMeta>, override val programId: PublicKey) : Instruction

private fun WcInstruction.toInstruction(): Instruction {
    return SolanaInstruction(data.foldIndexed(ByteArray(data.size)) { i, a, v -> a.apply { set(i, v.toByte()) } }, keys.map {
        AccountMeta(PublicKey(it.pubkey), it.isSigner, it.isWritable)
    }, PublicKey(programId))
}

fun WcSolanaTransaction.toTransaction(): Transaction = Transaction(recentBlockhash, instructions.map { it.toInstruction() }, PublicKey(feePayer))
