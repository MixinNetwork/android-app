package one.mixin.android.vo

import one.mixin.android.vo.safe.UtxoWrapper

class VerifiedTransactionData(
    val trace: String,
    val raw: String,
    val hash: String,
    val utxoWrapper: UtxoWrapper,
    val asset: String,
    val assetId: String,
    val amount: String,
    val changeMask: String,
    val keys: List<String>,
    val extra: ByteArray,
    val reference: String?
)