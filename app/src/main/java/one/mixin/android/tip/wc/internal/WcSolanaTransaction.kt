package one.mixin.android.tip.wc.internal

class WcSolanaTransaction (
    val signatures:List<Signature>,
    val feePayer:String,
    val instructions:List<Instruction>,
    val recentBlockhash:String,
    val transaction:String,
)

class Signature(
    val publicKey:String,
    val signature:String?
)

class Instruction(
val keys:List<Key>,
    val programId:String,
    val data:List<Int>
)

class Key(
    val pubkey: String,
    val isSigner: Boolean,
    val isWritable: Boolean
)
