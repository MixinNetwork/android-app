package one.mixin.android.vo.utxo

import one.mixin.android.vo.safe.Output

class SignedTransaction(
    val trace: String,
    val signResult: SignResult,
    val utxoWrapperIds :List<String>,
    val asset: String,
    val assetId: String,
    val transactionHash: String,
    val changeMask: String,
    val keys: List<String>,
    val lastOutput: Output,
    val amount: String,
    val memo: String,
    val reference: String? = null,
)
