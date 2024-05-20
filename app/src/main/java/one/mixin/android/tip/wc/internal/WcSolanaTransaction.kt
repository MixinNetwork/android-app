package one.mixin.android.tip.wc.internal

class WcSolanaTransaction(
    val signatures: List<WcSignature>?,
    val feePayer: String?,
    val instructions: List<WcInstruction>?,
    val recentBlockhash: String?,
    val transaction: String,
)

class WcSolanaMessage(
    val pubkey: String,
    val message: String,
)

class WcSignature(
    val publicKey: String? = null,
    val signature: String?,
)

class WcInstruction(
    val keys: List<WcAccountMeta>,
    val programId: String,
    val data: List<Int>,
)

class WcAccountMeta(
    val pubkey: String,
    val isSigner: Boolean,
    val isWritable: Boolean,
)
